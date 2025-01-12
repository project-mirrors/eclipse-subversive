/*******************************************************************************
 * Copyright (c) 2005, 2025 Polarion Software and others.
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

package org.eclipse.team.svn.ui.discovery;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.svn.core.discovery.util.WebUtil;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.svnstorage.SVNCachedProxyCredentialsManager;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.svnstorage.SVNRepositoryLocation;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.discovery.wizards.ConnectorDiscoveryWizard;
import org.eclipse.team.svn.ui.panel.callback.PromptCredentialsPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Operation which checks whether connectors are installed and if they're not, it provides a wizard to installing them.
 * 
 * Users need to restart Eclipse in order to apply changes
 * 
 * @author Igor Burilo
 */
public class DiscoveryConnectorsHelper {

	protected static class ProxyAuthenticator extends Authenticator {

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			if (getRequestorType() == Authenticator.RequestorType.PROXY) {
				SVNCachedProxyCredentialsManager proxyCredentialsManager = SVNRemoteStorage.instance()
						.getProxyCredentialsManager();
				if (proxyCredentialsManager.getUsername() == null || proxyCredentialsManager.getUsername() == "") { //$NON-NLS-1$
					final boolean[] result = new boolean[1];
					UIMonitorUtility.getDisplay().syncExec(() -> {
						PromptCredentialsPanel panel = new PromptCredentialsPanel(getRequestingPrompt(),
								SVNRepositoryLocation.PROXY_CONNECTION);
						DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getShell(), panel);
						if (dialog.open() == 0) {
							result[0] = true;
						}
					});
					if (result[0]) {
						String pswd = proxyCredentialsManager.getPassword();
						return new PasswordAuthentication(proxyCredentialsManager.getUsername(),
								pswd == null ? "".toCharArray() : pswd.toCharArray()); //$NON-NLS-1$
					}
				} else {
					String pswd = proxyCredentialsManager.getPassword();
					return new PasswordAuthentication(proxyCredentialsManager.getUsername(),
							pswd == null ? "".toCharArray() : pswd.toCharArray()); //$NON-NLS-1$
				}
			}
			return null;
		}
	}

	public void run(IProgressMonitor monitor) throws Exception {
		//check that connectors exist
		if (CoreExtensionsManager.instance().getAccessibleClients().isEmpty()
				&& Platform.getBundle("org.eclipse.equinox.p2.repository") != null) { //$NON-NLS-1$
			try {
				IConnectorsInstallJob installJob = new PrepareInstallProfileJob_3_6();

				if (installJob != null) {
					//set proxy authenticator to WebUtil for accessing Internet files
					WebUtil.setAuthenticator(new ProxyAuthenticator());

					UIMonitorUtility.getDisplay().asyncExec(() -> {
						ConnectorDiscoveryWizard wizard = new ConnectorDiscoveryWizard(installJob);
						WizardDialog dialog = new WizardDialog(UIMonitorUtility.getShell(), wizard);
						dialog.open();
					});
				}
			} catch (Throwable e) {
				//make more user-friendly error message
				throw new UnreportableException(
						"Errors occured while initializing provisioning framework. This make cause discovery install to fail.", //$NON-NLS-1$
						e);
			}
		}
	}

}
