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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.RemoteResourceTransfer;
import org.eclipse.team.svn.ui.RemoteResourceTransferrable;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Copy/Cut remote resources into clipboard operation implementation
 * 
 * @author Alexander Gurov
 */
public class PrepareRemoteResourcesTransferrableOperation extends AbstractActionOperation {
	protected IRepositoryResource[] resources;

	protected Display display;

	protected int operation;

	public PrepareRemoteResourcesTransferrableOperation(IRepositoryResource[] resources, int operation,
			Display display) {
		super("Operation_FillCopyPaste", SVNUIMessages.class); //$NON-NLS-1$
		this.resources = resources;
		this.display = display;
		this.operation = operation;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		Clipboard clipboard = new Clipboard(display);
		try {
			clipboard.setContents(
					new Object[] { new RemoteResourceTransferrable(resources, operation) },
					new Transfer[] { RemoteResourceTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

}
