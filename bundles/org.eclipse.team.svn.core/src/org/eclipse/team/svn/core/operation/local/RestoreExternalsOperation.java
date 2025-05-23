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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.change.IActionOperationProcessor;
import org.eclipse.team.svn.core.operation.local.change.ResourceChange;
import org.eclipse.team.svn.core.operation.local.change.visitors.RestorePropertiesVisitor;

/**
 * Restores svn:externals to its original values
 * 
 * @author Alexander Gurov
 */
public class RestoreExternalsOperation extends AbstractWorkingCopyOperation implements IActionOperationProcessor {
	protected FreezeExternalsOperation freezeOp;

	public RestoreExternalsOperation(FreezeExternalsOperation freezeOp) {
		super("Operation_RestoreExternals", SVNMessages.class, freezeOp); //$NON-NLS-1$
		this.freezeOp = freezeOp;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		final RestorePropertiesVisitor visitor = new RestorePropertiesVisitor();

		for (Iterator<?> it = freezeOp.getChanges().iterator(); it.hasNext() && !monitor.isCanceled();) {
			final ResourceChange change = (ResourceChange) it.next();
			this.protectStep(monitor1 -> change.traverse(visitor, IResource.DEPTH_ZERO, RestoreExternalsOperation.this, monitor1), monitor, freezeOp.getChanges().size());
		}
	}

	@Override
	public void doOperation(IActionOperation op, IProgressMonitor monitor) {
		this.reportStatus(op.run(monitor).getStatus());
	}

}
