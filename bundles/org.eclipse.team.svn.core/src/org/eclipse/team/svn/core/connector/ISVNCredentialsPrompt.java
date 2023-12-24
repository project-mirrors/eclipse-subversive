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

package org.eclipse.team.svn.core.connector;

import org.eclipse.team.svn.core.connector.ssl.SSLServerCertificateFailures;
import org.eclipse.team.svn.core.connector.ssl.SSLServerCertificateInfo;

/**
 * Interface that provide ability to ask user about repository credentials
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public interface ISVNCredentialsPrompt {
	ISVNCredentialsPrompt DEFAULT_PROMPT = new ISVNCredentialsPrompt() {
		@Override
		public boolean promptSSL(Object context, String realm) {
			return false;
		}

		@Override
		public boolean promptSSH(Object context, String realm) {
			return false;
		}

		@Override
		public boolean promptProxy(Object context) {
			return false;
		}

		@Override
		public boolean prompt(Object context, String realm) {
			return false;
		}

		@Override
		public boolean isSaveProxyPassword() {
			return false;
		}

		@Override
		public boolean isSaveCredentialsEnabled() {
			return false;
		}

		@Override
		public boolean isSSLSavePassphrase() {
			return false;
		}

		@Override
		public boolean isSSLAuthenticationEnabled() {
			return false;
		}

		@Override
		public boolean isSSHPublicKeySelected() {
			return false;
		}

		@Override
		public boolean isSSHPrivateKeyPassphraseSaved() {
			return false;
		}

		@Override
		public boolean isProxyEnabled() {
			return false;
		}

		@Override
		public boolean isProxyAuthenticationEnabled() {
			return false;
		}

		@Override
		public String getUsername() {
			return null;
		}

		@Override
		public String getSSLClientCertPath() {
			return null;
		}

		@Override
		public String getSSLClientCertPassword() {
			return null;
		}

		@Override
		public String getSSHPrivateKeyPath() {
			return null;
		}

		@Override
		public String getSSHPrivateKeyPassphrase() {
			return null;
		}

		@Override
		public int getSSHPort() {
			return -1;
		}

		@Override
		public String getRealmToSave() {
			return ISVNCredentialsPrompt.ROOT_LOCATION;
		}

		@Override
		public String getProxyUserName() {
			return null;
		}

		@Override
		public int getProxyPort() {
			return -1;
		}

		@Override
		public String getProxyPassword() {
			return null;
		}

		@Override
		public String getProxyHost() {
			return null;
		}

		@Override
		public String getPassword() {
			return null;
		}

		@Override
		public Answer askTrustSSLServer(Object context, SSLServerCertificateFailures failures,
				SSLServerCertificateInfo info, boolean allowPermanently) {
			return Answer.ACCEPT_TEMPORARY;
		}
	};

	public enum Answer {
		/**
		 * Reject the connection to the server.
		 */
		REJECT(0),
		/**
		 * Accept the connection to the server <i>once</i> per session.
		 */
		ACCEPT_TEMPORARY(1),
		/**
		 * Accept the connection to the server <i>permanently</i>.
		 */
		ACCEPT_PERMANENTLY(2);

		public final int id;

		Answer(int id) {
			this.id = id;
		}
	}

	String ROOT_LOCATION = "<Repository Location>"; //$NON-NLS-1$

	boolean prompt(Object context, String realm);

	boolean promptSSL(Object context, String realm);

	boolean promptSSH(Object context, String realm);

	boolean promptProxy(Object context);

	Answer askTrustSSLServer(Object context, SSLServerCertificateFailures failures, SSLServerCertificateInfo info,
			boolean allowPermanently);

	String getSSHPrivateKeyPath();

	String getSSHPrivateKeyPassphrase();

	boolean isSSHPrivateKeyPassphraseSaved();

	int getSSHPort();

	String getSSLClientCertPath();

	String getSSLClientCertPassword();

	String getUsername();

	String getPassword();

	boolean isSaveCredentialsEnabled();

	boolean isSSHPublicKeySelected();

	boolean isSSLAuthenticationEnabled();

	boolean isSSLSavePassphrase();

	boolean isProxyEnabled();

	boolean isProxyAuthenticationEnabled();

	String getProxyHost();

	int getProxyPort();

	String getProxyUserName();

	String getProxyPassword();

	boolean isSaveProxyPassword();

	String getRealmToSave();

}
