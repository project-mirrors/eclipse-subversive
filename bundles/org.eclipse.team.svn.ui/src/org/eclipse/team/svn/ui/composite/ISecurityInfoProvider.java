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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import org.eclipse.team.svn.core.resource.SSHSettings;
import org.eclipse.team.svn.core.resource.SSLSettings;

/**
 * This interface allows to transfer security information between repository location editor tabs
 * 
 * @author Alexander Gurov
 */
public interface ISecurityInfoProvider {
	String getUsername();

	void setUsername(String username);

	String getPassword();

	void setPassword(String password);

	boolean isPasswordSaved();

	void setPasswordSaved(boolean saved);

	SSLSettings getSSLSettings();

	SSHSettings getSSHSettings();

	void commit();
}
