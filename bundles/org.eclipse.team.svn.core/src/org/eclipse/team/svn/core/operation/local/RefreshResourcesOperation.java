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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.SVNResourceRuleFactory;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Refresh Eclipse IDE workspace resources operation implementation
 * 
 * @author Alexander Gurov
 */
public class RefreshResourcesOperation extends AbstractWorkingCopyOperation {
	public static int REFRESH_CACHE = 0;

	public static int REFRESH_CHANGES = 1;

	public static int REFRESH_ALL = 2;

	protected int depth;

	protected int refreshType;

	protected boolean ignoreNestedProjects;

	public RefreshResourcesOperation(IResource[] resources) {
		this(resources, false);
	}

	public RefreshResourcesOperation(IResource[] resources, boolean ignoreNestedProjects) {
		this(resources, IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_CHANGES, ignoreNestedProjects);
	}

	public RefreshResourcesOperation(IResource[] resources, int depth, int refreshType) {
		this(resources, depth, refreshType, false);
	}

	public RefreshResourcesOperation(IResource[] resources, int depth, int refreshType, boolean ignoreNestedProjects) {
		super("Operation_RefreshResources", SVNMessages.class, resources); //$NON-NLS-1$
		this.depth = depth;
		this.refreshType = refreshType;
		this.ignoreNestedProjects = ignoreNestedProjects;
	}

	public RefreshResourcesOperation(IResourceProvider provider) {
		this(provider, IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_CHANGES);
	}

	public RefreshResourcesOperation(IResourceProvider provider, int depth, int refreshType) {
		this(provider, depth, refreshType, false);
	}

	public RefreshResourcesOperation(IResourceProvider provider, int depth, int refreshType,
			boolean ignoreNestedProjects) {
		super("Operation_RefreshResources", SVNMessages.class, provider); //$NON-NLS-1$
		this.depth = depth;
		this.refreshType = refreshType;
		this.ignoreNestedProjects = ignoreNestedProjects;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		ISchedulingRule rule = super.getSchedulingRule();
		if (ignoreNestedProjects || rule == ResourcesPlugin.getWorkspace().getRoot()) {
			return rule;
		}
		IResource[] resources = operableData();
		HashSet<ISchedulingRule> ruleSet = new HashSet<>();
		for (IResource element : resources) {
			ruleSet.add(SVNResourceRuleFactory.INSTANCE.refreshRule(element));
		}
		return new MultiRule(ruleSet.toArray(new ISchedulingRule[ruleSet.size()]));
	}

	@Override
	protected IResource[] operableData() {
		IResource[] resources = super.operableData();
		if (ignoreNestedProjects) {
			return resources;
		}
		return mindNestedProject(resources);
	}

	protected IResource[] mindNestedProject(IResource[] resources) {
		HashSet<IResource> resourceSet = new HashSet<>();
		for (IResource element : resources) {
			URI uri = element.getLocationURI();
			if (uri != null) {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				Collections.addAll(resourceSet,
						element.getType() != IResource.FILE
								? root.findContainersForLocationURI(uri)
								: root.findFilesForLocationURI(uri));
				IPath path = FileUtility.getResourcePath(element);
				for (IResource resource : root.getProjects()) {
					if (path.isPrefixOf(FileUtility.getResourcePath(resource)) && depth != IResource.DEPTH_ZERO) {
						resourceSet.add(resource);
					}
				}
			}
		}
		return resourceSet.toArray(new IResource[resourceSet.size()]);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] original = operableData();
		if (original.length == 0) {
			return;
		}
		int tDepth = depth;
		if (tDepth != IResource.DEPTH_ZERO) {
			boolean allFiles = true; // reduce specified refresh depth in case all are files
			for (IResource element : original) {
				allFiles &= element.getType() == IResource.FILE;
			}
			if (allFiles) {
				tDepth = IResource.DEPTH_ZERO;
			}
		}
		final IResource[] resources = depth == IResource.DEPTH_INFINITE
				? FileUtility.shrinkChildNodes(original)
				: original;
		final boolean isPriorToSVN17 = SVNUtility.isPriorToSVN17();
		if (refreshType != RefreshResourcesOperation.REFRESH_CACHE) {
			ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) monitor2 -> {
				for (int i = 0; i < resources.length && !monitor2.isCanceled(); i++) {
					ProgressMonitorUtility.setTaskInfo(monitor2, RefreshResourcesOperation.this,
							resources[i].getName());
					final IResource resource = resources[i];
					RefreshResourcesOperation.this.protectStep(monitor1 -> {
						if (isPriorToSVN17) {
							if (resource.getType() != IResource.FILE && depth != IResource.DEPTH_INFINITE) {
								// if depth is set to "infinite", then meta info will be refreshed together with the resource itself
								RefreshResourcesOperation.this.refreshMetaInfo((IContainer) resource, monitor1);
							}
							if (resource.getType() != IResource.PROJECT) {
								// refresh parent's meta info if exists
								RefreshResourcesOperation.this.refreshMetaInfo(resource.getParent(), monitor1);
							}
						}
						RefreshResourcesOperation.this.doRefresh(resource, depth, monitor1);
					}, monitor2, resources.length);
				}
			}, null, IWorkspace.AVOID_UPDATE, monitor);
		}

		// FIXME there could be event doubles when SVN 1.7 is used
		if (RefreshResourcesOperation.this.refreshType != RefreshResourcesOperation.REFRESH_CHANGES
				|| !isPriorToSVN17) {
			SVNRemoteStorage.instance().refreshLocalResources(resources, depth);

			IResource[] roots = FileUtility.getPathNodes(resources);
			SVNRemoteStorage.instance()
					.fireResourceStatesChangedEvent(new ResourceStatesChangedEvent(roots, IResource.DEPTH_ZERO,
							ResourceStatesChangedEvent.PATH_NODES));
			SVNRemoteStorage.instance()
					.fireResourceStatesChangedEvent(
							new ResourceStatesChangedEvent(original, tDepth, ResourceStatesChangedEvent.CHANGED_NODES));
		}

		if (ignoreNestedProjects) {
			IResource[] withNested = mindNestedProject(original);
			// schedule refresh for nested projects
			if (withNested.length != original.length) {
				ProgressMonitorUtility.doTaskScheduledDefault(
						new RefreshResourcesOperation(withNested, depth, refreshType), true);
			}
		}
	}

	protected void refreshMetaInfo(IContainer container, IProgressMonitor monitor) throws CoreException {
		IResource metaInfo = container.findMember(SVNUtility.getSVNFolderName());
		if (metaInfo != null) {
			doRefresh(metaInfo, IResource.DEPTH_INFINITE, monitor);
		}
	}

	protected void doRefresh(final IResource resource, final int depth, IProgressMonitor monitor) throws CoreException {
		try {
			resource.refreshLocal(depth, monitor);
		} catch (CoreException ex) {
			if (ex.getStatus() != null && ex.getStatus().toString().indexOf("(.project)") != -1) { //$NON-NLS-1$
				throw new UnreportableException(SVNMessages.Operation_RefreshResources_DamagedProjectFile);
			}
			throw ex;
		}
	}

}
