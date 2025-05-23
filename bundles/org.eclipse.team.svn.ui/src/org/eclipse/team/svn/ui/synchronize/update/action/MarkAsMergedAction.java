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

package org.eclipse.team.svn.ui.synchronize.update.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.MarkAsMergedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.resource.ILocalFile;
import org.eclipse.team.svn.core.synchronize.UpdateSyncInfo;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.operation.ClearUpdateStatusesOperation;
import org.eclipse.team.svn.ui.operation.ShowPostCommitErrorsOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.svn.ui.utility.UnacceptableOperationNotificator;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Synchronize view <mark as merged> action implementation
 * 
 * @author Alexander Gurov
 */
public class MarkAsMergedAction extends AbstractSynchronizeModelAction {

	public MarkAsMergedAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] { SyncInfo.CONFLICTING, SyncInfo.INCOMING }) {
			@Override
			public boolean select(SyncInfo info) {
				if (super.select(info)) {
					UpdateSyncInfo sync = (UpdateSyncInfo) info;
					boolean localIsFile = sync.getLocalResource().getResource() instanceof IFile;
					boolean remoteIsFile = sync.getRemoteChangeResource() instanceof ILocalFile;
					return !IStateFilter.SF_OBSTRUCTED.accept(sync.getLocalResource()) && localIsFile == remoteIsFile;
				}
				return false;
			}
		};
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IResource[] resources = UnacceptableOperationNotificator.shrinkResourcesWithNotOnRespositoryParents(
				configuration.getSite().getShell(), syncInfoSelector.getSelectedResources());
		if (resources == null || resources.length == 0) {
			return null;
		}
		boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
				SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
		MarkAsMergedOperation mainOp = new MarkAsMergedOperation(resources, false, null, ignoreExternals);

		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

		op.add(mainOp);
		op.add(new ShowPostCommitErrorsOperation(mainOp));
		op.add(new ClearUpdateStatusesOperation(resources), new IActionOperation[] { mainOp });
		op.add(new RefreshResourcesOperation(FileUtility.getParents(resources, false)));

		return op;
	}

}
