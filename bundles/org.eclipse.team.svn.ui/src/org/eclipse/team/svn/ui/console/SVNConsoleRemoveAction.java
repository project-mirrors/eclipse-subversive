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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.console;

import org.eclipse.jface.action.Action;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Action that removes the SVN console from the console view. The console can be re-added via the console view "Open Console" drop-down.
 * 
 * @author Igor Burilo
 */
public class SVNConsoleRemoveAction extends Action {

	public SVNConsoleRemoveAction() {
		super(SVNUIMessages.SVNConsoleRemoveAction_CloseConsole);
		setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/remove.gif")); //$NON-NLS-1$
	}

	@Override
	public void run() {
		SVNConsoleFactory.destroyConsole();
	}

}
