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

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.UpdateOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractRecursiveTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.ClearUpdateStatusesOperation;
import org.eclipse.team.svn.ui.operation.NotifyUnresolvedConflictOperation;
import org.eclipse.team.svn.ui.panel.local.ResourceListPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UnacceptableOperationNotificator;

/**
 * Team services menu update action implementation
 * 
 * @author Alexander Gurov
 */
public class UpdateAction extends AbstractRecursiveTeamAction {

	public static IStateFilter SF_MISSING_RESOURCES = new IStateFilter.AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			//we shouldn't take into account resources with tree conflicts here
			return IStateFilter.SF_MISSING.accept(resource, state, mask)
					&& !IStateFilter.SF_TREE_CONFLICTING.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	public UpdateAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource[] resources = UnacceptableOperationNotificator.shrinkResourcesWithNotOnRespositoryParents(
				getShell(), this.getSelectedResources(IStateFilter.SF_ONREPOSITORY));
		if (resources == null || resources.length == 0) {
			return;
		}

		if (checkForResourcesPresenceRecursive(IStateFilter.SF_REVERTABLE)) {
			IResource[] missing = this.getSelectedResourcesRecursive(UpdateAction.SF_MISSING_RESOURCES);
			if (missing.length > 0 && !UpdateAction.updateMissing(getShell(), missing)) {
				return;
			}
		}

		runScheduled(UpdateAction.getUpdateOperation(resources, SVNRevision.HEAD));
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresence(IStateFilter.SF_ONREPOSITORY);
	}

	public static boolean updateMissing(Shell shell, IResource[] missing) {
		ResourceListPanel panel = new ResourceListPanel(missing, SVNUIMessages.UpdateAction_List_Title,
				SVNUIMessages.UpdateAction_List_Description, SVNUIMessages.UpdateAction_List_Message,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL });
		panel.setShowLocalNames(true);
		return new DefaultDialog(shell, panel).open() == 0;
	}

	public static CompositeOperation getUpdateOperation(IResource[] updateSet, SVNRevision selectedRevision) {
		return UpdateAction.getUpdateOperation(updateSet, selectedRevision, SVNDepth.INFINITY, false, null);
	}

	public static CompositeOperation getUpdateOperation(IResource[] updateSet, SVNRevision selectedRevision,
			SVNDepth depth, boolean isStickyDepth, String updateDepthPath) {
		boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
				SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
		UpdateOperation mainOp = new UpdateOperation(updateSet, selectedRevision, ignoreExternals);
		mainOp.setDepthOptions(depth, isStickyDepth, updateDepthPath);

		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

		SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(updateSet);
		op.add(saveOp);
		op.add(mainOp);
		op.add(new RestoreProjectMetaOperation(saveOp));
		op.add(new ClearUpdateStatusesOperation(mainOp), new IActionOperation[] { mainOp });
		op.add(new RefreshResourcesOperation(mainOp));
		op.add(new NotifyUnresolvedConflictOperation(mainOp));

		return op;
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

}
