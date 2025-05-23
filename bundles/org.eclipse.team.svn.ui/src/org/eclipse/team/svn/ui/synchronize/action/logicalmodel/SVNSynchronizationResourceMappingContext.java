/*******************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *   See git history
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action.logicalmodel;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;

public class SVNSynchronizationResourceMappingContext extends RemoteResourceMappingContext {

	private final ISynchronizationContext context;

	/**
	 * Create a resource mapping context for the given synchronization context
	 * 
	 * @param context
	 *            the synchronization context
	 */
	public SVNSynchronizationResourceMappingContext(ISynchronizationContext context) {
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#isThreeWay()
	 */
	@Override
	public boolean isThreeWay() {
		return context.getType() == ISynchronizationContext.THREE_WAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#hasRemoteChange(org.eclipse.core.resources.IResource, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean hasRemoteChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(resource);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff remote = twd.getRemoteChange();
			return remote != null && remote.getKind() != IDiff.NO_CHANGE;
		}
		return diff != null && diff.getKind() != IDiff.NO_CHANGE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#hasLocalChange(org.eclipse.core.resources.IResource, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean hasLocalChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(resource);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff local = twd.getLocalChange();
			return local != null && local.getKind() != IDiff.NO_CHANGE;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#fetchRemoteContents(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStorage fetchRemoteContents(IFile file, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(file);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff remote = twd.getRemoteChange();
			if (remote instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) remote;
				return rd.getAfterState().getStorage(monitor);
			}
		} else if (diff instanceof IResourceDiff) {
			IResourceDiff rd = (IResourceDiff) diff;
			return rd.getAfterState().getStorage(monitor);
		}
		return file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#fetchBaseContents(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStorage fetchBaseContents(IFile file, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(file);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff remote = twd.getRemoteChange();
			if (remote instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) remote;
				return rd.getBeforeState().getStorage(monitor);
			}
			IDiff local = twd.getLocalChange();
			if (local instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) local;
				return rd.getBeforeState().getStorage(monitor);
			}
		}
		return null;
	}

	@Override
	public IResource[] fetchMembers(IContainer container, IProgressMonitor monitor) throws CoreException {
		Set result = new HashSet();
		IResource[] children = UpdateSubscriber.instance().members(container);
		for (IResource resource : children) {
			result.add(resource);
		}
		IPath[] childPaths = context.getDiffTree().getChildren(container.getFullPath());
		for (IPath path : childPaths) {
			IDiff delta = context.getDiffTree().getDiff(path);
			IResource child;
			if (delta == null) {
				// the path has descendent deltas so it must be a folder
				if (path.segmentCount() == 1) {
					child = ((IWorkspaceRoot) container).getProject(path.lastSegment());
				} else {
					child = container.getFolder(new Path(path.lastSegment()));
				}
			} else {
				child = context.getDiffTree().getResource(delta);
			}
			result.add(child);
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}

	@Override
	public void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException {
		//context.refresh(traversals, flags, monitor);
	}

	public ISynchronizationContext getSynchronizationContext() {
		return context;
	}

	@Override
	public IProject[] getProjects() {
		Set projects = new HashSet();
		IResource[] roots = context.getScope().getRoots();
		for (IResource resource : roots) {
			projects.add(resource.getProject());
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

}
