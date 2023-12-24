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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourcePathVerifier;

/**
 * Contain a flag which determines whether to generate diff file and set path to it
 * 
 * @author Igor Burilo
 */
public class DiffFormatComposite extends Composite {

	protected Button generateUDiffCheckbox;

	protected Text uDiffPath;

	protected Button browseButton;

	protected String diffFile;

	protected IValidationManager validationManager;

	public DiffFormatComposite(Composite parent, IValidationManager validationManager) {
		super(parent, SWT.NONE);
		this.validationManager = validationManager;
		createControls();
	}

	public String getDiffFile() {
		return diffFile;
	}

	protected void createControls() {
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		setLayoutData(data);

		generateUDiffCheckbox = new Button(this, SWT.CHECK);
		generateUDiffCheckbox.setText(SVNUIMessages.DiffFormatComposite_GenerateDiffFile_Message);
		data = new GridData();
		generateUDiffCheckbox.setLayoutData(data);

		uDiffPath = new Text(this, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		uDiffPath.setLayoutData(data);

		browseButton = new Button(this, SWT.PUSH);
		browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
		browseButton.setLayoutData(data);

		//validation
		String name = SVNUIMessages.DiffFormatComposite_DiffFile_Name;
		CompositeVerifier cVerifier = new CompositeVerifier();
		cVerifier.add(new NonEmptyFieldVerifier(name));
		cVerifier.add(new ResourcePathVerifier(name));
		validationManager.attachTo(uDiffPath, new AbstractVerifierProxy(cVerifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return generateUDiffCheckbox.getSelection();
			}
		});

		//event handlers:

		generateUDiffCheckbox.addListener(SWT.Selection, e -> {
			validationManager.validateContent();
			DiffFormatComposite.this.setEnablement();
		});

		browseButton.addListener(SWT.Selection, e -> {
			FileDialog dlg = new FileDialog(DiffFormatComposite.this.getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
			dlg.setFilterExtensions(new String[] { "*.diff", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
			dlg.setText(SVNUIMessages.DiffFormatComposite_SaveDiffFileAs);
			String file = dlg.open();
			if (file != null) {
				uDiffPath.setText(file);
			}
		});

		uDiffPath.addModifyListener(e -> diffFile = ((Text) e.widget).getText());

		//set init values and run enablement
		generateUDiffCheckbox.setSelection(false);
		setEnablement();
	}

	protected void setEnablement() {
		boolean enabled = generateUDiffCheckbox.getSelection();
		DiffFormatComposite.this.uDiffPath.setEnabled(enabled);
		DiffFormatComposite.this.browseButton.setEnabled(enabled);
	}
}
