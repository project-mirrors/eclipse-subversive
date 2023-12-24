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

package org.eclipse.team.svn.core.resource.events;

import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * Repository location change listener
 * 
 * @author Alexander Gurov
 */
public interface ISSHSettingsStateListener {
	String SSH_PASS_PHRASE = "sshPassPhrase";

	String SSH_PORT = "sshPort";

	String SSH_PRIVATE_KEY_PATH = "sshPrivateKeyPath";

	String SSH_USE_KEY_FILE = "sshUseKeyFile";

	String SSH_PASS_PHRASE_SAVED = "sshPassPhraseSaved";

	void sshChanged(IRepositoryLocation where, String field, Object oldValue, Object newValue);
}
