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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.change.IActionOperationProcessor;
import org.eclipse.team.svn.core.operation.local.change.IResourceChangeVisitor;
import org.eclipse.team.svn.core.operation.local.change.ResourceChange;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Specified resources traversal operation. Usage sample: new ResourcesTraversalOperation(resources, new RemoveNonVersionedVisitor(true),
 * IResource.DEPTH_INFINITE)
 * 
 * @author Alexander Gurov
 */
public class ResourcesTraversalOperation extends AbstractWorkingCopyOperation implements IActionOperationProcessor {
	protected IResourceChangeVisitor visitor;

	protected int depth;

	public ResourcesTraversalOperation(String operationName, Class<? extends NLS> messagesClass, IResource[] resources,
			IResourceChangeVisitor visitor, int depth) {
		super(operationName, messagesClass, resources);
		this.visitor = visitor;
		this.depth = depth;
	}

	public ResourcesTraversalOperation(String operationName, Class<? extends NLS> messagesClass,
			IResourceProvider provider, IResourceChangeVisitor visitor, int depth) {
		super(operationName, messagesClass, provider);
		this.visitor = visitor;
		this.depth = depth;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();
		resources = depth == IResource.DEPTH_ZERO ? resources : FileUtility.shrinkChildNodes(resources);
		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final IResource current = resources[i];
			this.protectStep(monitor1 -> {
				ResourceChange change = ResourceChange.wrapLocalResource(null,
						SVNRemoteStorage.instance().asLocalResourceAccessible(current), false);
				if (change != null) {
					change.traverse(visitor, depth, ResourcesTraversalOperation.this, monitor1);
				}
			}, monitor, resources.length);
		}
	}

	@Override
	public void doOperation(IActionOperation op, IProgressMonitor monitor) {
		this.reportStatus(op.run(monitor).getStatus());
	}

}
