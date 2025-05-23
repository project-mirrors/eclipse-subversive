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
import org.eclipse.team.svn.core.connector.SVNCommitStatus;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IPostCommitErrorsProvider;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.ShowPostCommitErrorsDialog;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * This operation provide ability to notify user about unresolved conflict situation in time of update/merge
 * 
 * @author Alexander Gurov
 */
public class ShowPostCommitErrorsOperation extends AbstractActionOperation {
	protected IPostCommitErrorsProvider provider;

	public ShowPostCommitErrorsOperation(IPostCommitErrorsProvider provider) {
		super("Operation_ShowPostCommitErrors", SVNUIMessages.class); //$NON-NLS-1$
		this.provider = provider;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		SVNCommitStatus[] errors = provider.getPostCommitErrors();
		if (errors != null) {
			String tCompleteMessage = null;
			for (SVNCommitStatus error : errors) {
				tCompleteMessage = tCompleteMessage == null ? error.message : tCompleteMessage + "\n\n" + error.message; //$NON-NLS-1$
			}
			if (tCompleteMessage != null) {
				final String completeMessage = tCompleteMessage;
				UIMonitorUtility.getDisplay().syncExec(() -> new ShowPostCommitErrorsDialog(UIMonitorUtility.getShell(), completeMessage).open());
			}
		}
	}

}
