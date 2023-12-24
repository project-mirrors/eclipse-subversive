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
 *    Igor Burilo - Initial API and implementation
 *    Andrey Loskutov - Performance improvements for RemoteStatusCache
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.PersistantResourceVariantByteStore;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.operation.local.GetAllResourcesOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Persistent remote status cache implementation
 * 
 * @author Igor Burilo
 */
public class PersistentRemoteStatusCache extends PersistantResourceVariantByteStore implements IRemoteStatusCache {

	public PersistentRemoteStatusCache(QualifiedName qualifiedName) {
		super(qualifiedName);
	}

	@Override
	public boolean containsData() throws TeamException {
		boolean containsData = false;
		IResource[] roots = roots();
		for (IResource root : roots) {
			if (getBytes(root) != null) {
				containsData = true;
				break;
			}
		}
		return containsData;
	}

	@Override
	public void clearAll() throws TeamException {
		IResource[] resources = roots();
		for (IResource resource : resources) {
			flushBytes(resource, IResource.DEPTH_INFINITE);
		}
	}

	@Override
	public IResource[] allMembers(IResource resource) throws TeamException {
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
	public IResource[] members(IResource resource) throws TeamException {
		if (getBytes(resource) == null) {
			return new IResource[0];
		}
		return super.members(resource);
	}

	@Override
	public boolean setBytes(IResource resource, byte[] bytes) throws TeamException {
		if (!resource.isAccessible() && bytes != null) {
			// just one level of a phantom resources could be created by an ISynchronizer at once
			//	so, we run through all the parents to ensure everything is all right.
			IResource parent = resource.getParent();
			if (parent != null && !parent.isAccessible()) {
				setBytes(parent, new byte[0]);
			}
		}
		return super.setBytes(resource, bytes);
	}

	@Override
	public byte[] getBytes(IResource resource) throws TeamException {
		return super.getBytes(resource);
	}

	@Override
	public boolean flushBytes(IResource resource, int depth) throws TeamException {
		return resource.isAccessible() ? super.flushBytes(resource, depth) : false;
	}

	@Override
	public boolean deleteBytes(IResource resource) throws TeamException {
		return super.deleteBytes(resource);
	}

	@Override
	public void traverse(IResource[] resources, int depth, ICacheVisitor visitor) throws TeamException {
		for (IResource element : resources) {
			this.traverse(element, depth, visitor);
		}
	}

	protected void traverse(IResource resource, int depth, ICacheVisitor visitor) throws TeamException {
		IPath base = resource.getFullPath();
		traverseImpl(base, resource, depth, visitor);
	}

	protected void traverseImpl(IPath base, IResource resource, int depth, ICacheVisitor visitor) throws TeamException {
		byte[] data = getBytes(resource);
		if (data != null && isChildOf(base, resource.getFullPath(), depth)) {
			visitor.visit(resource.getFullPath(), data);
		}
		if (depth != IResource.DEPTH_ZERO) {
			IResource[] resources = members(resource);
			for (IResource res : resources) {
				traverseImpl(base, res, depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE,
						visitor);
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

	protected IResource[] roots() {
		return UpdateSubscriber.instance().roots();
	}

}
