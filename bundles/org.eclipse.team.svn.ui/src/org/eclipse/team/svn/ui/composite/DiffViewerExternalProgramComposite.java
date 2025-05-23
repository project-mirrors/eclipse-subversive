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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.common.DiffViewerVariablesPanel;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * Allow to set external program path and arguments
 * 
 * @author Igor Burilo
 */
public class DiffViewerExternalProgramComposite extends Composite {

	protected IValidationManager validationManager;

	protected String groupLabel;

	protected List<Control> controls = new ArrayList<>();

	protected PathSelectionComposite pathComposite;

	protected Text parametersText;

	protected String programParameters;

	public DiffViewerExternalProgramComposite(String groupLabel, Composite parent,
			IValidationManager validationManager) {
		super(parent, SWT.NONE);
		this.groupLabel = groupLabel;
		this.validationManager = validationManager;
		createControls();
	}

	public void setProgramPath(String programPath) {
		if (programPath != null) {
			pathComposite.setSelectedPath(programPath);
		}
	}

	public String getProgramPath() {
		return pathComposite.getSelectedPath();
	}

	public String getProgramParameters() {
		return programParameters;
	}

	public void setProgramParameters(String programParameters) {
		if (programParameters != null) {
			parametersText.setText(programParameters);
		}
	}

	protected void createControls() {
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		setLayout(layout);
		setLayoutData(data);

		Group parametersGroup = new Group(this, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 1;
		data = new GridData(GridData.FILL_HORIZONTAL);
		parametersGroup.setLayout(layout);
		parametersGroup.setLayoutData(data);
		parametersGroup.setText(groupLabel);

		//program path
		pathComposite = new PathSelectionComposite(
				SVNUIMessages.DiffViewerExternalProgramComposite_Path_LabelName,
				SVNUIMessages.DiffViewerExternalProgramComposite_Path_FieldName,
				SVNUIMessages.DiffViewerExternalProgramComposite_Path_BrowseDialogTitle, null, false, parametersGroup,
				validationManager);
		controls.add(pathComposite);

		//parameters
		parametersText = new Text(parametersGroup, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = DefaultDialog.convertHeightInCharsToPixels(parametersText, 5);
		parametersText.setLayoutData(data);
		controls.add(parametersText);

		Button variablesButton = new Button(parametersGroup, SWT.PUSH);
		variablesButton.setText(SVNUIMessages.DiffViewerExternalProgramComposite_Variables_Button);
		data = new GridData();
		data.horizontalAlignment = SWT.RIGHT;
		data.widthHint = DefaultDialog.computeButtonWidth(variablesButton);
		variablesButton.setLayoutData(data);
		controls.add(variablesButton);

		//handlers

		variablesButton.addListener(SWT.Selection, event -> {
			DiffViewerVariablesPanel panel = new DiffViewerVariablesPanel();
			DefaultDialog dlg = new DefaultDialog(DiffViewerExternalProgramComposite.this.getShell(), panel);
			if (dlg.open() == 0) {
				String variable = panel.getVariable();
				parametersText.insert(variable);
			}
		});

		parametersText.addModifyListener(e -> programParameters = ((Text) e.widget).getText());
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		for (Control control : controls) {
			control.setEnabled(enabled);
		}
	}
}
