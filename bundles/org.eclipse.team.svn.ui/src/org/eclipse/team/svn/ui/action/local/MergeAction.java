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
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.JavaHLMergeOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractNonRecursiveTeamAction;
import org.eclipse.team.svn.ui.dialog.AdvancedDialog;
import org.eclipse.team.svn.ui.dialog.OperationErrorDialog;
import org.eclipse.team.svn.ui.operation.ShowMergeViewOperation;
import org.eclipse.team.svn.ui.panel.local.MergePanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Merge action implementation
 * 
 * @author Alexander Gurov
 */
public class MergeAction extends AbstractNonRecursiveTeamAction {

	public MergeAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource[] resources = this.getSelectedResources(IStateFilter.SF_EXCLUDE_DELETED);

		if (!OperationErrorDialog.isAcceptableAtOnce(resources, SVNUIMessages.MergeAction_MergeError, getShell())) {
			return;
		}

		IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(resources[0]);
		long revision = SVNRemoteStorage.instance().asLocalResourceAccessible(resources[0]).getRevision();

		if (resources.length > 1) {
			revision = SVNRevision.INVALID_REVISION_NUMBER;
			remote = remote.getRoot();
		}

		MergePanel panel = new MergePanel(resources, remote, revision);
		AdvancedDialog dialog = new AdvancedDialog(getShell(), panel);
		if (dialog.open() == 0) {
			// 2URL mode requires peg as revision
			IRepositoryResourceProvider firstSet = new IRepositoryResourceProvider.DefaultRepositoryResourceProvider(
					panel.getFirstSelection());
			IRepositoryResourceProvider secondSet = null;
			if (panel.getMode() == MergePanel.MODE_2URL) {
				secondSet = new IRepositoryResourceProvider.DefaultRepositoryResourceProvider(
						panel.getSecondSelection());
			}

			IActionOperation mergeOp = null;
			if (SVNTeamPreferences.getMergeBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.MERGE_USE_JAVAHL_NAME) /*|| panel.getMode() == MergePanel.MODE_REINTEGRATE*/) {
				JavaHLMergeOperation mainOp = null;
				if (panel.getMode() == MergePanel.MODE_2URL) {
					mainOp = new JavaHLMergeOperation(resources, firstSet, secondSet, false, panel.getIgnoreAncestry(),
							panel.getDepth());
				} else if (panel.getMode() == MergePanel.MODE_1URL) {
					mainOp = new JavaHLMergeOperation(resources, firstSet, panel.getSelectedRevisions(), false,
							panel.getIgnoreAncestry(), panel.getDepth());
				} else {
					mainOp = new JavaHLMergeOperation(resources, firstSet, false);
				}
				mainOp.setRecordOnly(panel.getRecordOnly());
				CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
				SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
				op.add(saveOp);
				op.add(mainOp);
				op.add(new RestoreProjectMetaOperation(saveOp));
				op.add(new RefreshResourcesOperation(resources));
				mergeOp = op;
			} else if (panel.getMode() == MergePanel.MODE_2URL) {
				mergeOp = new ShowMergeViewOperation(resources, firstSet, secondSet, panel.getIgnoreAncestry(),
						panel.getDepth(), getTargetPart());
				((ShowMergeViewOperation) mergeOp).setRecordOnly(panel.getRecordOnly());
			} else if (panel.getMode() == MergePanel.MODE_1URL) {
				mergeOp = new ShowMergeViewOperation(resources, firstSet, panel.getSelectedRevisions(),
						panel.getIgnoreAncestry(), panel.getDepth(), getTargetPart());
				((ShowMergeViewOperation) mergeOp).setRecordOnly(panel.getRecordOnly());
			} else {
				mergeOp = new ShowMergeViewOperation(resources, firstSet, getTargetPart());
			}
			runScheduled(mergeOp);
		}
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresence(IStateFilter.SF_EXCLUDE_DELETED);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

}
