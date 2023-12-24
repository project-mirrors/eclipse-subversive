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

package org.eclipse.team.svn.core.resource;

import java.io.Serializable;

import org.eclipse.team.svn.core.resource.events.ISSLSettingsStateListener;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SSL settings implementation
 * 
 * @author Alexander Gurov
 */
public class SSLSettings implements Serializable {
	private static final long serialVersionUID = -5649960025841815445L;

	protected boolean authenticationEnabled;

	protected String certificatePath;

	protected boolean passPhraseSaved;

	// Base64 encoded
	protected String passPhrase;

	private transient String passPhraseTemporary;

	private transient ISSLSettingsStateListener parentLocation;

	public SSLSettings() {
		this(null);
	}

	public SSLSettings(ISSLSettingsStateListener parentLocation) {
		certificatePath = ""; //$NON-NLS-1$
		passPhrase = ""; //$NON-NLS-1$
		passPhraseSaved = false;
		this.parentLocation = parentLocation;
	}

	public String getCertificatePath() {
		return certificatePath;
	}

	public void setCertificatePath(String certificatePath) {
		String oldValue = this.certificatePath;
		this.certificatePath = certificatePath;
		fireSSLChanged(ISSLSettingsStateListener.SSL_CERTIFICATE_PATH, oldValue, certificatePath);
	}

	public String getPassPhrase() {
		return passPhraseSaved ? SVNUtility.base64Decode(passPhrase) : SVNUtility.base64Decode(passPhraseTemporary);
	}

	public void setPassPhrase(String passPhrase) {
		String oldValue = passPhraseSaved ? this.passPhrase : passPhraseTemporary;
		oldValue = oldValue != null ? SVNUtility.base64Decode(oldValue) : oldValue;
		if (passPhraseSaved) {
			this.passPhrase = SVNUtility.base64Encode(passPhrase);
		} else {
			passPhraseTemporary = SVNUtility.base64Encode(passPhrase);
		}
		fireSSLChanged(ISSLSettingsStateListener.SSL_PASS_PHRASE, oldValue, passPhrase);
	}

	public boolean isPassPhraseSaved() {
		return passPhraseSaved;
	}

	public void setPassPhraseSaved(boolean passPhraseSaved) {
		if (this.passPhraseSaved == passPhraseSaved) {
			return;
		}
		boolean oldValue = this.passPhraseSaved;
		this.passPhraseSaved = passPhraseSaved;
		if (!passPhraseSaved) {
			passPhraseTemporary = passPhrase;
			passPhrase = null;
		} else {
			passPhrase = passPhraseTemporary;
		}
		fireSSLChanged(ISSLSettingsStateListener.SSL_PASS_PHRASE_SAVED, Boolean.valueOf(oldValue),
				Boolean.valueOf(passPhraseSaved));
	}

	public boolean isAuthenticationEnabled() {
		return authenticationEnabled;
	}

	public void setAuthenticationEnabled(boolean authenticationEnabled) {
		boolean oldValue = this.authenticationEnabled;
		this.authenticationEnabled = authenticationEnabled;
		fireSSLChanged(ISSLSettingsStateListener.SSL_AUTHENTICATION_ENABLED, Boolean.valueOf(oldValue),
				Boolean.valueOf(authenticationEnabled));
	}

	protected void fireSSLChanged(String field, Object oldValue, Object newValue) {
		if (parentLocation != null) {
			parentLocation.sslChanged(null, field, oldValue, newValue);
		}
	}

}
