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

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
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
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.composite.BranchTagSelectionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.dialog.ReplaceWarningDialog;
import org.eclipse.team.svn.ui.panel.local.ReplaceBranchTagPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Replace with branch/tag action implementation
 * 
 * @author Alexei Goncharov
 */
public class ReplaceWithBranchTagAction extends AbstractWorkingCopyAction {
	protected int type;

	public ReplaceWithBranchTagAction(int type) {
		this.type = type;
	}

	@Override
	public boolean isEnabled() {
		return this.getSelectedResources().length == 1 && checkForResourcesPresence(IStateFilter.SF_ONREPOSITORY);
	}

	@Override
	public void runImpl(IAction action) {
		IResource[] resources = this.getSelectedResources(IStateFilter.SF_ONREPOSITORY);
		IActionOperation op = ReplaceWithBranchTagAction.getReplaceOperation(resources, getShell(), type);
		if (op != null) {
			runScheduled(op);
		}
	}

	public static IActionOperation getReplaceOperation(IResource[] resources, Shell shell, int type) {
		ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(resources[0]);
		IRepositoryResource remote = local.isCopied()
				? SVNUtility.getCopiedFrom(resources[0])
				: SVNRemoteStorage.instance().asRepositoryResource(resources[0]);

		boolean considerStructure = BranchTagSelectionComposite.considerStructure(remote);
		IRepositoryResource[] branchTagResources = considerStructure
				? BranchTagSelectionComposite.calculateBranchTagResources(remote, type)
				: new IRepositoryResource[0];
		if (!(considerStructure && branchTagResources.length == 0)) {
			ReplaceBranchTagPanel panel = new ReplaceBranchTagPanel(remote, local.getRevision(), type,
					branchTagResources);
			DefaultDialog dlg = new DefaultDialog(shell, panel);
			if (dlg.open() == 0) {
				ReplaceWarningDialog dialog = new ReplaceWarningDialog(shell);
				if (dialog.open() == 0 && panel.getResourceToReplaceWith() != null) {
					IRepositoryResource selected = panel.getResourceToReplaceWith();
					boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
							SVNTeamUIPlugin.instance().getPreferenceStore(),
							SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
					CompositeOperation op = new CompositeOperation("Operation_ReplaceWithRevision", //$NON-NLS-1$
							SVNUIMessages.class);
					SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
					op.add(saveOp);
					op.add(new ReplaceWithRemoteOperation(resources[0], selected, ignoreExternals));
					op.add(new RestoreProjectMetaOperation(saveOp));
					op.add(new RefreshResourcesOperation(resources));
					return op;
				}
			}
		}
		return null;
	}

}
