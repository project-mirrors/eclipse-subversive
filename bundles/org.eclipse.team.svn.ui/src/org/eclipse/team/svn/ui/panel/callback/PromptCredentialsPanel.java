/*******************************************************************************
 * Copyright (c) 2005, 2024 Polarion Software and others.
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
 *    Nikifor Fedorov (ArSysOp) - issue subversive/#245
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.callback;

import java.net.MalformedURLException;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.ISVNCredentialsPrompt;
import org.eclipse.team.svn.core.connector.ssl.SSLServerCertificateFailures;
import org.eclipse.team.svn.core.connector.ssl.SSLServerCertificateInfo;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.SSHSettings;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.svnstorage.SVNCachedProxyCredentialsManager;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.svnstorage.SVNRepositoryLocation;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.internal.ui.TabFolderLayout;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CredentialsComposite;
import org.eclipse.team.svn.ui.composite.ProxyComposite;
import org.eclipse.team.svn.ui.composite.SSHComposite;
import org.eclipse.team.svn.ui.composite.SSLComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Prompt user credentials panel
 * 
 * @author Alexander Gurov
 */
public class PromptCredentialsPanel extends AbstractDialogPanel {
	public static ISVNCredentialsPrompt DEFAULT_PROMPT = new DefaultPrompt();

	protected String selectedRealm;

	protected CredentialsComposite credentialsComposite;

	protected String username;

	protected String password;

	protected String host;

	protected boolean savePassword;

	protected SSHComposite compositeSSH;

	protected SSLComposite compositeSSL;

	protected ProxyComposite proxyComposite;

	protected SSHSettings sshSettings;

	protected SSLSettings sslSettings;

	protected String rootLocationName;

	protected int connectionType;

	public PromptCredentialsPanel() {
		this("", SVNRepositoryLocation.DEFAULT_CONNECTION); //$NON-NLS-1$
	}

	public PromptCredentialsPanel(String forWhat, int connectionType) {
		dialogTitle = SVNUIMessages.PromptCredentialsPanel_Title;
		dialogDescription = SVNUIMessages.PromptCredentialsPanel_Description;
		rootLocationName = SVNUIMessages.PromptCredentialsPanel_LocationRealm;
		defaultMessage = forWhat;
		host = SVNTeamPlugin.instance()
				.getProxyService()
				.getProxyData(
						forWhat.split(":")[0].equals("https") //$NON-NLS-1$//$NON-NLS-2$
								? IProxyData.HTTPS_PROXY_TYPE
								: IProxyData.HTTP_PROXY_TYPE)
				.getHost();
		this.connectionType = connectionType;
		selectedRealm = this.connectionType != SVNRepositoryLocation.PROXY_CONNECTION ? forWhat : rootLocationName;

		sslSettings = new SSLSettings();
		sslSettings.setAuthenticationEnabled(true);
		sshSettings = new SSHSettings();
		sshSettings.setUseKeyFile(true);
		savePassword = false;
		username = null;
		password = null;
	}

	public String getRealmToSave() {
		return selectedRealm == rootLocationName ? ISVNCredentialsPrompt.ROOT_LOCATION : selectedRealm;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void setSavePasswordEnabled(boolean savePassword) {
		this.savePassword = savePassword;
	}

	public boolean isSavePasswordEnabled() {
		return savePassword;
	}

	public String getSSHPassphrase() {
		return sshSettings.getPassPhrase();
	}

	public void setSSHPassphrase(String passphrase) {
		sshSettings.setPassPhrase(passphrase);
	}

	public boolean isSSHPassphraseSaved() {
		return sshSettings.isPassPhraseSaved();
	}

	public void setSSHPassphraseSaved(boolean passphraseSaved) {
		sshSettings.setPassPhraseSaved(passphraseSaved);
	}

	public String getSSHPrivateKeyFile() {
		return sshSettings.getPrivateKeyPath();
	}

	public void setSSHPrivateKeyFile(String privateKeyFile) {
		sshSettings.setPrivateKeyPath(privateKeyFile);
	}

	public int getSSHPort() {
		return sshSettings.getPort();
	}

	public void setSSHPort(int sshPort) {
		sshSettings.setPort(sshPort);
	}

	public boolean isSSHPublicKeySelected() {
		return sshSettings.isUseKeyFile();
	}

	public void setSSHPublicKeySelected(boolean publicKeySelected) {
		sshSettings.setUseKeyFile(publicKeySelected);
	}

	public boolean isSSLAuthenticationEnabled() {
		return sslSettings.isAuthenticationEnabled();
	}

	public void setSSLAuthenticationEnabled(boolean sslAuthenticationEnabled) {
		sslSettings.setAuthenticationEnabled(sslAuthenticationEnabled);
	}

	public String getSSLCertificatePath() {
		return sslSettings.getCertificatePath();
	}

	public void setSSLCertificatePath(String sslCertificatePath) {
		sslSettings.setCertificatePath(sslCertificatePath);
	}

	public String getSSLPassphrase() {
		return sslSettings.getPassPhrase();
	}

	public void setSSLPassphrase(String sslPassphrase) {
		sslSettings.setPassPhrase(sslPassphrase);
	}

	public boolean isSSLPassphraseSaved() {
		return sslSettings.isPassPhraseSaved();
	}

	public void setSSLPassphraseSaved(boolean sslPassphraseSaved) {
		sslSettings.setPassPhraseSaved(sslPassphraseSaved);
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayout(new TabFolderLayout());
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite composite = new Composite(tabFolder, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 3;
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		composite.setVisible(connectionType != SVNRepositoryLocation.PROXY_CONNECTION
				&& connectionType != SVNRepositoryLocation.SSL_CONNECTION);

		createGeneral(composite);

		if (connectionType != SVNRepositoryLocation.PROXY_CONNECTION) {
			if (connectionType != SVNRepositoryLocation.SSL_CONNECTION) {
				TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
				tabItem.setText(SVNUIMessages.PromptCredentialsPanel_Tab_General);
				tabItem.setControl(composite);

				if (connectionType == SVNRepositoryLocation.SSH_CONNECTION) {
					tabItem = new TabItem(tabFolder, SWT.NONE);
					tabItem.setText(SVNUIMessages.PromptCredentialsPanel_Tab_SSHSettings);
					compositeSSH = new SSHComposite(tabFolder, SWT.NONE, this, true);
					compositeSSH.setCredentialsInput(sshSettings);
					compositeSSH.initialize();
					tabItem.setControl(compositeSSH);
					if (isSSHPublicKeySelected()) {
						tabFolder.setSelection(new TabItem[] { tabItem });
					}
				}
			} else {
				TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
				tabItem.setText(SVNUIMessages.PromptCredentialsPanel_Tab_SSLSettings);
				compositeSSL = new SSLComposite(tabFolder, SWT.NONE, this, true);
				compositeSSL.setCredentialsInput(sslSettings);
				compositeSSL.initialize();
				tabItem.setControl(compositeSSL);
			}
		} else {
			TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
			tabItem.setText(SVNUIMessages.PromptCredentialsPanel_Tab_ProxySettings);
			proxyComposite = new ProxyComposite(tabFolder, SWT.NONE, this, true);
			SVNCachedProxyCredentialsManager proxyCredentialsManager = SVNRemoteStorage.instance()
					.getProxyCredentialsManager();
			proxyComposite.setUsername(proxyCredentialsManager.getUsername());
			proxyComposite.setPassword(proxyCredentialsManager.getPassword());
			proxyComposite.setHost(host);
			proxyComposite.initialize();
			tabItem.setControl(proxyComposite);
		}

		if (connectionType != SVNRepositoryLocation.PROXY_CONNECTION) {
			composite = new Composite(parent, SWT.NONE);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 1;
			layout.numColumns = 2;
			composite.setLayout(layout);
			data = new GridData(GridData.FILL_HORIZONTAL);
			composite.setLayoutData(data);

			Label label = new Label(composite, SWT.NONE);
			data = new GridData();
			label.setLayoutData(data);
			label.setText(SVNUIMessages.PromptCredentialsPanel_ApplyTo);

			final Combo combo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
			data = new GridData(GridData.FILL_HORIZONTAL);
			combo.setLayoutData(data);
			combo.setItems(selectedRealm, rootLocationName);
			combo.select(0);
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					selectedRealm = combo.getItem(combo.getSelectionIndex());
				}
			});
		}
	}

	@Override
	public void postInit() {
		if (connectionType == SVNRepositoryLocation.SSH_CONNECTION) {
			compositeSSH.resetChanges();
		}
		if (connectionType == SVNRepositoryLocation.SSL_CONNECTION) {
			compositeSSL.resetChanges();
		}
		super.postInit();
	}

	protected void createGeneral(Composite parent) {
		credentialsComposite = new CredentialsComposite(parent, SWT.NONE);

		credentialsComposite.setUsernameInput(username);
		credentialsComposite.setPasswordInput(password);
		credentialsComposite.setPasswordSaved(savePassword);

		credentialsComposite.initialize();
	}

	@Override
	protected void saveChangesImpl() {
		if (connectionType != SVNRepositoryLocation.PROXY_CONNECTION) {
			username = credentialsComposite.getUsername().getText().trim();
			password = credentialsComposite.getPassword().getText().trim();
			savePassword = credentialsComposite.getSavePassword().getSelection();

			if (connectionType == SVNRepositoryLocation.SSH_CONNECTION) {
				compositeSSH.saveChanges();
			}

			if (connectionType == SVNRepositoryLocation.SSL_CONNECTION) {
				compositeSSL.saveChanges();
			}
		} else {
			proxyComposite.saveChanges();
			SVNCachedProxyCredentialsManager proxyCredentialsManager = SVNRemoteStorage.instance()
					.getProxyCredentialsManager();
			proxyCredentialsManager.setUsername(proxyComposite.getUsername());
			proxyCredentialsManager.setPassword(proxyComposite.getPassword());
		}
	}

	@Override
	protected void cancelChangesImpl() {
	}

	protected static class DefaultPrompt implements ISVNCredentialsPrompt {
		protected String realmToSave;

		protected String username;

		protected String password;

		protected boolean saveCredentials;

		protected boolean sslAuthenticationEnabled;

		protected boolean sslSavePassphrase;

		protected String sslClientCertPassword;

		protected String sslClientCertPath;

		protected boolean sshPublicKeySelected;

		protected String sshPrivateKeyPath;

		protected String sshPrivateKeyPassphrase;

		protected boolean sshPrivateKeyPassphraseSaved;

		protected int sshPort;

		protected boolean proxyEnabled;

		protected String proxyHost;

		protected int proxyPort;

		protected boolean proxyAuthenticationEnabled;

		protected String proxyUsername;

		protected String proxyPassword;

		protected boolean proxySavePassword;

		@Override
		public String getRealmToSave() {
			return realmToSave;
		}

		@Override
		public boolean prompt(Object context, String realm) {
			return showPanel((IRepositoryLocation) context, SVNRepositoryLocation.DEFAULT_CONNECTION, realm);
		}

		@Override
		public boolean promptSSL(Object context, String realm) {
			return showPanel((IRepositoryLocation) context, SVNRepositoryLocation.SSL_CONNECTION, realm);
		}

		@Override
		public boolean promptSSH(Object context, String realm) {
			return showPanel((IRepositoryLocation) context, SVNRepositoryLocation.SSH_CONNECTION, realm);
		}

		@Override
		public boolean promptProxy(Object context) {
			return showPanel((IRepositoryLocation) context, SVNRepositoryLocation.PROXY_CONNECTION,
					((IRepositoryLocation) context).getUrlAsIs());
		}

		@Override
		public Answer askTrustSSLServer(final Object context, final SSLServerCertificateFailures failures,
				final SSLServerCertificateInfo info, final boolean allowPermanently) {
			final int[] retVal = new int[1];
			UIMonitorUtility.getDisplay().syncExec(() -> {
				AskTrustSSLServerPanel panel = new AskTrustSSLServerPanel(
						((IRepositoryLocation) context).getUrlAsIs(), failures, info, allowPermanently);
				DefaultDialog dlg = new DefaultDialog(UIMonitorUtility.getShell(), panel);
				retVal[0] = dlg.open();
			});
			return retVal[0] == 0
					? ISVNCredentialsPrompt.Answer.ACCEPT_TEMPORARY
					: retVal[0] == 2
							? ISVNCredentialsPrompt.Answer.REJECT
							: retVal[0] == 1
									? allowPermanently
											? ISVNCredentialsPrompt.Answer.ACCEPT_PERMANENTLY
											: ISVNCredentialsPrompt.Answer.REJECT
									: ISVNCredentialsPrompt.Answer.REJECT;
		}

		@Override
		public String getUsername() {
			return username;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public boolean isSaveCredentialsEnabled() {
			return saveCredentials;
		}

		@Override
		public boolean isSSLAuthenticationEnabled() {
			return sslAuthenticationEnabled;
		}

		@Override
		public boolean isSSLSavePassphrase() {
			return sslSavePassphrase;
		}

		@Override
		public String getSSLClientCertPath() {
			return sslClientCertPath;
		}

		@Override
		public String getSSLClientCertPassword() {
			return sslClientCertPassword;
		}

		@Override
		public String getSSHPrivateKeyPath() {
			return sshPublicKeySelected ? sshPrivateKeyPath : null;
		}

		@Override
		public String getSSHPrivateKeyPassphrase() {
			return sshPublicKeySelected ? sshPrivateKeyPassphrase : null;
		}

		@Override
		public int getSSHPort() {
			return sshPort;
		}

		@Override
		public boolean isSSHPublicKeySelected() {
			return sshPublicKeySelected;
		}

		@Override
		public boolean isSSHPrivateKeyPassphraseSaved() {
			return sshPrivateKeyPassphraseSaved;
		}

		@Override
		public String getProxyHost() {
			return proxyHost;
		}

		@Override
		public int getProxyPort() {
			return proxyPort;
		}

		@Override
		public String getProxyUserName() {
			return proxyUsername;
		}

		@Override
		public String getProxyPassword() {
			return proxyPassword;
		}

		@Override
		public boolean isProxyEnabled() {
			return proxyEnabled;
		}

		@Override
		public boolean isProxyAuthenticationEnabled() {
			return proxyAuthenticationEnabled;
		}

		@Override
		public boolean isSaveProxyPassword() {
			return proxySavePassword;
		}

		protected boolean showPanel(IRepositoryLocation inputLocation, final int connectionType, final String realm) {
			final IRepositoryLocation location = inputLocation.getLocationForRealm(realm) != null
					? inputLocation.getLocationForRealm(realm)
							: inputLocation;
			final int[] retVal = new int[1];
			final SSLSettings settings = location.getSSLSettings();

			UIMonitorUtility.getDisplay().syncExec(() -> {
				PromptCredentialsPanel panel = new PromptCredentialsPanel(realm, connectionType);
				DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getShell(), panel);
				if (connectionType != SVNRepositoryLocation.PROXY_CONNECTION) {
					panel.setUsername(location.getUsername());
					panel.setPassword(location.getPassword());
					panel.setSavePasswordEnabled(location.isPasswordSaved());
					if (connectionType == SVNRepositoryLocation.SSH_CONNECTION) {
						SSHSettings settings1 = location.getSSHSettings();
						panel.setSSHPublicKeySelected(settings1.isUseKeyFile());
						panel.setSSHPrivateKeyFile(settings1.getPrivateKeyPath());
						panel.setSSHPassphrase(settings1.getPassPhrase());
						panel.setSSHPassphraseSaved(settings1.isPassPhraseSaved());
						panel.setSSHPort(settings1.getPort());
					} else if (connectionType == SVNRepositoryLocation.SSL_CONNECTION) {
						panel.setSSLAuthenticationEnabled(settings.isAuthenticationEnabled());
						panel.setSSLCertificatePath(settings.getCertificatePath());
						panel.setSSLPassphrase(settings.getPassPhrase());
						panel.setSSLPassphraseSaved(settings.isPassPhraseSaved());
					}
				}
				if ((retVal[0] = dialog.open()) == 0) {
					realmToSave = panel.getRealmToSave();
					if (connectionType != SVNRepositoryLocation.PROXY_CONNECTION) {
						username = panel.getUsername();
						password = panel.getPassword();
						saveCredentials = panel.isSavePasswordEnabled();

						if (connectionType == SVNRepositoryLocation.SSH_CONNECTION) {
							sshPublicKeySelected = panel.isSSHPublicKeySelected();
							sshPrivateKeyPath = panel.getSSHPrivateKeyFile();
							sshPrivateKeyPassphrase = panel.getSSHPassphrase();
							sshPort = panel.getSSHPort();
							sshPrivateKeyPassphraseSaved = panel.isSSHPassphraseSaved();
						}
						if (connectionType == SVNRepositoryLocation.SSL_CONNECTION) {
							sslAuthenticationEnabled = panel.isSSLAuthenticationEnabled();
							sslClientCertPath = panel.getSSLCertificatePath();
							sslClientCertPassword = panel.getSSLPassphrase();
							sslSavePassphrase = panel.isSSLPassphraseSaved();
						}
					} else {
						IProxyService proxyService = SVNTeamPlugin.instance().getProxyService();
						String proxyType;
						SVNCachedProxyCredentialsManager proxyCredentialsManager = SVNRemoteStorage.instance()
								.getProxyCredentialsManager();
						String protocol = "http"; //$NON-NLS-1$
						try {
							protocol = SVNUtility.getSVNUrl(location.getUrlAsIs()).getProtocol();
						} catch (MalformedURLException ex) {
							//ignore
						}
						if (protocol != null && protocol.equals("https")) { //$NON-NLS-1$
							proxyType = IProxyData.HTTPS_PROXY_TYPE;
						} else {
							proxyType = IProxyData.HTTP_PROXY_TYPE;
						}
						IProxyData proxyData = proxyService.getProxyData(proxyType);
						proxyHost = proxyData.getHost();
						proxyPort = proxyData.getPort();
						proxyAuthenticationEnabled = proxyData.isRequiresAuthentication();
						proxyUsername = proxyCredentialsManager.getUsername();
						proxyPassword = proxyCredentialsManager.getPassword();
						proxySavePassword = true;
					}
				} else {
					username = null;
					password = null;
					saveCredentials = false;
				}
			});
			return retVal[0] == 0;
		}

	}

}
