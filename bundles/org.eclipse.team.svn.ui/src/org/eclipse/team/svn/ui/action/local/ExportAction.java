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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.local.ExportOperation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Export working copy action implementation
 * 
 * @author Sergiy Logvin
 */

public class ExportAction extends AbstractWorkingCopyAction {

	public ExportAction() {
		super();
	}

	public void runImpl(IAction action) {
		IResource[] resources = this.getSelectedResources(IStateFilter.SF_EXCLUDE_DELETED);

		DirectoryDialog fileDialog = new DirectoryDialog(this.getShell());
		fileDialog.setText(SVNUIMessages.ExportAction_Select_Title);
		fileDialog.setMessage(SVNUIMessages.ExportAction_Select_Description);
		String path = fileDialog.open();
		if (path != null) {
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			this.runScheduled(new ExportOperation(resources, path, SVNRevision.WORKING, ignoreExternals));
		}
	}

	public boolean isEnabled() {
		return this.checkForResourcesPresence(IStateFilter.SF_EXCLUDE_DELETED);
	}

	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

}
