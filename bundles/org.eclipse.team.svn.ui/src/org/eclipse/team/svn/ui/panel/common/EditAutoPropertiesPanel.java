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
 *    Alexey Mikoyan - Initial implementation
 *    Gabor Liptak - Speedup Pattern's usage
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPropsPreferencePage;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.FileNameTemplateVerifier;
import org.eclipse.team.svn.ui.verifier.MultiLinePropertyVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;

/**
 * Edit automatic properties panel
 *
 * @author Alexey Mikoyan
 *
 */
public class EditAutoPropertiesPanel extends AbstractDialogPanel {

	protected SVNTeamPropsPreferencePage.AutoProperty property;

	protected Text txtFileName;

	protected Text txtProperties;

	protected String fileName;

	protected String properties;

	public EditAutoPropertiesPanel(SVNTeamPropsPreferencePage.AutoProperty property) {
		this.property = property;
		dialogTitle = property == null
				? SVNUIMessages.EditAutoPropertiesPanel_Title_Add
				: SVNUIMessages.EditAutoPropertiesPanel_Title_Edit;
		dialogDescription = SVNUIMessages.EditAutoPropertiesPanel_Description;
		defaultMessage = SVNUIMessages.EditAutoPropertiesPanel_Message;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridLayout layout;
		GridData layoutData;
		Label label;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginBottom = 5;
		composite.setLayout(layout);
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(layoutData);

		label = new Label(composite, SWT.NONE);
		label.setText(SVNUIMessages.EditAutoPropertiesPanel_FileName);

		txtFileName = new Text(composite, SWT.BORDER);
		txtFileName.setText(property == null ? "" : property.fileName); //$NON-NLS-1$
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		txtFileName.setLayoutData(layoutData);
		String fieldName = SVNUIMessages.EditAutoPropertiesPanel_FileName_Verifier;
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(fieldName));
		verifier.add(new AbstractVerifierProxy(new FileNameTemplateVerifier(fieldName)) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return txtFileName.getText().trim().length() > 0;
			}
		});
		attachTo(txtFileName, verifier);

		Group group = new Group(parent, SWT.NONE);
		group.setText(SVNUIMessages.EditAutoPropertiesPanel_Properties);
		layoutData = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(layoutData);
		layout = new GridLayout();
		group.setLayout(layout);

		label = new Label(group, SWT.NONE);
		label.setText(SVNUIMessages.EditAutoPropertiesPanel_Properties_Hint);

		txtProperties = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		txtProperties.setText(property == null ? "" : property.properties.trim()); //$NON-NLS-1$
		layoutData = new GridData(GridData.FILL_BOTH);
		layoutData.heightHint = DefaultDialog.convertHeightInCharsToPixels(txtProperties, 7);
		txtProperties.setLayoutData(layoutData);
		attachTo(txtProperties, new AbstractVerifierProxy(
				new MultiLinePropertyVerifier(SVNUIMessages.EditAutoPropertiesPanel_Properties_Verifier)) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return txtProperties.getText().trim().length() > 0;
			}
		});
	}

	@Override
	protected void cancelChangesImpl() {
	}

	@Override
	protected void saveChangesImpl() {
		fileName = txtFileName.getText().trim();
		properties = txtProperties.getText().trim();
	}

	public String getFileName() {
		return fileName;
	}

	public String getProperties() {
		return properties;
	}

}
