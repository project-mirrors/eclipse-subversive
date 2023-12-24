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
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.local.IUnresolvedConflictDetector;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.NotifyUnresolvedConflictDialog;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * This operation provide ability to notify user about unresolved conflict situation in time of update/merge
 * 
 * @author Alexander Gurov
 */
public class NotifyUnresolvedConflictOperation extends AbstractActionOperation {
	protected IUnresolvedConflictDetector sign;

	public NotifyUnresolvedConflictOperation(IUnresolvedConflictDetector sign) {
		super("Operation_NotifyConflicts", SVNUIMessages.class); //$NON-NLS-1$
		this.sign = sign;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (sign.hasUnresolvedConflicts()) {
			UIMonitorUtility.getDisplay().syncExec(() -> new NotifyUnresolvedConflictDialog(UIMonitorUtility.getShell(), sign.getMessage()).open());
		}
	}

}
