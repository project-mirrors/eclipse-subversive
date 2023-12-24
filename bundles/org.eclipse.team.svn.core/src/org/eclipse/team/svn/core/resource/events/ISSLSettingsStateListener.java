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
public interface ISSLSettingsStateListener {
	public final String SSL_CERTIFICATE_PATH = "sslCertificatePath";

	public final String SSL_PASS_PHRASE = "sslPassPhrase";

	public final String SSL_PASS_PHRASE_SAVED = "sslPassPhraseSaved";

	public final String SSL_AUTHENTICATION_ENABLED = "sslAuthenticationEnabled";

	public void sslChanged(IRepositoryLocation where, String field, Object oldValue, Object newValue);
}
