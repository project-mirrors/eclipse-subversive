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
 *    Tobias Bosch - [patch] bug fix: Quick fix for JavaSVN problem with "force" option
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.merge.action;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNConflictResolution;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.GetRemoteContentsOperation;
import org.eclipse.team.svn.core.operation.local.MarkResolvedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.synchronize.IMergeSyncInfo;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.operation.ClearMergeStatusesOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Override and update action
 * 
 * @author Alexander Gurov
 */
public class OverrideAndUpdateAction extends AbstractSynchronizeModelAction {

	public OverrideAndUpdateAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] { SyncInfo.CONFLICTING, SyncInfo.INCOMING }) {
			@Override
			public boolean select(SyncInfo info) {
				if (super.select(info) && info instanceof IMergeSyncInfo) {
					//check if there's a tree conflict
					ILocalResource local = ((AbstractSVNSyncInfo) info).getLocalResource();
					if (IStateFilter.SF_TREE_CONFLICTING.accept(local)) {
						IResourceChange resourceChange = ((IMergeSyncInfo) info).getRemoteResource();
						return resourceChange != null && IStateFilter.ST_DELETED != resourceChange.getStatus();
					} else {
						return true;
					}
				}
				return false;
			}
		};
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		AbstractSVNSyncInfo[] infos = getSVNSyncInfos();
		HashMap<String, String> remote2local = new HashMap<>();
		ArrayList<IRepositoryResource> remoteSet = new ArrayList<>();
		ArrayList<IResource> localSet = new ArrayList<>();
		for (AbstractSVNSyncInfo element : infos) {
			IResource resource = element.getLocal();
			localSet.add(resource);
			if (element instanceof IMergeSyncInfo) {
				IResourceChange resourceChange = ((IMergeSyncInfo) element).getRemoteResource();
				if (resourceChange != null) {
					IRepositoryResource remote = resourceChange.getOriginator();
					remoteSet.add(remote);
					remote2local.put(SVNUtility.encodeURL(remote.getUrl()), FileUtility.getWorkingCopyPath(resource));
				}
			}
		}

		if (!remote2local.isEmpty()) {
			IResource[] resources = localSet.toArray(new IResource[localSet.size()]);
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			GetRemoteContentsOperation mainOp = new GetRemoteContentsOperation(resources,
					remoteSet.toArray(new IRepositoryResource[remoteSet.size()]), remote2local, ignoreExternals);
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
			op.add(saveOp);
			IResource[] conflicting = FileUtility.getResourcesRecursive(resources, IStateFilter.SF_CONFLICTING,
					IResource.DEPTH_ZERO);
			if (conflicting.length > 0) {
				op.add(new MarkResolvedOperation(conflicting, SVNConflictResolution.Choice.CHOOSE_LOCAL_FULL,
						SVNDepth.INFINITY));
			}
			op.add(mainOp);
			op.add(new RestoreProjectMetaOperation(saveOp));
			op.add(new RefreshResourcesOperation(
					conflicting.length > 0 ? FileUtility.getParents(resources, false) : resources));
			op.add(new ClearMergeStatusesOperation(resources));
			return op;
		}
		return null;
	}

}
