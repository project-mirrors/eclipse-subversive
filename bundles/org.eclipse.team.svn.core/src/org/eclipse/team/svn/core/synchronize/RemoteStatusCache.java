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
 *    Andrey Loskutov - Performance improvements for RemoteStatusCache
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.operation.local.GetAllResourcesOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Remote status cache implementation
 * 
 * @author Alexander Gurov
 */
public class RemoteStatusCache extends ResourceVariantByteStore implements IRemoteStatusCache {
	protected Map<IPath, byte[]> resourceStateMap;

	protected Map<IPath, Set<IResource>> resourceChildrenMap;

	public RemoteStatusCache() {
		resourceStateMap = new HashMap<>();
		resourceChildrenMap = new HashMap<>();
	}

	@Override
	public synchronized boolean containsData() {
		return resourceStateMap.size() > 0;
	}

	@Override
	public void dispose() {

	}

	@Override
	public synchronized void clearAll() {
		resourceChildrenMap.clear();
		resourceStateMap.clear();
	}

	@Override
	public synchronized byte[] getBytes(IResource resource) {
		return resourceStateMap.get(resource.getFullPath());
	}

	@Override
	public synchronized boolean setBytes(IResource resource, byte[] bytes) {
		byte[] old = resourceStateMap.put(resource.getFullPath(), bytes);
		IPath parentPath = resource.getParent().getFullPath();
		Set<IResource> members = resourceChildrenMap.get(parentPath);
		if (members == null) {
			resourceChildrenMap.put(parentPath, members = new HashSet<>());
		}
		members.add(resource);
		return !this.equals(old, bytes);
	}

	@Override
	public synchronized boolean flushBytes(IResource resource, int depth) {
		final HashSet<IPath> removedSet = new HashSet<>();
		boolean retVal = resourceStateMap.remove(resource.getFullPath()) != null;
		this.traverse(resource, depth, (ICacheVisitor) (path, data) -> removedSet.add(path));
		for (IPath current : removedSet) {
			resourceStateMap.remove(current);
			resourceChildrenMap.remove(current);
		}
		Set<IResource> members = resourceChildrenMap.get(resource.getParent().getFullPath());
		if (members != null) {
			members.remove(resource);
		}
		return retVal;
	}

	@Override
	public synchronized boolean deleteBytes(IResource resource) {
		return flushBytes(resource, IResource.DEPTH_ZERO);
	}

	@Override
	public synchronized IResource[] members(IResource resource) {
		Set<?> members = resourceChildrenMap.get(resource.getFullPath());
		return members == null ? FileUtility.NO_CHILDREN : members.toArray(new IResource[members.size()]);
	}

	@Override
	public synchronized IResource[] allMembers(IResource resource) {
		if (!(resource instanceof IContainer)) {
			return FileUtility.NO_CHILDREN;
		}
		IResource[] known = members(resource);
		List<IResource> members;
		if (known.length == 0) {
			members = new ArrayList<>();
		} else {
			members = new ArrayList<>(Arrays.asList(known));
		}
		if (RepositoryProvider.getProvider(resource.getProject(), SVNTeamPlugin.NATURE_ID) != null) {
			IContainer container = (IContainer) resource;
			GetAllResourcesOperation op = new GetAllResourcesOperation(container);
			ProgressMonitorUtility.doTaskExternal(op, new NullProgressMonitor());
			IResource[] children = op.getChildren();
			if (children.length > 0) {
				members.addAll(Arrays.asList(children));
			}
		}
		return members.toArray(new IResource[members.size()]);
	}

	@Override
	public synchronized void traverse(IResource[] resources, int depth, ICacheVisitor visitor) {
		for (IResource element : resources) {
			this.traverse(element, depth, visitor);
		}
	}

	protected void traverse(IResource resource, int depth, ICacheVisitor visitor) {
		IPath base = resource.getFullPath();
		for (Map.Entry<IPath, byte[]> entry : resourceStateMap.entrySet()) {
			IPath current = entry.getKey();
			if (isChildOf(base, current, depth)) {
				visitor.visit(current, entry.getValue());
			}
		}
	}

	protected boolean isChildOf(IPath base, IPath current, int depth) {
		if (base.isPrefixOf(current)) {
			int cachedSegmentsCount = current.segmentCount();
			int matchingSegmentsCount = base.matchingFirstSegments(current);
			int difference = cachedSegmentsCount - matchingSegmentsCount;
			if (difference >= 0 && depth == IResource.DEPTH_INFINITE ? true : depth >= difference) {
				return true;
			}
		}
		return false;
	}

}
