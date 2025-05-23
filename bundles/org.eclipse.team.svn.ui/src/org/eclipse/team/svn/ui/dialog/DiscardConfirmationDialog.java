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
 * Discard location confirmation dialog
 * 
 * @author Alexander Gurov
 */
public class DiscardConfirmationDialog extends MessageDialog {
	public static final int MSG_LOCATION = 0;

	public static final int MSG_LINK = 1;

	public static final int MSG_RESOURCE = 2;

	protected static final String[][] MESSAGES = {
			new String[] { "DiscardConfirmationDialog_Location_Title", "DiscardConfirmationDialog_Location_Message" }, //$NON-NLS-1$ //$NON-NLS-2$
			new String[] { "DiscardConfirmationDialog_RevisionLink_Title", //$NON-NLS-1$
					"DiscardConfirmationDialog_RevisionLink_Message" }, //$NON-NLS-1$
			new String[] { "DiscardConfirmationDialog_Resource_Title", "DiscardConfirmationDialog_Resource_Message" } //$NON-NLS-1$ //$NON-NLS-2$
	};

	public DiscardConfirmationDialog(Shell parentShell, boolean oneLocation, int msgSelector) {
		super(parentShell, SVNUIMessages.getString(
				DiscardConfirmationDialog.MESSAGES[msgSelector][0] + (oneLocation ? "_Single" : "_Multi")), //$NON-NLS-1$ //$NON-NLS-2$
				null, SVNUIMessages.getString(
						DiscardConfirmationDialog.MESSAGES[msgSelector][1] + (oneLocation ? "_Single" : "_Multi")), //$NON-NLS-1$ //$NON-NLS-2$
				MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
	}

}
