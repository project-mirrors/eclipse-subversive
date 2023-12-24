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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.svnstorage;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

/**
 * Cached proxy credentials manager.
 * 
 * @author Alexei Goncharov
 */
public class SVNCachedProxyCredentialsManager {
	protected String username;

	protected String password;

	public SVNCachedProxyCredentialsManager(IProxyService proxyService) {
		IProxyData proxyData = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE);
		username = proxyData.isRequiresAuthentication() ? proxyData.getUserId() : ""; //$NON-NLS-1$
		password = proxyData.isRequiresAuthentication() ? proxyData.getPassword() : ""; //$NON-NLS-1$
	}

	public String getPassword() {
		return password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
