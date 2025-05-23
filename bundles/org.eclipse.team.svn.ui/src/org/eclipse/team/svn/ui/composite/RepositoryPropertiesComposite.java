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
 *    Alexander Gurov - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.operation.local.management.FindRelatedProjectsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.SSHSettings;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.common.RepositoryTreePanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbsolutePathVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.URLVerifier;

/**
 * Repository properties editor panel
 * 
 * @author Alexander Gurov
 */
public class RepositoryPropertiesComposite extends Composite implements IPropertiesPanel {
	protected static final String URL_HISTORY_NAME = "repositoryURL"; //$NON-NLS-1$

	protected Text repositoryLabel;

	protected Combo url;

	protected CompositeVerifier urlVerifier;

	protected UserInputHistory urlHistory;

	protected Button browse;

	protected Button useLocationButton;

	protected Button newLabelButton;

	protected CredentialsComposite credentialsComposite;

	protected IRepositoryLocation repositoryLocation;

	protected String rootUrl;

	protected IValidationManager validationManager;

	protected IRepositoryLocation credentialsInput;

	protected ISecurityInfoProvider provider;

	public RepositoryPropertiesComposite(Composite parent, int style, IValidationManager validationManager) {
		super(parent, style);

		this.validationManager = validationManager;
	}

	public String getPasswordDirect() {
		return credentialsComposite.getPassword().getText();
	}

	public void setPasswordDirect(String password) {
		credentialsComposite.getPassword().setText(password);
	}

	public String getUsernameDirect() {
		return credentialsComposite.getUsername().getText();
	}

	public void setUsernameDirect(String username) {
		credentialsComposite.getUsername().setText(username);
	}

	public boolean getPasswordSavedDirect() {
		return credentialsComposite.getSavePassword().getSelection();
	}

	public void setPasswordSavedDirect(boolean saved) {
		credentialsComposite.getSavePassword().setSelection(saved);
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

		Composite rootURLGroup = new Composite(this, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		rootURLGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		rootURLGroup.setLayoutData(data);

		Label description = new Label(rootURLGroup, SWT.NULL);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		description.setLayoutData(data);
		description.setText(SVNUIMessages.RepositoryPropertiesComposite_URL);

		urlHistory = new UserInputHistory(RepositoryPropertiesComposite.URL_HISTORY_NAME);

		url = new Combo(rootURLGroup, SWT.DROP_DOWN);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		url.setLayoutData(data);
		url.setVisibleItemCount(urlHistory.getDepth());
		url.setItems(urlHistory.getHistory());
		urlVerifier = new CompositeVerifier() {
			@Override
			public boolean verify(Control input) {
				boolean retVal = super.verify(input);
				browse.setEnabled(retVal);
				return retVal;
			}
		};
		defineUrlVerifier(null);
		validationManager.attachTo(url, urlVerifier);

		browse = new Button(rootURLGroup, SWT.PUSH);
		browse.setText(SVNUIMessages.Button_Browse);
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		data.widthHint = DefaultDialog.computeButtonWidth(browse);
		browse.setLayoutData(data);
		browse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNRemoteStorage storage = SVNRemoteStorage.instance();
				IRepositoryLocation location = storage.newRepositoryLocation();
				location.setUrl(RepositoryPropertiesComposite.this.getLocationUrl());
				location.setLabel(RepositoryPropertiesComposite.this.getLocationUrl());

				location.setPassword(provider.getPassword());
				location.setUsername(provider.getUsername());
				location.setPasswordSaved(provider.isPasswordSaved());

				SSHSettings sshNew = location.getSSHSettings();
				SSHSettings sshOriginal = provider.getSSHSettings();
				sshNew.setPassPhrase(sshOriginal.getPassPhrase());
				sshNew.setPassPhraseSaved(sshOriginal.isPassPhraseSaved());
				sshNew.setPort(sshOriginal.getPort());
				sshNew.setPrivateKeyPath(sshOriginal.getPrivateKeyPath());
				sshNew.setUseKeyFile(sshOriginal.isUseKeyFile());

				SSLSettings sslOriginal = location.getSSLSettings();
				SSLSettings sslNew = provider.getSSLSettings();
				sslNew.setAuthenticationEnabled(sslOriginal.isAuthenticationEnabled());
				sslNew.setCertificatePath(sslOriginal.getCertificatePath());
				sslNew.setPassPhrase(sslOriginal.getPassPhrase());
				sslNew.setPassPhraseSaved(sslOriginal.isPassPhraseSaved());

				RepositoryTreePanel panel = new RepositoryTreePanel(
						SVNUIMessages.RepositoryPropertiesComposite_SelectNewURL,
						SVNUIMessages.RepositoryBrowsingPanel_Description,
						SVNUIMessages.RepositoryBrowsingPanel_Message, null, true, location, false);
				panel.setAutoExpandFirstLevel(true);
				DefaultDialog browser = new DefaultDialog(RepositoryPropertiesComposite.this.getShell(), panel);
				if (browser.open() == 0) {
					if (panel.getSelectedResource() != null) {
						String newUrl = panel.getSelectedResource().getUrl();
						url.setText(newUrl);
					}
					provider.setUsername(location.getUsername());
					provider.setPassword(location.getPassword());
					provider.setPasswordSaved(location.isPasswordSaved());

					sshNew = provider.getSSHSettings();
					sshOriginal = location.getSSHSettings();
					sshNew.setPassPhrase(sshOriginal.getPassPhrase());
					sshNew.setPassPhraseSaved(sshOriginal.isPassPhraseSaved());
					sshNew.setPort(sshOriginal.getPort());
					sshNew.setPrivateKeyPath(sshOriginal.getPrivateKeyPath());
					sshNew.setUseKeyFile(sshOriginal.isUseKeyFile());

					sslOriginal = provider.getSSLSettings();
					sslNew = location.getSSLSettings();
					sslNew.setAuthenticationEnabled(sslOriginal.isAuthenticationEnabled());
					sslNew.setCertificatePath(sslOriginal.getCertificatePath());
					sslNew.setPassPhrase(sslOriginal.getPassPhrase());
					sslNew.setPassPhraseSaved(sslOriginal.isPassPhraseSaved());

					provider.commit();
				}
			}
		});

		Group labelGroup = new Group(this, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		data.horizontalSpan = 2;
		layout = new GridLayout();
		labelGroup.setLayout(layout);
		labelGroup.setLayoutData(data);
		labelGroup.setText(SVNUIMessages.RepositoryPropertiesComposite_Label);

		useLocationButton = new Button(labelGroup, SWT.RADIO);
		useLocationButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		useLocationButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validationManager.validateContent();
				Button button = (Button) e.widget;
				repositoryLabel.setEnabled(!button.getSelection());
				if (!button.getSelection()) {
					repositoryLabel.selectAll();
				} else {
					repositoryLabel.setText(""); //$NON-NLS-1$
				}
			}
		});
		useLocationButton.setText(SVNUIMessages.RepositoryPropertiesComposite_UseURL);

		newLabelButton = new Button(labelGroup, SWT.RADIO);
		newLabelButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		newLabelButton.setText(SVNUIMessages.RepositoryPropertiesComposite_UseCustom);

		repositoryLabel = new Text(labelGroup, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		repositoryLabel.setLayoutData(data);
		validationManager.attachTo(repositoryLabel, new AbstractVerifierProxy(
				new NonEmptyFieldVerifier(SVNUIMessages.RepositoryPropertiesComposite_UseCustom_Verifier)) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return newLabelButton.getSelection();
			}
		});
		repositoryLabel.setEnabled(false);

		credentialsComposite = new CredentialsComposite(this, SWT.NONE);
		credentialsComposite.initialize();

		url.setFocus();

		resetChanges();
	}

	public void setRepositoryLocation(IRepositoryLocation location, String rootUrl, ISecurityInfoProvider provider) {
		credentialsInput = repositoryLocation = location;
		this.rootUrl = rootUrl;
		this.provider = provider;
	}

	public IRepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	public String getLocationUrl() {
		return url.getText().trim();
	}

	public void setCredentialsInput(IRepositoryLocation location, ISecurityInfoProvider provider) {
		credentialsInput = location;
		this.provider = provider;
	}

	public void defineUrlVerifier(AbstractVerifier verifier) {
		String name = SVNUIMessages.RepositoryPropertiesComposite_URL_Verifier;
		urlVerifier.removeAll();
		urlVerifier.add(new URLVerifier(name));
		urlVerifier.add(new AbsolutePathVerifier(name));
		if (rootUrl != null && SVNRemoteStorage.instance().getRepositoryLocation(repositoryLocation.getId()) != null) {
			urlVerifier.add(new AbstractFormattedVerifier(name) {
				protected Boolean relatedProjects;

				@Override
				protected String getErrorMessageImpl(Control input) {
					return null;
				}

				@Override
				protected String getWarningMessageImpl(Control input) {
					if (relatedProjects == null) {
						FindRelatedProjectsOperation op = new FindRelatedProjectsOperation(
								repositoryLocation);
						UIMonitorUtility.doTaskBusyDefault(op);
						relatedProjects = op.getResources() == null || op.getResources().length == 0
								? Boolean.FALSE
								: Boolean.TRUE;
					}
					if (relatedProjects == Boolean.FALSE) {
						return null;
					}
					String newUrl = getText(input);
					newUrl = SVNUtility.normalizeURL(newUrl);
					try {
						newUrl = SVNUtility.decodeURL(newUrl);
					} catch (Exception ex) {
						// is not encoded URL
					}
					if (!SVNUtility.createPathForSVNUrl(rootUrl).isPrefixOf(SVNUtility.createPathForSVNUrl(newUrl))) {
						return SVNUIMessages.RepositoryPropertiesComposite_URL_Verifier_Warning;
					}
					return null;
				}
			});
		}

		if (verifier != null) {
			urlVerifier.add(verifier);
		}
	}

	@Override
	public void saveChanges() {
		if (useLocationButton.getSelection()) {
			repositoryLocation.setLabel(getLocationUrl());
		} else {
			repositoryLocation.setLabel(repositoryLabel.getText());
		}
		String newUrl = getLocationUrl();
		urlHistory.addLine(newUrl);
		repositoryLocation.setUrl(newUrl);

		credentialsComposite.getUserHistory().addLine(credentialsComposite.userName.getText());

		credentialsInput.setUsername(credentialsComposite.getUsername().getText());
		credentialsInput.setPassword(credentialsComposite.getPassword().getText());
		credentialsInput.setPasswordSaved(credentialsComposite.getSavePassword().getSelection());
	}

	@Override
	public void resetChanges() {
		String url = repositoryLocation.getUrlAsIs();
		url = url == null ? "" : url; //$NON-NLS-1$
		if (repositoryLocation.getLabel() == null
				|| repositoryLocation.getLabel().equalsIgnoreCase(repositoryLocation.getUrlAsIs())
				|| repositoryLocation.getLabel().equalsIgnoreCase(repositoryLocation.getUrl())) {
			repositoryLabel.setText(url);
			useLocationButton.setSelection(true);
			newLabelButton.setSelection(false);
		} else {
			repositoryLabel.setText(repositoryLocation.getLabel());
			useLocationButton.setSelection(false);
			newLabelButton.setSelection(true);
		}
		RepositoryPropertiesComposite.this.repositoryLabel.setEnabled(!useLocationButton.getSelection());
		this.url.setText(url);

		String username = credentialsInput.getUsername();
		credentialsComposite.getUsername().setText(username == null ? "" : username); //$NON-NLS-1$
		String password = credentialsInput.getPassword();
		credentialsComposite.getPassword().setText(password == null ? "" : password); //$NON-NLS-1$

		credentialsComposite.getSavePassword().setSelection(credentialsInput.isPasswordSaved());
	}

	@Override
	public void cancelChanges() {

	}

}
