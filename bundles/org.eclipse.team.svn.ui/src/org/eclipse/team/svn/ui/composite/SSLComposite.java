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
 *    Markus Oberlassnig  (ilogs information logistics GmbH) - MSCAPI support via SVNKit
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import java.security.Provider;
import java.security.Security;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.SSLCertificateResourceVerifier;

/**
 * SSL Host properties editor panel
 *
 * @author Sergiy Logvin
 */
public class SSLComposite extends Composite implements IPropertiesPanel {
	protected boolean callback;

	protected Button enableAuthenticationCheckBox;

	protected Text certificateFileText;

	protected Button browseButton;

	protected Text certificatePassphraseText;

	protected Button savePassphraseCheckBox;

	protected Button mscapiButton;

	protected Button aliasButton;

	protected Text aliasText;

	protected boolean mscapiSupport;

	protected IValidationManager validationManager;

	protected SSLSettings credentialsInput;

	public SSLComposite(Composite parent, int style, IValidationManager validationManager) {
		this(parent, style, validationManager, false);
	}

	public SSLComposite(Composite parent, int style, IValidationManager validationManager, boolean callback) {
		super(parent, style);
		this.validationManager = validationManager;
		this.callback = callback;
		credentialsInput = new SSLSettings();
	}

	public SSLSettings getSSLSettingsDirect() {
		SSLSettings settings = new SSLSettings();
		getSSLSettingsDirectImpl(settings);
		return settings;
	}

	public void setSSLSettingsDirect(SSLSettings settings) {
		savePassphraseCheckBox.setSelection(settings.isPassPhraseSaved());
		enableAuthenticationCheckBox.setSelection(settings.isAuthenticationEnabled());
		String text = settings.getPassPhrase();
		certificatePassphraseText.setText(text == null ? "" : text); //$NON-NLS-1$
		text = settings.getCertificatePath();
		certificateFileText.setText(text == null ? "" : text); //$NON-NLS-1$

		if (callback) {
			if (text != null && text.length() > 0) {
				certificatePassphraseText.setFocus();
				certificatePassphraseText.selectAll();
			} else {
				certificateFileText.setFocus();
			}
		}

		refreshControlsEnablement();
	}

	@Override
	public void initialize() {
		GridLayout layout = null;
		GridData data = null;

		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 7;
		layout.verticalSpacing = 3;
		data = new GridData(GridData.FILL_BOTH);
		setLayout(layout);
		setLayoutData(data);

		enableAuthenticationCheckBox = new Button(this, SWT.CHECK);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		data.horizontalSpan = 2;
		enableAuthenticationCheckBox.setLayoutData(data);
		enableAuthenticationCheckBox.setText(SVNUIMessages.SSLComposite_EnableAuthentication);
		enableAuthenticationCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SSLComposite.this.refreshControlsEnablement();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		mscapiSupport = false;
		Provider pjacapi = Security.getProvider("CAPI"); //$NON-NLS-1$
		Provider pmscapi = Security.getProvider("SunMSCAPI"); //$NON-NLS-1$
		// Check that Java supports MSCAPI
		if (pmscapi != null) {
			try {
				ClassLoader.getSystemClassLoader().loadClass("sun.security.mscapi.NONEwithRSASignature");
			} catch (Exception e1) {
				pmscapi = null;
			}
		}
		// ms capi is only suported for windows and for provider SunMSCAPI and JACAPI from keyon
		// further ms capi is only supported from svnkit as client!
		// JAVAHL supports this feature, with version > 1.6.16 and windows 32 platforms
		String svnClientText = CoreExtensionsManager.instance().getSVNConnectorFactory().getId();
		if (FileUtility.isWindows() && (pjacapi != null || pmscapi != null) && svnClientText.contains("svnkit")) { //$NON-NLS-1$
			mscapiSupport = true;
		}
		if (mscapiSupport) {
			mscapiButton = new Button(this, SWT.CHECK);
			data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
			data.horizontalSpan = 2;
			mscapiButton.setLayoutData(data);
			mscapiButton.setText("Use MSCAPI");
			SelectionListener mscapiSelectionListener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (mscapiButton.getSelection()) {
						certificateFileText.setText("MSCAPI");
						certificateFileText.setEnabled(false);
						certificatePassphraseText.setEnabled(false);
						browseButton.setEnabled(false);
						aliasButton.setEnabled(true);
					} else {
						certificateFileText.setEnabled(true);
						certificatePassphraseText.setEnabled(true);
						browseButton.setEnabled(true);
						aliasButton.setEnabled(false);
						aliasText.setText(""); //$NON-NLS-1$
						certificateFileText.setText("");
					}
				}
			};
			mscapiButton.addSelectionListener(mscapiSelectionListener);
		}
		/*
		Group group1 = new Group(this, SWT.NONE);
		group1.setText("MSCAPI"+FileUtility.isWindows()+" - JACAPI:"+pjacapi + " - MSCAPI:"+ pmscapi + " - SVN Clien:" + svnClientText);
		layout = new GridLayout();
		layout.verticalSpacing = 12;
		group1.setLayout(layout);
		*/
		Group group = new Group(this, SWT.NONE);
		group.setText(SVNUIMessages.SSLComposite_ClientCertificate);
		layout = new GridLayout();
		layout.verticalSpacing = 12;
		group.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		group.setLayoutData(data);

		Composite fileAndPassphrase = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 2;
		fileAndPassphrase.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fileAndPassphrase.setLayoutData(data);

		Label description = new Label(fileAndPassphrase, SWT.NULL);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		description.setLayoutData(data);
		description.setText(SVNUIMessages.SSLComposite_File);

		Composite inner = new Composite(fileAndPassphrase, SWT.FILL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		inner.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		inner.setLayoutData(data);

		certificateFileText = new Text(inner, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		certificateFileText.setLayoutData(data);
		validationManager.attachTo(certificateFileText, new AbstractVerifierProxy(
				new SSLCertificateResourceVerifier(SVNUIMessages.SSLComposite_File_Verifier, true)) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return enableAuthenticationCheckBox.getSelection();
			}
		});
		browseButton = new Button(inner, SWT.PUSH);
		browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
		browseButton.setLayoutData(data);
		browseButton.addListener(SWT.Selection, event -> {
			FileDialog fileDialog = new FileDialog(SSLComposite.this.getShell(), SWT.OPEN);
			String res = fileDialog.open();
			if (res != null) {
				certificateFileText.setText(res);
				validationManager.validateContent();
			}
		});

		description = new Label(fileAndPassphrase, SWT.NULL);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		description.setLayoutData(data);
		description.setText(SVNUIMessages.SSLComposite_Passphrase);

		certificatePassphraseText = new Text(fileAndPassphrase, SWT.BORDER | SWT.PASSWORD);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		certificatePassphraseText.setLayoutData(data);

		if (mscapiSupport) {
			description = new Label(fileAndPassphrase, SWT.NULL);
			data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
			description.setLayoutData(data);
			description.setText("Certificate Alias:"); //$NON-NLS-1$

			inner = new Composite(fileAndPassphrase, SWT.FILL);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = layout.marginWidth = 0;
			inner.setLayout(layout);
			data = new GridData(GridData.FILL_HORIZONTAL);
			inner.setLayoutData(data);

			aliasText = new Text(inner, SWT.BORDER);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
			aliasText.setLayoutData(data);
			aliasText.setEnabled(false);

			aliasButton = new Button(inner, SWT.PUSH);
			aliasButton.setText("Select alias"); //$NON-NLS-1$
			data = new GridData(GridData.HORIZONTAL_ALIGN_END);
			data.widthHint = DefaultDialog.computeButtonWidth(aliasButton);
			aliasButton.setLayoutData(data);
			SelectionListener msCapiCertificateSelectionListener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					SSLClientCertificatesMSCapi dialog = new SSLClientCertificatesMSCapi(
							Display.getCurrent().getActiveShell(), "Certifacte selection");
					if (dialog.open() == Window.OK) {
						aliasText.setText(dialog.getAlias());
						certificateFileText.setText("MSCAPI;" + dialog.getAlias());
					}
				}
			};
			aliasButton.addSelectionListener(msCapiCertificateSelectionListener);
			aliasButton.setEnabled(false);
		}

		inner = new Composite(group, SWT.FILL);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		inner.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		inner.setLayoutData(data);

		savePassphraseCheckBox = new Button(inner, SWT.CHECK);
		savePassphraseCheckBox.setText(SVNUIMessages.SSLComposite_SavePassphrase);

		new SecurityWarningComposite(inner);
	}

	public void setCredentialsInput(SSLSettings input) {
		credentialsInput = input;
	}

	@Override
	public void saveChanges() {
		getSSLSettingsDirectImpl(credentialsInput);
	}

	@Override
	public void resetChanges() {
		setSSLSettingsDirect(credentialsInput);
	}

	@Override
	public void cancelChanges() {

	}

	protected void getSSLSettingsDirectImpl(SSLSettings settings) {
		settings.setAuthenticationEnabled(enableAuthenticationCheckBox.getSelection());
		settings.setCertificatePath(certificateFileText.getText());
		settings.setPassPhrase(certificatePassphraseText.getText());
		settings.setPassPhraseSaved(savePassphraseCheckBox.getSelection());
	}

	protected void refreshControlsEnablement() {
		boolean enabled = enableAuthenticationCheckBox.getSelection();
		if (enabled && mscapiSupport && certificateFileText != null && certificateFileText.getText() != null
				&& certificateFileText.getText().startsWith("MSCAPI")) {
			certificateFileText.setEnabled(false);
			certificatePassphraseText.setEnabled(false);
			browseButton.setEnabled(false);
			savePassphraseCheckBox.setEnabled(false);
			aliasButton.setEnabled(true);
			mscapiButton.setEnabled(true);
			mscapiButton.setSelection(true);
			String[] certAlias = certificateFileText.getText().split(";");
			if (certAlias.length > 1) {
				aliasText.setText(certAlias[1]);
			}
		} else {
			certificateFileText.setEnabled(enabled);
			browseButton.setEnabled(enabled);
			certificatePassphraseText.setEnabled(enabled);
			savePassphraseCheckBox.setEnabled(enabled);
			if (mscapiSupport) {
				mscapiButton.setEnabled(enabled);
			}
		}
		validationManager.validateContent();
	}

}
