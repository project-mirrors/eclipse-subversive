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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.compare;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;

/**
 * Panel for properties comparison in a dialog
 * 
 * @author Alexei Goncharov
 */
public class PropertyComparePanel extends AbstractDialogPanel {

	protected PropertyCompareInput input;

	public PropertyComparePanel(PropertyCompareInput input, boolean local) {
		super(new String[] { IDialogConstants.CLOSE_LABEL });
		this.input = input;
		dialogDescription = SVNUIMessages.ComparePropsPanel_Description;
		if (local) {
			defaultMessage = SVNUIMessages.ComparePropsPanel_Local_Message;
		} else {
			defaultMessage = SVNUIMessages.ComparePropsPanel_Remote_Message;
		}
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		Control control = input.createContents(parent);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		Shell shell = control.getShell();
		shell.setText(input.getTitle());
		shell.setImage(input.getTitleImage());
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.comparePropsDialogContext"; //$NON-NLS-1$
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(650, 500);
	}

	@Override
	protected void saveChangesImpl() {
	}

	@Override
	protected void cancelChangesImpl() {
	}

}
