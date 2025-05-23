/*******************************************************************************
 * Copyright (c) 2008, 2023 Polarion Software and others.
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

package org.eclipse.team.svn.mylyn;

import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Mylyn integration entry point.
 * 
 * @author Alexander Gurov
 */
public class SVNMylynIntegrationPlugin extends AbstractUIPlugin {
	public static final String ID = "org.eclipse.team.svn.mylyn"; //$NON-NLS-1$

	private static SVNMylynIntegrationPlugin instance;

	public SVNMylynIntegrationPlugin() {
		SVNMylynIntegrationPlugin.instance = this;
	}

	public static SVNMylynIntegrationPlugin instance() {
		return SVNMylynIntegrationPlugin.instance;
	}

}
