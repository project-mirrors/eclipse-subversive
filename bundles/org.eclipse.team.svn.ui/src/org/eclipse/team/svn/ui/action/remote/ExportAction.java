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

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.operation.remote.ExportOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.remote.ExportPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Export Action implementation
 * 
 * @author Sergiy Logvin
 */
public class ExportAction extends AbstractRepositoryTeamAction {

	public ExportAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		ExportPanel panel = new ExportPanel(resources.length > 1 ? null : resources[0]);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == 0) {
			if (resources.length == 1) {
				resources[0] = SVNUtility.copyOf(resources[0]);
				resources[0].setSelectedRevision(panel.getSelectedRevision());
			}
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			runScheduled(new ExportOperation(resources, panel.getLocation(), panel.getDepth(), ignoreExternals));
		}
	}

	@Override
	public boolean isEnabled() {
		return getSelectedRepositoryResources().length > 0;
	}

}
