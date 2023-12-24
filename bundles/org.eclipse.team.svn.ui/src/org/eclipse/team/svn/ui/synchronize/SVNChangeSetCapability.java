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
 *    Alessandro Nistico - [patch] Initial API and implementation
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.CommitSetPanel;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Change Set capability implementation
 * 
 * @author Alessandro Nistico
 */
public class SVNChangeSetCapability extends ChangeSetCapability {
	public static boolean isEnabled = false;

	public static String getProposedComment(IResource[] resourcesToCommit) {
		String retVal = null;
		ChangeSet[] sets = SVNTeamPlugin.instance().getModelChangeSetManager().getSets();
		for (ChangeSet set : sets) {
			if (SVNChangeSetCapability.containsOneOf(set, resourcesToCommit)) {
				String comment = set.getComment();
				retVal = retVal == null ? comment : retVal + "\n" + comment; //$NON-NLS-1$
			}
		}
		return retVal;
	}

	public static boolean containsOneOf(ChangeSet set, IResource[] resourcesToCommit) {
		for (IResource element : resourcesToCommit) {
			if (set.contains(element)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean supportsCheckedInChangeSets() {
		return true;
	}

	@Override
	public SyncInfoSetChangeSetCollector createSyncInfoSetChangeSetCollector(
			ISynchronizePageConfiguration configuration) {
		SVNChangeSetCapability.isEnabled = true;
		return new SVNChangeSetCollector(configuration);
	}

	@Override
	public boolean supportsActiveChangeSets() {
		return true;
	}

	@Override
	public ActiveChangeSet createChangeSet(ISynchronizePageConfiguration configuration, IDiff[] diffs) {
		ActiveChangeSet set = getActiveChangeSetManager().createSet(SVNUIMessages.ChangeSet_NewSet, new IDiff[0]);
		CommitSetPanel panel = new CommitSetPanel(set, getResources(diffs), CommitSetPanel.MSG_CREATE);
		DefaultDialog dialog = new DefaultDialog(configuration.getSite().getShell(), panel);
		dialog.open();
		if (dialog.getReturnCode() != Window.OK) {
			return null;
		}
		set.add(diffs);
		return set;
	}

	private IResource[] getResources(IDiff[] diffs) {
		Set<IResource> result = new HashSet<>();
		for (IDiff diff : diffs) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null) {
				result.add(resource);
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	@Override
	public void editChangeSet(ISynchronizePageConfiguration configuration, ActiveChangeSet set) {
		CommitSetPanel panel = new CommitSetPanel(set, set.getResources(), CommitSetPanel.MSG_EDIT);
		DefaultDialog dialog = new DefaultDialog(configuration.getSite().getShell(), panel);
		dialog.open();
	}

	@Override
	public ActiveChangeSetManager getActiveChangeSetManager() {
		return SVNTeamPlugin.instance().getModelChangeSetManager();
	}

	@Override
	public boolean enableActiveChangeSetsFor(ISynchronizePageConfiguration configuration) {
		return supportsActiveChangeSets() && configuration.getMode() != ISynchronizePageConfiguration.INCOMING_MODE;
	}

	@Override
	public boolean enableCheckedInChangeSetsFor(ISynchronizePageConfiguration configuration) {
		return supportsCheckedInChangeSets() && configuration.getMode() != ISynchronizePageConfiguration.OUTGOING_MODE;
	}

}
