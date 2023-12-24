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

package org.eclipse.team.svn.ui.composite;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.resource.SSHSettings;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.ExistingResourceVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ProxyPortVerifier;

/**
 * SSH properties editor composite
 *
 * @author Sergiy Logvin
 */
public class SSHComposite extends AbstractDynamicComposite implements IPropertiesPanel {
	protected boolean callback;

	protected Button passwordRadioButton;

	protected Button privateKeyRadioButton;

	protected Text privateKeyFileText;

	protected Button browseButton;

	protected Text passphraseText;

	protected Button savePassphraseCheckBox;

	protected Text portText;

	protected IValidationManager validationManager;

	protected UserInputHistory userHistory;

	protected SSHSettings credentialsInput;

	protected boolean tempOptionsSaved;

	protected boolean tempPassBtnEnabled;

	protected boolean tempKeyBtnEnabled;

	protected boolean tempSavePassChecked;

	public SSHComposite(Composite parent, int style, IValidationManager validationManager) {
		this(parent, style, validationManager, false);
	}

	public SSHComposite(Composite parent, int style, IValidationManager validationManager, boolean callback) {
		super(parent, style);
		this.validationManager = validationManager;
		this.callback = callback;
		credentialsInput = new SSHSettings();
	}

	public SSHSettings getSSHSettingsDirect() {
		SSHSettings settings = new SSHSettings();
		getSSHSettingsDirectImpl(settings);
		return settings;
	}

	public void setSSHSettingsDirect(SSHSettings settings) {
		savePassphraseCheckBox.setSelection(settings.isPassPhraseSaved());
		privateKeyRadioButton.setSelection(settings.isUseKeyFile());
		passwordRadioButton.setSelection(!settings.isUseKeyFile());
		String text = settings.getPassPhrase();
		passphraseText.setText(text == null ? "" : text); //$NON-NLS-1$
		text = settings.getPrivateKeyPath();
		privateKeyFileText.setText(text == null ? "" : text); //$NON-NLS-1$
		portText.setText(String.valueOf(settings.getPort()));

		if (callback && text != null && text.length() > 0) {
			passphraseText.setFocus();
			passphraseText.selectAll();
		}

		refreshControlsEnablement();
	}

	@Override
	public void initialize() {
		GridLayout layout = null;
		GridData data = null;

		layout = new GridLayout();
		layout.marginHeight = 7;
		layout.verticalSpacing = 3;
		setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		setLayoutData(data);

		Composite sshGroup = new Composite(this, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		sshGroup.setLayout(layout);
		sshGroup.setLayoutData(data);

		Label lblPort = new Label(sshGroup, SWT.NONE);
		lblPort.setText(SVNUIMessages.SSHComposite_Port);

		portText = new Text(sshGroup, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		portText.setLayoutData(data);
		CompositeVerifier verifier = new CompositeVerifier();
		String name = SVNUIMessages.SSHComposite_Port_Verifier;
		verifier.add(new NonEmptyFieldVerifier(name));
		verifier.add(new ProxyPortVerifier(name));
		portText.setText(String.valueOf(SSHSettings.SSH_PORT_DEFAULT));
		validationManager.attachTo(portText, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return SSHComposite.this.isVisible();
			}
		});

		Group passGroup = new Group(this, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		layout = new GridLayout();
		layout.verticalSpacing = 12;
		passGroup.setLayout(layout);
		passGroup.setLayoutData(data);
		passGroup.setText(SVNUIMessages.SSHComposite_Authentication);

		Composite inner = new Composite(passGroup, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		inner.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		inner.setLayoutData(data);

		passwordRadioButton = new Button(inner, SWT.RADIO);
		data = new GridData(GridData.BEGINNING);
		passwordRadioButton.setLayoutData(data);
		passwordRadioButton.setText(SVNUIMessages.SSHComposite_Password);

		passwordRadioButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SSHComposite.this.refreshControlsEnablement();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		privateKeyRadioButton = new Button(inner, SWT.RADIO);
		data = new GridData();
		privateKeyRadioButton.setLayoutData(data);
		privateKeyRadioButton.setText(SVNUIMessages.SSHComposite_PrivateKey);
		privateKeyRadioButton.setSelection(false);

		Composite groupInner = new Composite(inner, SWT.FILL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		groupInner.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		groupInner.setLayoutData(data);

		Label description = new Label(groupInner, SWT.NULL);
		description.setText(SVNUIMessages.SSHComposite_File);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		description.setLayoutData(data);

		Composite privateKeyFileComposite = new Composite(groupInner, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 2;
		privateKeyFileComposite.setLayout(layout);
		privateKeyFileComposite.setLayoutData(data);

		privateKeyFileText = new Text(privateKeyFileComposite, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		privateKeyFileText.setLayoutData(data);
		validationManager.attachTo(privateKeyFileText, new AbstractVerifierProxy(
				new ExistingResourceVerifier(SVNUIMessages.SSHComposite_File_Verifier, true)) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return privateKeyRadioButton.getSelection() && SSHComposite.this.isVisible();
			}
		});

		browseButton = new Button(privateKeyFileComposite, SWT.PUSH);
		browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
		browseButton.setLayoutData(data);

		browseButton.addListener(SWT.Selection, event -> {
			FileDialog fileDialog = new FileDialog(SSHComposite.this.getShell(), SWT.OPEN);
			String res = fileDialog.open();
			if (res != null) {
				privateKeyFileText.setText(res);
				validationManager.validateContent();
			}
		});

		description = new Label(groupInner, SWT.NULL);
		description.setText(SVNUIMessages.SSHComposite_Passphrase);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		description.setLayoutData(data);

		passphraseText = new Text(groupInner, SWT.BORDER | SWT.PASSWORD);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		passphraseText.setLayoutData(data);

		Composite savePassphrase = new Composite(passGroup, SWT.FILL);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		savePassphrase.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		savePassphrase.setLayoutData(data);

		savePassphraseCheckBox = new Button(savePassphrase, SWT.CHECK);
		savePassphraseCheckBox.setText(SVNUIMessages.SSHComposite_SavePassphrase);

		new SecurityWarningComposite(savePassphrase);
	}

	public void setCredentialsInput(SSHSettings input) {
		credentialsInput = input;
	}

	@Override
	public void saveChanges() {
		getSSHSettingsDirectImpl(credentialsInput);
	}

	@Override
	public void resetChanges() {
		setSSHSettingsDirect(credentialsInput);
	}

	@Override
	public void cancelChanges() {

	}

	protected void getSSHSettingsDirectImpl(SSHSettings settings) {
		settings.setPassPhraseSaved(savePassphraseCheckBox.getSelection());
		settings.setUseKeyFile(privateKeyRadioButton.getSelection());
		settings.setPassPhrase(passphraseText.getText());
		settings.setPrivateKeyPath(privateKeyFileText.getText());
		String port = portText.getText().trim();
		if (port.length() > 0) {
			settings.setPort(Integer.parseInt(port));
		}
	}

	protected void refreshControlsEnablement() {
		boolean buttonSelected = passwordRadioButton.getSelection();

		buttonSelected = privateKeyRadioButton.getSelection();
		privateKeyFileText.setEnabled(buttonSelected);
		browseButton.setEnabled(buttonSelected);
		passphraseText.setEnabled(buttonSelected);
		savePassphraseCheckBox.setEnabled(buttonSelected);

		validationManager.validateContent();
	}

	@Override
	public void saveAppearance() {
		tempPassBtnEnabled = passwordRadioButton.getSelection();
		tempKeyBtnEnabled = privateKeyRadioButton.getSelection();
		tempSavePassChecked = savePassphraseCheckBox.getSelection();
		tempOptionsSaved = true;
	}

	@Override
	public void restoreAppearance() {
		if (tempOptionsSaved) {
			passwordRadioButton.setSelection(tempPassBtnEnabled);
			privateKeyRadioButton.setSelection(tempKeyBtnEnabled);
			savePassphraseCheckBox.setSelection(tempSavePassChecked);
			tempOptionsSaved = false;
			refreshControlsEnablement();
		} else {
			resetChanges();
		}
	}

	@Override
	public void revalidateContent() {
		validationManager.validateContent();
	}

}
