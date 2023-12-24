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

package org.eclipse.team.svn.ui.panel.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.operation.remote.GetLogMessagesOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Select resource revision panel implementation
 * 
 * @author Alexander Gurov
 */
public class SelectRevisionPanel extends SVNHistoryPanel {

	protected SVNLogEntry[] selectedLogMessages;

	public SelectRevisionPanel(GetLogMessagesOperation msgOp, boolean multiSelect, boolean useCheckboxes,
			long currentRevision) {
		super(SVNUIMessages.SelectRevisionPanel_Title, SVNUIMessages.SelectRevisionPanel_Description,
				SVNUIMessages.SelectRevisionPanel_Message, msgOp, multiSelect, useCheckboxes, currentRevision);
	}

	public long getSelectedRevision() {
		return selectedLogMessages[0].revision;
	}

	public SVNLogEntry[] getSelectedLogMessages() {
		return selectedLogMessages;
	}

	public SVNRevisionRange[] getSelectedRevisions() {
		ArrayList<SVNRevisionRange> revisions = new ArrayList<>();

		List<SVNLogEntry> selected = Arrays.asList(selectedLogMessages);
		long startRev = SVNRevision.INVALID_REVISION_NUMBER;
		long lastRev = SVNRevision.INVALID_REVISION_NUMBER;
		for (int i = 0; i < logMessages.length; i++) {
			SVNLogEntry entry = logMessages[i];
			if (selected.indexOf(entry) != -1) {
				startRev = entry.revision;
				if (lastRev == SVNRevision.INVALID_REVISION_NUMBER) {
					lastRev = entry.revision;
				}
				//add the last element to result list
				if (i == logMessages.length - 1) {
					revisions.add(new SVNRevisionRange(startRev == lastRev || startRev == 0 ? startRev : startRev - 1,
							lastRev));
				}
			} else if (lastRev != SVNRevision.INVALID_REVISION_NUMBER) {
				revisions.add(
						new SVNRevisionRange(startRev == lastRev || startRev == 0 ? startRev : startRev - 1, lastRev));
				lastRev = SVNRevision.INVALID_REVISION_NUMBER;
			}
		}

		return revisions.toArray(new SVNRevisionRange[revisions.size()]);
	}

	@Override
	public void postInit() {
		manager.setButtonEnabled(0, false);
	}

	@Override
	protected void initTableViewerListener() {
		tableViewerListener = event -> {
			if (SelectRevisionPanel.this.manager != null) {
				SVNLogEntry[] messages = SelectRevisionPanel.this.history.getSelectedLogMessages();
				SelectRevisionPanel.this.manager.setButtonEnabled(0, messages != null && messages.length > 0);
			}
		};
		history.getTreeViewer().addSelectionChangedListener(tableViewerListener);
	}

	@Override
	protected void saveChangesImpl() {
		selectedLogMessages = history.getSelectedLogMessages();
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.revisionLinkDialogContext"; //$NON-NLS-1$
	}
}
