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
import java.util.Arrays;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.core.subscribers.WorkingSetFilteredSyncInfoCollector;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SubscriberParticipantPage;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePage;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ParticipantPagePane;

public class PaneParticipantHelper {

	protected boolean isParticipantPane;

	protected BasePaneParticipant participant;

	protected ISynchronizePageConfiguration syncPageConfiguration;

	protected ParticipantPagePane pagePane;

	protected List<IResource> resourcesRemovedFromPane = new ArrayList<>();

	protected ISyncInfoSetChangeListener paneSyncInfoSetListener;

	public PaneParticipantHelper() {
		isParticipantPane = PaneParticipantHelper.isParticipantPaneOption();
	}

	public void init(BasePaneParticipant participant) {
		this.participant = participant;
		syncPageConfiguration = this.participant.createPageConfiguration();
	}

	public void initListeners() {
		//sync view listener
		SyncInfoSet paneSyncInfoSet = getPaneSyncInfoSet();
		paneSyncInfoSetListener = new PaneSyncInfoSetListener();
		paneSyncInfoSet.addSyncSetChangedListener(paneSyncInfoSetListener);
	}

	public boolean isParticipantPane() {
		return isParticipantPane;
	}

	public static boolean isParticipantPaneOption() {
		return SVNTeamPreferences.getBehaviourBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.BEHAVIOUR_SHOW_SELECTED_RESOURCES_IN_SYNC_PANE_NAME);
	}

	public ISynchronizePageConfiguration getSyncPageConfiguration() {
		return syncPageConfiguration;
	}

	public BasePaneParticipant getParticipant() {
		return participant;
	}

	public IResource[] getSelectedResources() {
		SyncInfoSet syncInfoSet = getPaneSyncInfoSetToProcess();
		return syncInfoSet.getResources();
	}

	public IResource[] getNotSelectedResources() {
		/*
		 * As we can delete resources using 'Remove from View' action,
		 * we need to process not selected resources.
		 */
		return resourcesRemovedFromPane.toArray(new IResource[0]);
	}

	public Control createChangesPage(Composite composite) {
		pagePane = new ParticipantPagePane(UIMonitorUtility.getShell(), true /* modal */, syncPageConfiguration,
				participant);
		Control control = pagePane.createPartControl(composite);
		return control;
	}

	protected SyncInfoSet getPaneSyncInfoSet() {
		SyncInfoSet syncInfoSet = null;
		ISynchronizePage page = syncPageConfiguration.getPage();
		if (page instanceof SubscriberParticipantPage) {
			WorkingSetFilteredSyncInfoCollector collector = ((SubscriberParticipantPage) page).getCollector();
			syncInfoSet = collector.getWorkingSetSyncInfoSet();
		}
		return syncInfoSet;
	}

	protected SyncInfoSet getPaneSyncInfoSetToProcess() {
		final SyncInfoSet infos = new SyncInfoSet();
		if (syncPageConfiguration == null) {
			return participant.getSyncInfoSet();
		}

		final IDiffElement root = (ISynchronizeModelElement) syncPageConfiguration
				.getProperty(SynchronizePageConfiguration.P_MODEL);
		final IDiffElement[] elements = Utils.getDiffNodes(new IDiffElement[] { root });

		for (IDiffElement element : elements) {
			if (element instanceof SyncInfoModelElement) {
				SyncInfo syncInfo = ((SyncInfoModelElement) element).getSyncInfo();
				infos.add(syncInfo);
			}
		}
		return infos;
	}

	public void dispose() {
		if (syncPageConfiguration != null) {
			SyncInfoSet paneSyncInfoSet = getPaneSyncInfoSet();
			paneSyncInfoSet.removeSyncSetChangedListener(paneSyncInfoSetListener);
		}

		// Disposing of the page pane will dispose of the page and the configuration
		if (pagePane != null) {
			pagePane.dispose();
		}

		if (participant != null) {
			participant.dispose();
		}
	}

	public void expandPaneTree() {
		Viewer viewer = syncPageConfiguration.getPage().getViewer();
		if (viewer instanceof TreeViewer) {
			try {
				viewer.getControl().setRedraw(false);
				((TreeViewer) viewer).expandAll();
			} finally {
				viewer.getControl().setRedraw(true);
			}
		}
	}

	/*
	 * Pane validator
	 */
	public static class PaneVerifier extends AbstractVerifier {

		protected PaneParticipantHelper paneParticipantHelper;

		public PaneVerifier(PaneParticipantHelper paneParticipantHelper) {
			this.paneParticipantHelper = paneParticipantHelper;
		}

		@Override
		protected String getErrorMessage(Control input) {
			IResource[] resources = paneParticipantHelper.getSelectedResources();
			if (resources.length == 0) {
				return SVNUIMessages.ParticipantPagePane_Verifier_Error;
			}
			return null;
		}

		@Override
		protected String getWarningMessage(Control input) {
			return null;
		}
	}

	/*
	 * Listens to changes in sync view for pane.
	 * As we need to track not selected resources(e.g. removed from view), current listener
	 * tracks removed resources
	 */
	private class PaneSyncInfoSetListener implements ISyncInfoSetChangeListener {

		@Override
		public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
			IResource[] removed = event.getRemovedResources();
			if (removed.length > 0) {
				resourcesRemovedFromPane.addAll(Arrays.asList(removed));
			}
		}

		@Override
		public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
		}

		@Override
		public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
		}
	}
}
