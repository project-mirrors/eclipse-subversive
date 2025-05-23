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

package org.eclipse.team.svn.ui.synchronize.merge.action;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.GetRemoteContentsOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.operation.ClearMergeStatusesOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.svn.ui.synchronize.action.ISyncStateFilter;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Merge update action implementation
 * 
 * @author Alexander Gurov
 */
public class UpdateAction extends AbstractSynchronizeModelAction {
	protected boolean advancedMode;

	public UpdateAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		advancedMode = false;
	}

	public UpdateAction(String text, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		advancedMode = true;
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] { SyncInfo.INCOMING, SyncInfo.OUTGOING }) {
			@Override
			public boolean select(SyncInfo info) {
				return super.select(info)
						&& !IStateFilter.SF_OBSTRUCTED.accept(((AbstractSVNSyncInfo) info).getLocalResource());
			}
		};
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IResource[] allResources = syncInfoSelector.getSelectedResources();
		if (advancedMode) {
			String message;
			if (allResources.length == 1) {
				message = SVNUIMessages.AcceptAll_Message_Single;
			} else {
				message = BaseMessages.format(SVNUIMessages.AcceptAll_Message_Multi,
						new String[] { String.valueOf(allResources.length) });
			}
			MessageDialog dlg = new MessageDialog(configuration.getSite().getShell(), SVNUIMessages.AcceptAll_Title,
					null, message, MessageDialog.QUESTION,
					new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
			if (dlg.open() != 0) {
				return null;
			}
		}

		AbstractSVNSyncInfo[] infos = getSVNSyncInfos();
		HashMap<String, String> remote2local = new HashMap<>();
		ArrayList<IRepositoryResource> remoteSet = new ArrayList<>();
		ArrayList<IResource> localSet = new ArrayList<>();
		for (AbstractSVNSyncInfo element : infos) {
			ILocalResource remote = element.getRemoteChangeResource();
			if (remote instanceof IResourceChange && ISyncStateFilter.SF_ONREPOSITORY.acceptRemote(remote.getResource(),
					remote.getStatus(), remote.getChangeMask())) {
				IResource resource = element.getLocal();
				localSet.add(resource);
				IRepositoryResource remoteResource = ((IResourceChange) element.getRemoteChangeResource())
						.getOriginator();
				remoteSet.add(remoteResource);
				remote2local.put(SVNUtility.encodeURL(remoteResource.getUrl()),
						FileUtility.getWorkingCopyPath(resource));
			}
		}

		IResource[] resources = localSet.toArray(new IResource[localSet.size()]);
		if (resources.length > 0) {
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			GetRemoteContentsOperation mainOp = new GetRemoteContentsOperation(resources,
					remoteSet.toArray(new IRepositoryResource[remoteSet.size()]), remote2local, ignoreExternals);
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
			op.add(saveOp);
			op.add(mainOp);
			op.add(new RestoreProjectMetaOperation(saveOp));
			op.add(new RefreshResourcesOperation(resources));
			op.add(new ClearMergeStatusesOperation(allResources));
			return op;
		}
		return new ClearMergeStatusesOperation(allResources);
	}

}
