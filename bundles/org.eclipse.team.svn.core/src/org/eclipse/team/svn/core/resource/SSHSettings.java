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

import org.eclipse.team.svn.core.resource.events.ISSHSettingsStateListener;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SSH settings implementation
 * 
 * @author Alexander Gurov
 */
public class SSHSettings implements Serializable {
	private static final long serialVersionUID = 300172856661110588L;

	public static final int SSH_PORT_DEFAULT = 22;

	protected int port;

	protected boolean useKeyFile;

	protected String privateKeyPath;

	// Base64 encoded
	protected String passPhrase;

	protected boolean passPhraseSaved;

	private transient String passPhraseTemporary;

	private transient ISSHSettingsStateListener parentLocation;

	public SSHSettings() {
		this(null);
	}

	public SSHSettings(ISSHSettingsStateListener parentLocation) {
		port = SSHSettings.SSH_PORT_DEFAULT;
		useKeyFile = false;
		privateKeyPath = ""; //$NON-NLS-1$
		passPhrase = ""; //$NON-NLS-1$
		passPhraseSaved = false;
		this.parentLocation = parentLocation;
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
		fireSSHChanged(ISSHSettingsStateListener.SSH_PASS_PHRASE, oldValue, passPhrase);
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		int oldValue = this.port;
		this.port = port;
		fireSSHChanged(ISSHSettingsStateListener.SSH_PORT, Integer.valueOf(oldValue), Integer.valueOf(port));
	}

	public String getPrivateKeyPath() {
		return privateKeyPath;
	}

	public void setPrivateKeyPath(String privateKeyPath) {
		String oldValue = this.privateKeyPath;
		this.privateKeyPath = privateKeyPath;
		fireSSHChanged(ISSHSettingsStateListener.SSH_PRIVATE_KEY_PATH, oldValue, privateKeyPath);
	}

	public boolean isUseKeyFile() {
		return useKeyFile;
	}

	public void setUseKeyFile(boolean useKeyFile) {
		boolean oldValue = this.useKeyFile;
		this.useKeyFile = useKeyFile;
		fireSSHChanged(ISSHSettingsStateListener.SSH_USE_KEY_FILE, Boolean.valueOf(oldValue),
				Boolean.valueOf(useKeyFile));
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
		fireSSHChanged(ISSHSettingsStateListener.SSH_PASS_PHRASE_SAVED, Boolean.valueOf(oldValue),
				Boolean.valueOf(passPhraseSaved));
	}

	protected void fireSSHChanged(String field, Object oldValue, Object newValue) {
		if (parentLocation != null) {
			parentLocation.sshChanged(null, field, oldValue, newValue);
		}
	}

}
