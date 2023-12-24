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

package org.eclipse.team.svn.ui.panel;

import org.eclipse.jface.dialogs.IDialogConstants;

/**
 * Abstract advanced dialog panel
 * 
 * @author Sergiy Logvin
 */
public abstract class AbstractAdvancedDialogPanel extends AbstractDialogPanel {
	protected String[] buttonNamesEx;

	public AbstractAdvancedDialogPanel() {
		this(new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL },
				new String[] { IDialogConstants.SHOW_DETAILS_LABEL });
	}

	public AbstractAdvancedDialogPanel(String[] buttonNames, String[] buttonNamesEx) {
		super(buttonNames);
		this.buttonNamesEx = buttonNamesEx;
	}

	public String[] getButtonNamesEx() {
		return this.buttonNamesEx;
	}

	public void extendedButtonPressed(int idx) {
		if (idx == 0) {
			this.showDetails();
		}
	}

	protected abstract void showDetails();
}
