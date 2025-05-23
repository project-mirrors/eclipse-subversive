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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNEntryStatusCallback;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNNotification;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Number;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * This operation fetch remote resource statuses
 * 
 * @author Alexander Gurov
 */
public class RemoteStatusOperation extends AbstractWorkingCopyOperation
		implements IRemoteStatusOperation, ISVNNotificationCallback {
	protected SVNChangeStatus[] statuses;

	protected Map<String, Number> pegRevisions;

	public RemoteStatusOperation(IResource[] resources) {
		super("Operation_UpdateStatus", SVNMessages.class, resources); //$NON-NLS-1$
		pegRevisions = new HashMap<>();
	}

	public RemoteStatusOperation(IResourceProvider provider) {
		super("Operation_UpdateStatus", SVNMessages.class, provider); //$NON-NLS-1$
		pegRevisions = new HashMap<>();
	}

	@Override
	public IResource[] getScope() {
		return operableData();
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = FileUtility.shrinkChildNodes(operableData());

		final HashSet<IPath> projectPaths = new HashSet<>();
		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			projectPaths.add(new Path(FileUtility.getWorkingCopyPath(resources[i].getProject())));
		}
		final HashMap<IPath, SVNChangeStatus> result = new HashMap<>();
		final List<SVNChangeStatus> conflicts = new ArrayList<>();
		final ISVNEntryStatusCallback cb = new ISVNEntryStatusCallback() {
			@Override
			public void next(SVNChangeStatus status) {
				result.put(new Path(status.path), status);
				if (status.hasConflict && status.treeConflicts == null) {
					conflicts.add(status);
				}
				String parent = new File(status.path).getParent();
				if (parent != null) {// can be null for drive roots
					IPath projectPath = getProjectPath(parent);
					if (projectPath != null) {
						if (status.reposKind != SVNEntry.Kind.DIR) {
							/*
							 * The reason why we don't set statuses for all parents
							 * of resource, but set only for its direct parent:
							 * there are 3 presentations in old Synchronize view
							 * (Flat, Tree, Compressed Folders). If we set statuses
							 * for all parents then in Compressed Folders presentation
							 * resources are shown in another way as they should be.
							 * Example:
							 * Project/src/com/Foo.java
							 * where Foo.java has incoming changes.
							 * If we set statuses for all parents Sync view would show:
							 * Project/
							 * 	src
							 * 	src/com
							 *  	Foo.java
							 * instead of
							 * Project
							 * 	src/com
							 * 		Foo.java
							 * 
							 */
							postStatus(parent, status);
						}
						postStatus(projectPath.toString(), status);
					}
				}
			}

			private void postStatus(String path, SVNChangeStatus baseStatus) {
				IPath tPath = new Path(path);
				SVNChangeStatus st = result.get(tPath);
				if (st == null || st.reposLastCmtRevision < baseStatus.reposLastCmtRevision) {
					SVNChangeStatus status = makeStatus(path, baseStatus);
					result.put(tPath, status);
				}
			}

			private SVNChangeStatus makeStatus(String path, SVNChangeStatus status) {
				int deltaSegments = new Path(status.path).segmentCount() - new Path(path).segmentCount();
				return new SVNChangeStatus(
						path,
						status.url != null
								? SVNUtility.createPathForSVNUrl(status.url)
										.removeLastSegments(deltaSegments)
										.toString()
								: null,
						SVNEntry.Kind.DIR, SVNRevision.INVALID_REVISION_NUMBER, SVNRevision.INVALID_REVISION_NUMBER, 0,
						null, SVNEntryStatus.Kind.NORMAL, SVNEntryStatus.Kind.NONE, SVNEntryStatus.Kind.NORMAL,
						SVNEntryStatus.Kind.MODIFIED, false, false, false, null, null, status.reposLastCmtRevision,
						status.reposLastCmtDate, SVNEntry.Kind.DIR, status.reposLastCmtAuthor, false, false, null,
						null);
			}

			private IPath getProjectPath(String path) {
				IPath tPath = new Path(path);
				for (IPath projectPath : projectPaths) {
					if (projectPath.isPrefixOf(tPath)) {
						return projectPath;
					}
				}
				return null;
			}
		};
		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resources[i]);
			final ISVNConnector proxy = location.acquireSVNProxy();

			SVNUtility.addSVNNotifyListener(proxy, this);
			final IResource current = resources[i];
			ProgressMonitorUtility.setTaskInfo(monitor, this, current.getFullPath().toString());
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn status -u \"" + FileUtility.normalizePath(current.getLocation().toString()) + "\\"" + FileUtility.getUsernameParam(location.getUsername()) + "\\n"
			conflicts.clear();

			this.protectStep(monitor1 -> proxy.status(
					FileUtility.getWorkingCopyPath(current), SVNDepth.UNKNOWN,
					ISVNConnector.Options.SERVER_SIDE | ISVNConnector.Options.LOCAL_SIDE, null, cb,
					new SVNProgressMonitor(RemoteStatusOperation.this, monitor1, null, false)), monitor, resources.length);
			SVNUtility.removeSVNNotifyListener(proxy, this);

			for (Iterator<SVNChangeStatus> it = conflicts.iterator(); it.hasNext() && !monitor.isCanceled();) {
				final SVNChangeStatus svnChangeStatus = it.next();
				this.protectStep(monitor1 -> proxy.getInfo(new SVNEntryRevisionReference(svnChangeStatus.path), SVNDepth.EMPTY,
						ISVNConnector.Options.FETCH_ACTUAL_ONLY, null, info -> svnChangeStatus.setTreeConflicts(info.treeConflicts), new SVNProgressMonitor(RemoteStatusOperation.this, monitor1, null, false)), monitor, resources.length);
			}

			location.releaseSVNProxy(proxy);
		}
		statuses = result.values().toArray(new SVNChangeStatus[result.size()]);
	}

	@Override
	public SVNEntryStatus[] getStatuses() {
		return statuses;
	}

	@Override
	public void setPegRevision(IResourceChange change) {
		IPath resourcePath = FileUtility.getResourcePath(change.getResource());
		int prefixLength = 0;
		SVNRevision revision = SVNRevision.INVALID_REVISION;
		for (Iterator<?> it = pegRevisions.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			IPath rootPath = new Path((String) entry.getKey());
			int segments = rootPath.segmentCount();
			if (rootPath.isPrefixOf(resourcePath) && segments > prefixLength) {
				prefixLength = segments;
				revision = (SVNRevision) entry.getValue();
			}
		}
		if (revision != SVNRevision.INVALID_REVISION) {
			change.setPegRevision(revision);
		} else if (change.getResource().getType() == IResource.PROJECT) {
			IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(change.getResource());
			change.setPegRevision(remote.getPegRevision());
		}
	}

	@Override
	public void notify(SVNNotification info) {
		if (info.revision != SVNRevision.INVALID_REVISION_NUMBER) {
			pegRevisions.put(info.path, SVNRevision.fromNumber(info.revision));
		}
	}

}
