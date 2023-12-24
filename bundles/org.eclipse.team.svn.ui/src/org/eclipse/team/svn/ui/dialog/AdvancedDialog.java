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

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.ui.panel.AbstractAdvancedDialogPanel;
import org.eclipse.team.svn.ui.panel.IDialogManagerEx;

/**
 * Advanced dialog implementation
 * 
 * @author Sergiy Logvin
 */
public class AdvancedDialog extends DefaultDialog implements IDialogManagerEx {
	protected Button[] buttonsEx;

	protected String[] buttonLabelsEx;

	protected int basePanelButtonsCount;

	protected int focusButtonIdx;

	public AdvancedDialog(Shell parentShell, AbstractAdvancedDialogPanel panel) {
		super(parentShell, panel);
		basePanelButtonsCount = panel.getButtonNames().length;
		buttonLabelsEx = panel.getButtonNamesEx();
	}

	public AdvancedDialog(Shell parentShell, AbstractAdvancedDialogPanel panel, int focusButtonIdx) {
		super(parentShell, panel);
		basePanelButtonsCount = panel.getButtonNames().length;
		buttonLabelsEx = panel.getButtonNamesEx();
		this.focusButtonIdx = focusButtonIdx;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId < basePanelButtonsCount) {
			baseButtonPressed(buttonId);
		} else {
			extendedButtonPressed(buttonId - basePanelButtonsCount);
		}
	}

	protected void baseButtonPressed(int buttonId) {
		super.buttonPressed(buttonId);
	}

	protected void extendedButtonPressed(int buttonId) {
		((AbstractAdvancedDialogPanel) panel).extendedButtonPressed(buttonId);
	}

	@Override
	protected Control createButtonPanel(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite buttonPanel = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		buttonPanel.setLayout(layout);

		data = new GridData(GridData.FILL_HORIZONTAL);
		buttonPanel.setLayoutData(data);

		createExtendedButtonPanel(buttonPanel);
		createBaseButtonPanel(buttonPanel);

		ArrayList<Button> allButtons = new ArrayList<>();
		for (int i = 0; i < getButtonLabels().length; i++) {
			allButtons.add(getButton(i));
		}
		setButtons(allButtons.toArray(new Button[allButtons.size()]));
		if (focusButtonIdx != 0) {
			getShell().setDefaultButton(getButton(focusButtonIdx));
		}
		return buttonPanel;
	}

	protected Control createExtendedButtonPanel(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite buttonPanel = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonPanel.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalAlignment = SWT.LEFT;
		buttonPanel.setLayoutData(data);

		return createExtendedButtonBar(buttonPanel);
	}

	protected Control createBaseButtonPanel(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite buttonPanel = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonPanel.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalAlignment = SWT.RIGHT;
		buttonPanel.setLayoutData(data);

		return createButtonBar(buttonPanel);
	}

	protected Control createExtendedButtonBar(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		// create a layout with spacing and margins appropriate for the font
		// size.
		GridLayout layout = new GridLayout();
		layout.numColumns = 0; // this is incremented by createButton
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
		data.horizontalSpan = 2;
		composite.setLayoutData(data);
		composite.setFont(parent.getFont());
		// Add the buttons to the left button bar.
		createButtonsForExtendedButtonBar(composite);
		return composite;
	}

	protected void createButtonsForExtendedButtonBar(Composite parent) {
		buttonsEx = new Button[buttonLabelsEx.length];
		for (int i = 0; i < buttonsEx.length; i++) {
			String label = buttonLabelsEx[i];
			Button button = createButton(parent, basePanelButtonsCount + i, label, false);
			buttonsEx[i] = button;
		}
	}

	public Button getButtonEx(int idx) {
		return buttonsEx[idx];
	}

	public void setButtonEx(Button[] newButtons) {
		buttonsEx = newButtons;
	}

	@Override
	public void setExtendedButtonEnabled(int idx, boolean enabled) {
		buttonsEx[idx].setEnabled(enabled);
	}

	@Override
	public void setExtendedButtonCaption(int idx, String text) {
		buttonsEx[idx].setText(text);
	}

}
