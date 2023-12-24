/*******************************************************************************
 * Copyright (c) 2005, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *    Andrey Loskutov - [scalability] SVN update takes hours if "Synchronize" view is opened
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.AsynchronousActiveQueue;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.IQueuedElement;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Workspace resource changes listener
 * 
 * @author Alexander Gurov
 */
public class ResourceChangeListener implements IResourceChangeListener, ISaveParticipant {

	private static class ResourceChange implements IQueuedElement<ResourceChange> {
		private IResource[] resources;

		private int depth;

		public ResourceChange(IResource[] resources, int depth) {
			this.resources = resources;
			this.depth = depth;
		}

		@Override
		public boolean canSkip() {
			return true;
		}

		@Override
		public boolean canMerge(ResourceChange d) {
			return depth == d.depth;
		}

		@Override
		public ResourceChange merge(ResourceChange d) {
			IResource[] arr = new IResource[resources.length + d.resources.length];
			System.arraycopy(resources, 0, arr, 0, resources.length);
			System.arraycopy(d.resources, 0, arr, resources.length, d.resources.length);
			return new ResourceChange(arr, depth);
		}

		@Override
		public int hashCode() {
			return Objects.hash(depth, Arrays.hashCode(resources));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ResourceChange)) {
				return false;
			}
			ResourceChange other = (ResourceChange) obj;
			if ((depth != other.depth) || !Arrays.equals(resources, other.resources)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ResourceChange [depth=");
			builder.append(depth);
			builder.append(", ");
			builder.append(", size=");
			builder.append(resources.length);
			builder.append(", ");
			builder.append("resources=");
			builder.append(Arrays.toString(resources));
			builder.append("]");
			return builder.toString();
		}
	}

	protected AsynchronousActiveQueue<ResourceChange> refreshQueue;

	public static int INTERESTING_CHANGES = IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO | IResourceDelta.OPEN
			| IResourceDelta.REPLACED | IResourceDelta.TYPE;

	public ResourceChangeListener() {
		refreshQueue = new AsynchronousActiveQueue<>("Operation_ResourcesChanged",
				(monitor, op, record) -> {
					IResource[] resources = record.resources;
					SVNRemoteStorage.instance().refreshLocalResources(resources, record.depth);

					ResourceStatesChangedEvent pathEvent = new ResourceStatesChangedEvent(
							FileUtility.getPathNodes(resources), IResource.DEPTH_ZERO,
							ResourceStatesChangedEvent.PATH_NODES);
					SVNRemoteStorage.instance().fireResourceStatesChangedEvent(pathEvent);

					ResourceStatesChangedEvent resourcesEvent = new ResourceStatesChangedEvent(resources,
							IResource.DEPTH_ZERO, ResourceStatesChangedEvent.CHANGED_NODES);
					SVNRemoteStorage.instance().fireResourceStatesChangedEvent(resourcesEvent);
				}, false);
	}

	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		ProgressMonitorUtility
				.doTaskScheduledDefault(new AbstractActionOperation("Operation_ResourcesChanged", SVNMessages.class) { //$NON-NLS-1$
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						final Set<IResource> modified = new HashSet<>();
						final int[] depth = { IResource.DEPTH_ZERO };
						event.getDelta().accept((IResourceDeltaVisitor) delta -> {
							IResource resource = delta.getResource();

							if (resource.getType() == IResource.ROOT) {
								return true;
							}
							if (!FileUtility.isConnected(resource)) {
								return false;
							}
							IResource toAdd = null;
							IResource svnFolder = FileUtility.getSVNFolder(resource);
							if (svnFolder != null) {
								toAdd = svnFolder.getParent();
								return false;
							}
							if (resource instanceof IFolder && SVNUtility.isIgnored(resource)) {
								if (SVNTeamPlugin.instance().isDebugging()) {
									System.out.println("Ignoring: " + resource.getLocation());
								}
								return false;
							}
							if (delta.getKind() == IResourceDelta.ADDED
									|| delta.getKind() == IResourceDelta.REMOVED) {
								toAdd = resource;
							} else if (delta.getKind() == IResourceDelta.CHANGED) {
								int flags = delta.getFlags();
								if (resource instanceof IContainer
										&& (flags & ResourceChangeListener.INTERESTING_CHANGES) != 0
										|| resource instanceof IFile
												&& (flags & (ResourceChangeListener.INTERESTING_CHANGES
														| IResourceDelta.CONTENT)) != 0) {
									toAdd = resource;
								}
							}
							if (toAdd != null) {
								if (SVNUtility.isIgnored(toAdd)) {
									if (SVNTeamPlugin.instance().isDebugging()) {
										System.out.println("Ignoring: " + toAdd.getLocation());
									}
									return false;
								}
								modified.add(toAdd);
								if (toAdd.getType() != IResource.FILE) {
									depth[0] = IResource.DEPTH_INFINITE;
								}
							}

							return true;
						}, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);

						// reset statuses only for changed resources, but notify regarding all and including parents
						if (modified.size() > 0) {
							IResource[] resources = modified.toArray(new IResource[modified.size()]);
							refreshQueue.push(new ResourceChange(resources, depth[0]));
						}
					}
				});
	}

	public void handleInitialWorkspaceDelta() throws CoreException {
		// We register a save participant so we can get the delta from the workbench startup to plugin startup.
		ISavedState ss = ResourcesPlugin.getWorkspace().addSaveParticipant(SVNTeamPlugin.instance(), this);
		if (ss != null) {
			ss.processResourceChangeEvents(this);
		}
	}

	@Override
	public void doneSaving(ISaveContext context) {

	}

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {

	}

	@Override
	public void rollback(ISaveContext context) {

	}

	@Override
	public void saving(ISaveContext context) throws CoreException {
		context.needDelta();
	}

}
