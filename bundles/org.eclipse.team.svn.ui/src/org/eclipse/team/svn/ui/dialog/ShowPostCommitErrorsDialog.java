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

package org.eclipse.team.svn.ui.dialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Unresolved conflict notification dialog
 * 
 * @author Alexander Gurov
 */
public class ShowPostCommitErrorsDialog extends MessageDialog {

    public ShowPostCommitErrorsDialog(Shell parentShell, String message) {
		super(parentShell, 
			SVNUIMessages.ShowPostCommitErrorsDialog_Title, 
			null, 
			message,
			MessageDialog.WARNING, 
			new String[] {IDialogConstants.OK_LABEL}, 
			0);
    }

}
