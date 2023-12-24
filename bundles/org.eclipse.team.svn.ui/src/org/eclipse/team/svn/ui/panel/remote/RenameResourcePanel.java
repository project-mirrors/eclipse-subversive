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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Rename remote resource panel
 * 
 * @author Alexander Gurov
 */
public class RenameResourcePanel extends AbstractGetResourceNamePanel {
	public RenameResourcePanel(String originalName) {
		super(SVNUIMessages.RenameResourcePanel_Title, true);
		dialogDescription = SVNUIMessages.RenameResourcePanel_Description;
		disallowedName = originalName;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		super.createControlsImpl(parent);

		text.setText(disallowedName);
		text.selectAll();
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.renameDialogContext"; //$NON-NLS-1$
	}

}
