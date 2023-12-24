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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ExternalProgramParameters;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ResourceSpecificParameterKind;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ResourceSpecificParameters;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.DiffViewerExternalProgramComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractValidationManagerProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;

/**
 * Edit file associations with external compare editor panel
 * 
 * @author Igor Burilo
 */
public class EditFileAssociationsPanel extends AbstractDialogPanel {

	protected DiffViewerSettings diffSettings;

	protected ResourceSpecificParameters param;

	protected Text extensionText;

	protected DiffViewerExternalProgramComposite diffExternalComposite;

	protected DiffViewerExternalProgramComposite mergeExternalComposite;

	public EditFileAssociationsPanel(ResourceSpecificParameters param, DiffViewerSettings diffSettings) {
		this.param = param;
		this.diffSettings = diffSettings;

		dialogTitle = this.param == null
				? SVNUIMessages.EditFileAssociationsPanel_AddDialogTitle
				: SVNUIMessages.EditFileAssociationsPanel_EditDialogTitle;
		dialogDescription = SVNUIMessages.EditFileAssociationsPanel_DialogDescription;
		defaultMessage = SVNUIMessages.EditFileAssociationsPanel_DialogDefaultMessage;
	}

	public ResourceSpecificParameters getResourceSpecificParameters() {
		return param;
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayout(layout);
		composite.setLayoutData(data);

		//extension or mime-type
		Label extensionLabel = new Label(composite, SWT.NONE);
		data = new GridData();
		extensionLabel.setLayoutData(data);
		extensionLabel.setText(SVNUIMessages.EditFileAssociationsPanel_ExtensionMimeType_Label);

		extensionText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		data = new GridData();
		data.widthHint = 100;
		extensionText.setLayoutData(data);

		diffExternalComposite = new DiffViewerExternalProgramComposite(
				SVNUIMessages.DiffViewerExternalProgramComposite_DiffProgramArguments_Label, composite, this);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		diffExternalComposite.setLayoutData(data);

		mergeExternalComposite = new DiffViewerExternalProgramComposite(
				SVNUIMessages.DiffViewerExternalProgramComposite_MergeProgramArguments_Label, composite,
				new AbstractValidationManagerProxy(this) {
					@Override
					protected boolean isVerificationEnabled(Control input) {
						return false;
					}
				});
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		mergeExternalComposite.setLayoutData(data);

		CompositeVerifier cmpVerifier = new CompositeVerifier();
		cmpVerifier.add(new NonEmptyFieldVerifier(SVNUIMessages.EditFileAssociationsPanel_ExtensionMimeType_FieldName));
		cmpVerifier.add(
				new AbstractFormattedVerifier(SVNUIMessages.EditFileAssociationsPanel_ExtensionMimeType_FieldName) {
					@Override
					protected String getErrorMessageImpl(Control input) {
						String kindString = ((Text) input).getText();
						ResourceSpecificParameterKind kind = ResourceSpecificParameterKind.getKind(kindString);
						ResourceSpecificParameters resourceParams = diffSettings.getResourceSpecificParameters(kind);
						if (resourceParams != null && (param != null && !param.kind.equals(kind) || param == null)) {
							return SVNUIMessages.EditFileAssociationsPanel_DuplicateExtension_Verifier_Error;
						}
						return null;
					}

					@Override
					protected String getWarningMessageImpl(Control input) {
						return null;
					}
				});
		attachTo(extensionText, cmpVerifier);

		//init value
		if (param != null) {
			if (param.kind.kindValue != null) {
				extensionText.setText(param.kind.formatKindValue());
			}
			diffExternalComposite.setProgramPath(param.params.diffProgramPath);
			diffExternalComposite.setProgramParameters(param.params.diffParamatersString);

			mergeExternalComposite.setProgramPath(param.params.mergeProgramPath);
			mergeExternalComposite.setProgramParameters(param.params.mergeParamatersString);
		}
	}

	@Override
	protected void saveChangesImpl() {
		String extensionStr = extensionText.getText();
		ResourceSpecificParameterKind kind = ResourceSpecificParameterKind.getKind(extensionStr);

		ExternalProgramParameters externalProgramParams = new ExternalProgramParameters(
				diffExternalComposite.getProgramPath(), mergeExternalComposite.getProgramPath(),
				diffExternalComposite.getProgramParameters(), mergeExternalComposite.getProgramParameters());

		if (param == null) {
			param = new ResourceSpecificParameters(kind, externalProgramParams);
			param.isEnabled = true;
		} else {
			param.kind = kind;
			param.params = externalProgramParams;
		}
	}

	@Override
	protected void cancelChangesImpl() {
		param = null;
	}

}
