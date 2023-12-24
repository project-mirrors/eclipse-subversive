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

package org.eclipse.team.svn.ui.properties;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Remove property dialog
 * 
 * @author Alexander Gurov
 */
public class RemovePropertyDialog extends MessageDialog {

	protected boolean recursive;

	protected boolean isFile;

	public RemovePropertyDialog(Shell parentShell, boolean oneProperty, boolean isFile) {
		super(parentShell,
				oneProperty ? SVNUIMessages.RemoveProperty_Title_Single : SVNUIMessages.RemoveProperty_Title_Multi,
				null,
				oneProperty ? SVNUIMessages.RemoveProperty_Message_Single : SVNUIMessages.RemoveProperty_Message_Multi,
				MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);

		recursive = false;
		this.isFile = isFile;
	}

	public boolean isRecursive() {
		return recursive;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		if (!isFile) {
			Button recursive = new Button(composite, SWT.CHECK);
			recursive.setLayoutData(new GridData());
			recursive.setSelection(false);
			recursive.setText(SVNUIMessages.RemoveProperty_Recursively);
			recursive.addListener(SWT.Selection, event -> {
				Button button = (Button) event.widget;
				RemovePropertyDialog.this.recursive = button.getSelection();
			});
		}

		return composite;
	}

}
