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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.remote.ReplaceWithRemoteOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractNonRecursiveTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.dialog.ReplaceWarningDialog;
import org.eclipse.team.svn.ui.panel.local.ReplaceWithUrlPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Team services menu "replace with revision" action implementation
 *
 * @author Sergiy Logvin
 */
public class ReplaceWithRevisionAction extends AbstractNonRecursiveTeamAction {
	public ReplaceWithRevisionAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource[] resources = this.getSelectedResources(IStateFilter.SF_ONREPOSITORY);
		IActionOperation op = ReplaceWithRevisionAction.getReplaceOperation(resources, getShell());
		if (op != null) {
			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		return this.getSelectedResources().length == 1 && checkForResourcesPresence(IStateFilter.SF_ONREPOSITORY);
	}

	public static IActionOperation getReplaceOperation(IResource[] resources, Shell shell) {
		IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(resources[0]);
		ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(resources[0]);

		ReplaceWithUrlPanel panel = new ReplaceWithUrlPanel(remote, local.getRevision());
		DefaultDialog selectionDialog = new DefaultDialog(shell, panel);

		if (selectionDialog.open() == Window.OK) {
			ReplaceWarningDialog dialog = new ReplaceWarningDialog(shell);
			if (dialog.open() == 0) {
				IRepositoryResource selected = panel.getSelectedResource();
				boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
						SVNTeamUIPlugin.instance().getPreferenceStore(),
						SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
				CompositeOperation op = new CompositeOperation("Operation_ReplaceWithRevision", SVNUIMessages.class); //$NON-NLS-1$
				SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
				op.add(saveOp);
				op.add(new ReplaceWithRemoteOperation(resources[0], selected, ignoreExternals));
				op.add(new RestoreProjectMetaOperation(saveOp));
				op.add(new RefreshResourcesOperation(resources));
				return op;
			}
		}
		return null;
	}

}
