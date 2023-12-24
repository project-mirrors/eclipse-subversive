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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.dialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Property ovewrite warning dialog
 *
 * @author Sergiy Logvin
 */
public class SetPropertyWithOverrideDialog extends MessageDialog {

	public SetPropertyWithOverrideDialog(Shell parentShell, String existingName) {
		super(parentShell,
			SVNUIMessages.SetPropertyWithOverrideDialog_Title, 
			null, 
			SVNUIMessages.format(SVNUIMessages.SetPropertyWithOverrideDialog_Message, new String[] {existingName}),
			MessageDialog.QUESTION, 
			new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 
			0);
	}
		
}