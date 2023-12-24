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

package org.eclipse.team.svn.ui.panel.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;

/**
 * Pre-defined variables selection panel for diff viewer's external program
 * 
 * @author Igor Burilo
 */
public class DiffViewerVariablesPanel extends AbstractDialogPanel {

	protected List variablesList;

	protected Text variableDescription;

	protected Map<String, String> variablesMap = new LinkedHashMap<>();

	protected String variable;

	public DiffViewerVariablesPanel() {
		dialogTitle = SVNUIMessages.DiffViewerVariablesPanel_DialogTitle;
		dialogDescription = SVNUIMessages.DiffViewerVariablesPanel_DialogDescription;
		defaultMessage = SVNUIMessages.DiffViewerVariablesPanel_DialogDefaultMessage;

		variablesMap.put("base", SVNUIMessages.DiffViewerVariablesPanel_BaseVariable_Description); //$NON-NLS-1$
		variablesMap.put("mine", SVNUIMessages.DiffViewerVariablesPanel_MineVariable_Description); //$NON-NLS-1$
		variablesMap.put("theirs", SVNUIMessages.DiffViewerVariablesPanel_TheirsVariable_Description); //$NON-NLS-1$
		variablesMap.put("merged", SVNUIMessages.DiffViewerVariablesPanel_MergedVariable_Description); //$NON-NLS-1$

		variablesMap.put("default-doc-program", //$NON-NLS-1$
				BaseMessages.format(SVNUIMessages.DiffViewerVariablesPanel_DefaultVariable_Description,
						SVNUIMessages.DiffViewerVariablesPanel_DefaultDocVariable_Program));
		variablesMap.put("default-xls-program", //$NON-NLS-1$
				BaseMessages.format(SVNUIMessages.DiffViewerVariablesPanel_DefaultVariable_Description,
						SVNUIMessages.DiffViewerVariablesPanel_DefaultXlsVariable_Program));
		variablesMap.put("default-ppt-program", //$NON-NLS-1$
				BaseMessages.format(SVNUIMessages.DiffViewerVariablesPanel_DefaultVariable_Description,
						SVNUIMessages.DiffViewerVariablesPanel_DefaultPptVariable_Program));

		variablesMap.put("default-odt-program", //$NON-NLS-1$
				BaseMessages.format(SVNUIMessages.DiffViewerVariablesPanel_DefaultVariable_Description,
						SVNUIMessages.DiffViewerVariablesPanel_DefaultOdtVariable_Program));
		variablesMap.put("default-ods-program", //$NON-NLS-1$
				BaseMessages.format(SVNUIMessages.DiffViewerVariablesPanel_DefaultVariable_Description,
						SVNUIMessages.DiffViewerVariablesPanel_DefaultOdsVariable_Program));
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 1;
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayout(layout);
		composite.setLayoutData(data);

		variablesList = new List(composite, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = DefaultDialog.convertHeightInCharsToPixels(variablesList, 10);
		variablesList.setLayoutData(data);

		Label variableDescriptionLabel = new Label(composite, SWT.NONE);
		data = new GridData();
		variableDescriptionLabel.setLayoutData(data);
		variableDescriptionLabel.setText(SVNUIMessages.DiffViewerVariablesPanel_VariableDescriptionLabel);

		variableDescription = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = DefaultDialog.convertHeightInCharsToPixels(variableDescription, 4);
		variableDescription.setLayoutData(data);
		variableDescription.setBackground(variableDescription.getBackground());
		variableDescription.setEditable(false);

		//handlers

		variablesList.addListener(SWT.Selection, event -> {
			String variableName = DiffViewerVariablesPanel.this.getSelectedVariable();
			if (variableName != null) {
				//init description
				variableDescription.setText(variablesMap.get(variableName));

				//run validation
				DiffViewerVariablesPanel.this.validateContent();
			}
		});

		initializeControls();
	}

	protected String getSelectedVariable() {
		String variable = null;
		String[] selected = DiffViewerVariablesPanel.this.variablesList.getSelection();
		if (selected.length > 0) {
			variable = selected[0];
		}
		return variable;
	}

	protected void initializeControls() {
		String firstVariable = null;
		for (String variableName : variablesMap.keySet()) {
			variablesList.add(variableName);
			if (firstVariable == null) {
				firstVariable = variableName;
			}
		}
		variablesList.select(0);
		variableDescription.setText(variablesMap.get(firstVariable));
	}

	@Override
	protected void saveChangesImpl() {
		String var = getSelectedVariable();
		variable = var != null ? "\"${" + var + "}\"" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	protected void cancelChangesImpl() {
		variable = null;
	}

	public String getVariable() {
		return variable;
	}

}
