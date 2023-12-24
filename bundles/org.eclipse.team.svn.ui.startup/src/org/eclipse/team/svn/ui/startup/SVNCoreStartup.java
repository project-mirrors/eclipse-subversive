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

package org.eclipse.team.svn.ui.startup;

import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.ui.IStartup;

/**
 * Initiates plug-in startup if it is loaded in the IDE scope (details in bug #336689).
 * 
 * @author Alexander Gurov
 */
public class SVNCoreStartup implements IStartup {

	public void earlyStartup() {
		// touch the core plug-in to trigger its load
		SVNTeamPlugin.instance();
	}

}
