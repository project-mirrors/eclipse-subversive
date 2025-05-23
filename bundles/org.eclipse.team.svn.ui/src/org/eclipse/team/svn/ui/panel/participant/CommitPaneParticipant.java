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

package org.eclipse.team.svn.ui.panel.participant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.local.CommitPanel;
import org.eclipse.team.svn.ui.synchronize.AbstractSynchronizeActionGroup;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.svn.ui.synchronize.action.CleanUpAction;
import org.eclipse.team.svn.ui.synchronize.action.CompareWithLatestRevisionPaneAction;
import org.eclipse.team.svn.ui.synchronize.action.CompareWithRevisionPaneAction;
import org.eclipse.team.svn.ui.synchronize.action.CompareWithWorkingCopyPaneAction;
import org.eclipse.team.svn.ui.synchronize.action.CreateBranchAction;
import org.eclipse.team.svn.ui.synchronize.action.CreatePatchFileAction;
import org.eclipse.team.svn.ui.synchronize.action.DeletePaneAction;
import org.eclipse.team.svn.ui.synchronize.action.EditConflictsAction;
import org.eclipse.team.svn.ui.synchronize.action.EditTreeConflictsAction;
import org.eclipse.team.svn.ui.synchronize.action.ExtractOutgoingToAction;
import org.eclipse.team.svn.ui.synchronize.action.OpenInComparePaneAction;
import org.eclipse.team.svn.ui.synchronize.action.ReplaceWithLatestRevisionPaneAction;
import org.eclipse.team.svn.ui.synchronize.action.ReplaceWithRevisionPaneAction;
import org.eclipse.team.svn.ui.synchronize.action.RevertAction;
import org.eclipse.team.svn.ui.synchronize.update.action.LockAction;
import org.eclipse.team.svn.ui.synchronize.update.action.MarkAsMergedAction;
import org.eclipse.team.svn.ui.synchronize.update.action.UnlockAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;

/**
 * Commit pane participant
 * 
 * @author Igor Burilo
 */
public class CommitPaneParticipant extends BasePaneParticipant {

	protected CommitPanel commitPanel;

	public CommitPaneParticipant(ISynchronizeScope scope, CommitPanel commitPanel) {
		super(scope, commitPanel);
		this.commitPanel = commitPanel;
	}

	@Override
	protected Collection<AbstractSynchronizeActionGroup> getActionGroups() {
		List<AbstractSynchronizeActionGroup> actionGroups = new ArrayList<>();
		actionGroups.add(new CommitPaneActionGroup(commitPanel));
		return actionGroups;
	}

	/**
	 * Commit pane's action set
	 *
	 * 
	 * @author Igor Burilo
	 */
	protected static class CommitPaneActionGroup extends BasePaneActionGroup {

		protected MenuManager compareWithGroup;

		protected MenuManager replaceWithGroup;

		protected CommitPanel commitPanel;

		public CommitPaneActionGroup(CommitPanel commitPanel) {
			super(commitPanel);
			this.commitPanel = commitPanel;
		}

		@Override
		protected void configureActions(ISynchronizePageConfiguration configuration) {
			super.configureActions(configuration);

			//Open in compare editor: handle double click
			final OpenInComparePaneAction openInCompareAction = new OpenInComparePaneAction(configuration);
			configuration.setProperty(SynchronizePageConfiguration.P_OPEN_ACTION, new Action() {
				@Override
				public void run() {
					openInCompareAction.run();
				}
			});

			//separator
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					new Separator());

			//paste selected names
			AbstractSynchronizeModelAction pasteSelectedNamesAction = new AbstractSynchronizeModelAction(
					SVNUIMessages.CommitPanel_PasteNames_Action, configuration) {
				@Override
				protected IActionOperation getOperation(ISynchronizePageConfiguration configuration,
						IDiffElement[] elements) {
					commitPanel.pasteNames();
					return null;
				}
			};
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					pasteSelectedNamesAction);

			//create patch
			final CreatePatchFileAction patchAction = new CreatePatchFileAction(SVNUIMessages.CreatePatchCommand_label,
					configuration);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL, patchAction);

			//create branch
			CreateBranchAction branchAction = new CreateBranchAction(SVNUIMessages.SynchronizeActionGroup_Branch,
					configuration);
			branchAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/branch.gif")); //$NON-NLS-1$
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL, branchAction);

			//separator
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					new Separator());

			//revert
			RevertAction revertAction = new RevertAction(SVNUIMessages.SynchronizeActionGroup_Revert, configuration);
			revertAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/revert.gif")); //$NON-NLS-1$
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL, revertAction);

			//edit conflicts
			EditConflictsAction editConflictsAction = new EditConflictsAction(
					SVNUIMessages.UpdateActionGroup_EditConflicts, configuration);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					editConflictsAction);

			//edit tree conflicts
			EditTreeConflictsAction editTreeConflictsAction = new EditTreeConflictsAction(
					SVNUIMessages.UpdateActionGroup_EditTreeConflicts, configuration);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					editTreeConflictsAction);

			//mark as merged
			MarkAsMergedAction markMergedAction = new MarkAsMergedAction(
					SVNUIMessages.SynchronizeActionGroup_MarkAsMerged, configuration);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					markMergedAction);

			//separator
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					new Separator());

			//lock
			LockAction lockAction = new LockAction(SVNUIMessages.UpdateActionGroup_Lock, configuration);
			lockAction
					.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/lock.gif")); //$NON-NLS-1$
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL, lockAction);

			//unlock
			UnlockAction unlockAction = new UnlockAction(SVNUIMessages.UpdateActionGroup_Unlock, configuration);
			unlockAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/unlock.gif")); //$NON-NLS-1$
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL, unlockAction);

			//separator
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					new Separator());

			//Compare With group
			compareWithGroup = new MenuManager(SVNUIMessages.CommitPanel_CompareWith_Group);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					compareWithGroup);

			//compare with working copy
			CompareWithWorkingCopyPaneAction compareWithWorkingCopyAction = new CompareWithWorkingCopyPaneAction(
					SVNUIMessages.CompareWithWorkingCopyAction_label, configuration);
			compareWithGroup.add(compareWithWorkingCopyAction);

			//compare with latest revision
			CompareWithLatestRevisionPaneAction compareWithLatestRevisionPaneAction = new CompareWithLatestRevisionPaneAction(
					SVNUIMessages.CompareWithLatestRevisionAction_label, configuration);
			compareWithGroup.add(compareWithLatestRevisionPaneAction);

			//compare with revision
			CompareWithRevisionPaneAction compareWithRevisionPaneAction = new CompareWithRevisionPaneAction(
					SVNUIMessages.CompareWithRevisionAction_label, configuration);
			compareWithGroup.add(compareWithRevisionPaneAction);

			//Replace with group
			replaceWithGroup = new MenuManager(SVNUIMessages.CommitPanel_ReplaceWith_Group);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					replaceWithGroup);

			//replace with latest revision
			ReplaceWithLatestRevisionPaneAction replaceWithLatestRevisionPaneAction = new ReplaceWithLatestRevisionPaneAction(
					SVNUIMessages.ReplaceWithLatestRevisionAction_label, configuration);
			replaceWithGroup.add(replaceWithLatestRevisionPaneAction);

			//replace with revision
			ReplaceWithRevisionPaneAction replaceWithRevisionPaneAction = new ReplaceWithRevisionPaneAction(
					SVNUIMessages.ReplaceWithRevisionAction_label, configuration);
			replaceWithGroup.add(replaceWithRevisionPaneAction);

			//separator
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					new Separator());

			//export
			ExtractOutgoingToAction extractActionOutgoing = new ExtractOutgoingToAction(
					SVNUIMessages.ExtractToAction_Label, configuration);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					extractActionOutgoing);

			//cleanup
			CleanUpAction cleanUpAction = new CleanUpAction(SVNUIMessages.SynchronizeActionGroup_Cleanup,
					configuration);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL, cleanUpAction);

			//separator
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					new Separator());

			//delete
			DeletePaneAction deleteAction = new DeletePaneAction(SVNUIMessages.CommitPanel_Delete_Action,
					configuration);
			deleteAction.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/delete.gif")); //$NON-NLS-1$
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL, deleteAction);
		}

		@Override
		public void dispose() {
			if (compareWithGroup != null) {
				compareWithGroup.removeAll();
				compareWithGroup.dispose();
			}

			if (replaceWithGroup != null) {
				replaceWithGroup.removeAll();
				replaceWithGroup.dispose();
			}

			super.dispose();
		}
	}
}
