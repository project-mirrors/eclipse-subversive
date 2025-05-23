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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Disconnect from repository confirmation dialog
 * 
 * @author Alexander Gurov
 */
public class DisconnectDialog extends MessageDialog {

	protected boolean dropSVNFolders;

	public DisconnectDialog(Shell parentShell, IProject[] projects) {
		super(parentShell, SVNUIMessages.DisconnectDialog_Title, null,
				BaseMessages.format(SVNUIMessages.DisconnectDialog_Message,
						new String[] { FileUtility.getNamesListAsString(projects) }),
				MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);

		dropSVNFolders = false;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		Button dropSVNFoldersButton = new Button(composite, SWT.RADIO);
		dropSVNFoldersButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
				dropSVNFolders = button.getSelection();
			}
		});

		dropSVNFoldersButton.setText(SVNUIMessages.DisconnectDialog_Option_dropSVNMeta);

		Button leaveSVNFoldersButton = new Button(composite, SWT.RADIO);

		leaveSVNFoldersButton.setText(SVNUIMessages.DisconnectDialog_Option_leaveSVNMeta);

		// set initial state
		dropSVNFoldersButton.setSelection(false);
		leaveSVNFoldersButton.setSelection(true);

//		WorkbenchHelp.setHelp(composite, IHelpContextIds.DISCONNECT_ACTION);

		return composite;
	}

	public boolean dropSVNFolders() {
		return dropSVNFolders;
	}

}
