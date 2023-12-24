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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourcePathVerifier;

/**
 * Path selection composite Contain path label and text input with browse button Allow to select either file or directory
 * 
 * @author Igor Burilo
 */
public class PathSelectionComposite extends Composite {

	protected IValidationManager validationManager;

	protected boolean isDirectorySelection;

	protected String pathLabelName;

	protected String pathFieldName;

	protected String browseDialogTitle;

	protected String browseDialogDescription;

	protected Text pathInput;

	protected String selectedPath;

	protected List<Control> controls = new ArrayList<>();

	public PathSelectionComposite(String pathLabelName, String pathFieldName, String browseDialogTitle,
			String browseDialogDescription, boolean isDirectorySelection, Composite parent,
			IValidationManager validationManager) {
		super(parent, SWT.NONE);
		this.isDirectorySelection = isDirectorySelection;
		this.validationManager = validationManager;
		this.pathLabelName = pathLabelName;
		this.pathFieldName = pathFieldName;
		this.browseDialogTitle = browseDialogTitle;
		this.browseDialogDescription = browseDialogDescription;

		createControls();
	}

	public void setSelectedPath(String selectedPath) {
		if (selectedPath != null) {
			pathInput.setText(selectedPath);
		}
	}

	public String getSelectedPath() {
		return selectedPath;
	}

	protected void createControls() {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 3;
		setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		setLayoutData(data);

		Label pathLabel = new Label(this, SWT.NONE);
		data = new GridData();
		pathLabel.setLayoutData(data);
		pathLabel.setText(pathLabelName);
		controls.add(pathLabel);

		pathInput = new Text(this, SWT.BORDER | SWT.SINGLE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		pathInput.setLayoutData(data);
		controls.add(pathInput);

		Button browseButton = new Button(this, SWT.PUSH);
		browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
		browseButton.setLayoutData(data);
		controls.add(browseButton);

		//validation
		String name = pathFieldName;
		CompositeVerifier cVerifier = new CompositeVerifier();
		cVerifier.add(new NonEmptyFieldVerifier(name));
		cVerifier.add(new ResourcePathVerifier(name));
		validationManager.attachTo(pathInput, cVerifier);

		pathInput.addModifyListener(e -> selectedPath = pathInput.getText());

		browseButton.addListener(SWT.Selection, event -> {
			if (isDirectorySelection) {
				DirectoryDialog dlg = new DirectoryDialog(PathSelectionComposite.this.getShell());
				dlg.setText(browseDialogTitle);
				dlg.setMessage(browseDialogDescription);
				String path = dlg.open();
				if (path != null) {
					pathInput.setText(path);
				}
			} else {
				FileDialog dlg = new FileDialog(PathSelectionComposite.this.getShell());
				dlg.setText(browseDialogTitle);
				String path = dlg.open();
				if (path != null) {
					pathInput.setText(path);
				}
			}
		});
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		for (Control control : controls) {
			control.setEnabled(enabled);
		}
	}
}
