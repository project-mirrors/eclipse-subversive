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

package org.eclipse.team.svn.core.operation.local.change;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.resource.ILocalFolder;
import org.eclipse.team.svn.core.resource.ILocalResource;

/**
 * Local folder change store
 * 
 * @author Alexander Gurov
 */
public class FolderChange extends ResourceChange {
	protected ResourceChange[] children;

	public FolderChange(ResourceChange parent, ILocalFolder local, boolean needsTemporary) {
		super(parent, local, needsTemporary);
		if (needsTemporary) {
			tmp.mkdir();
		}
		ILocalResource[] tmpChildren = local.getChildren();
		children = new ResourceChange[tmpChildren.length];
		for (int i = 0; i < tmpChildren.length; i++) {
			children[i] = ResourceChange.wrapLocalResource(this, tmpChildren[i], needsTemporary);
		}
	}

	public ResourceChange[] getChildren() {
		return children;
	}

	@Override
	protected void preTraverse(IResourceChangeVisitor visitor, int depth, IActionOperationProcessor processor,
			IProgressMonitor monitor) throws Exception {
		if (depth != IResource.DEPTH_ZERO) {
			int nextDepth = depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
			for (int i = 0; i < children.length && !monitor.isCanceled(); i++) {
				children[i].preTraverse(visitor, nextDepth, processor, monitor);
			}
		}
		visitor.preVisit(this, processor, monitor);
	}

	@Override
	protected void postTraverse(IResourceChangeVisitor visitor, int depth, IActionOperationProcessor processor,
			IProgressMonitor monitor) throws Exception {
		visitor.postVisit(this, processor, monitor);
		if (depth != IResource.DEPTH_ZERO) {
			int nextDepth = depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
			for (int i = 0; i < children.length && !monitor.isCanceled(); i++) {
				children[i].postTraverse(visitor, nextDepth, processor, monitor);
			}
		}
	}

}
