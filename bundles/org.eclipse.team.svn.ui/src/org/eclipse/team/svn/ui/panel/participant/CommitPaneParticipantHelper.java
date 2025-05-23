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

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.local.CommitPanel;

/**
 * @author Igor Burilo
 */
public class CommitPaneParticipantHelper extends PaneParticipantHelper {

	public CommitPaneParticipantHelper() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.ui.panel.participant.PaneParticipantHelper#getPaneSyncInfoSet()
	 */
	@Override
	protected SyncInfoSet getPaneSyncInfoSetToProcess() {
		SyncInfoSet resultSet = new SyncInfoSet();

		SyncInfoSet syncSet = super.getPaneSyncInfoSetToProcess();
		SyncInfo[] syncInfos = syncSet.getSyncInfos();
		for (SyncInfo syncInfo : syncInfos) {
			int direction = syncInfo.getKind() & SyncInfo.DIRECTION_MASK;
			if (direction == SyncInfo.OUTGOING || direction == SyncInfo.CONFLICTING) {
				resultSet.add(syncInfo);
			}
		}
		return resultSet;
	}

	public static class CommitPaneVerifier extends PaneVerifier {

		public CommitPaneVerifier(PaneParticipantHelper paneParticipantHelper) {
			super(paneParticipantHelper);
		}

		@Override
		protected String getErrorMessage(Control input) {
			String errorMessage = super.getErrorMessage(input);
			if (errorMessage == null) {
				IResource[] resourcesToProcess = paneParticipantHelper.getSelectedResources();
				if (FileUtility.checkForResourcesPresenceRecursive(resourcesToProcess, new IStateFilter.OrStateFilter(
						new IStateFilter[] { IStateFilter.SF_CONFLICTING, IStateFilter.SF_TREE_CONFLICTING }))) {
					return SVNUIMessages.CommitPanel_Pane_Conflicting_Error;
				}
				return null;
			}
			return errorMessage;
		}

		@Override
		protected String getWarningMessage(Control input) {
			String message = super.getWarningMessage(input);
			if (message == null) {
				IResource[] resourcesToProcess = paneParticipantHelper.getSelectedResources();
				if (resourcesToProcess != null && resourcesToProcess.length > 0) {
					message = CommitPanel.validateResources(resourcesToProcess);
				}
			}
			return message;
		}

	}
}
