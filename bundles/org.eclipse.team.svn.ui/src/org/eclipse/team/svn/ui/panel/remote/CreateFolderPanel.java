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

package org.eclipse.team.svn.ui.panel.remote;

import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Create remote folder panel
 * 
 * @author Alexander Gurov
 */
public class CreateFolderPanel extends AbstractGetResourceNamePanel {

	public CreateFolderPanel() {
		super(SVNUIMessages.CreateFolderPanel_Title, true);
		dialogDescription = SVNUIMessages.CreateFolderPanel_Description;
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.remote_createFolderDialogContext"; //$NON-NLS-1$
	}

}
