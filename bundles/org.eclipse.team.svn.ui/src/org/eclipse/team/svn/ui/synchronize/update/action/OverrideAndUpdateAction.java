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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.ClearLocalStatusesOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.ResourcesTraversalOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.RevertOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.UpdateOperation;
import org.eclipse.team.svn.core.operation.local.change.visitors.RemoveNonVersionedVisitor;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.ClearUpdateStatusesOperation;
import org.eclipse.team.svn.ui.panel.local.OverrideResourcesPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.svn.ui.synchronize.action.ISyncStateFilter;
import org.eclipse.team.svn.ui.utility.UnacceptableOperationNotificator;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Override and update conflicting files action
 * 
 * @author Alexander Gurov
 */
public class OverrideAndUpdateAction extends AbstractSynchronizeModelAction {

	public OverrideAndUpdateAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(
				new int[] { SyncInfo.OUTGOING, SyncInfo.INCOMING, SyncInfo.CONFLICTING });
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		final IResource[][] resources = new IResource[1][];
		IResource[] obstructedResources = OverrideAndUpdateAction.this.syncInfoSelector
				.getSelectedResourcesRecursive(IStateFilter.SF_OBSTRUCTED);
		obstructedResources = FileUtility.addOperableParents(obstructedResources, IStateFilter.SF_OBSTRUCTED);
		HashSet<IResource> allResources = new HashSet<>(Arrays.asList(obstructedResources));
		IResource[] changedResources = OverrideAndUpdateAction.this.syncInfoSelector
				.getSelectedResourcesRecursive(ISyncStateFilter.SF_OVERRIDE);
		changedResources = UnacceptableOperationNotificator
				.shrinkResourcesWithNotOnRespositoryParents(configuration.getSite().getShell(), changedResources);
		ArrayList<IResource> affected = new ArrayList<>();
		if (changedResources != null) {
			IResource[] changedWithOperableParents = FileUtility.addOperableParents(changedResources,
					IStateFilter.SF_NOTONREPOSITORY);
			ArrayList<IResource> changedList = new ArrayList<>(Arrays.asList(changedResources));
			for (IResource current : changedWithOperableParents) {
				if (!changedList.contains(current)) {
					changedList.add(current);
					IResource[] currentAffectedArray = FileUtility.getResourcesRecursive(new IResource[] { current },
							IStateFilter.SF_ANY_CHANGE);
					for (IResource currentAffected : currentAffectedArray) {
						if (!changedList.contains(currentAffected)) {
							affected.add(currentAffected);
						}
					}
				}
			}
			changedResources = changedWithOperableParents;
			allResources.addAll(Arrays.asList(changedResources));
		}

		if (allResources.size() > 0) {
			IResource[] fullSet = allResources.toArray(new IResource[allResources.size()]);
			OverrideResourcesPanel panel = new OverrideResourcesPanel(fullSet, fullSet,
					OverrideResourcesPanel.MSG_UPDATE, affected.toArray(new IResource[affected.size()]));
			DefaultDialog dialog = new DefaultDialog(configuration.getSite().getShell(), panel);
			if (dialog.open() != 0) {
				return null;
			}
			resources[0] = panel.getSelectedResources();
		} else {
			return null;
		}

		CompositeOperation op = new CompositeOperation("Operation_UOverrideAndUpdate", SVNUIMessages.class); //$NON-NLS-1$

		SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources[0]);
		op.add(saveOp);
		/*
		 * We should call RemoveNonVersionedResourcesOperation before revert operation, because we don't want
		 * to delete ignored resources (revert operation makes 'ignored' resource as 'new' in case if ignore properties were not committed)
		 * 
		 * Probably there are case where we need to call RemoveNonVersionedResourcesOperation once again after revert operation,
		 * but I didn't find them
		 */
		IActionOperation removeNonVersionedResourcesOp = new ResourcesTraversalOperation("Operation_RemoveNonSVN",
				SVNMessages.class, resources[0], new RemoveNonVersionedVisitor(true), IResource.DEPTH_INFINITE);
		op.add(removeNonVersionedResourcesOp);
		RevertOperation revertOp = new RevertOperation(
				FileUtility.getResourcesRecursive(resources[0], IStateFilter.SF_REVERTABLE, IResource.DEPTH_ZERO),
				true);
		op.add(revertOp);
		op.add(new ClearLocalStatusesOperation(resources[0]));
		// Obstructed resources are deleted now. So, try to revert all corresponding entries
		RevertOperation revertOp1 = new RevertOperation(
				FileUtility.getResourcesRecursive(resources[0], IStateFilter.SF_OBSTRUCTED, IResource.DEPTH_ZERO),
				true);
		op.add(revertOp1);
		op.add(new ClearLocalStatusesOperation(resources[0]));

		Map<SVNRevision, Set<IResource>> splitted = UpdateAction.splitByPegRevision(this, resources[0]);

		for (Map.Entry<SVNRevision, Set<IResource>> entry : splitted.entrySet()) {
			final IResource[] toUpdate = entry.getValue().toArray(new IResource[0]);
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			UpdateOperation mainOp = new UpdateOperation(() -> FileUtility.getResourcesRecursive(toUpdate, new IStateFilter.AbstractStateFilter() {
				@Override
				protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
					return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
							|| IStateFilter.SF_NOTEXISTS.accept(resource, state, mask);
				}

				@Override
				protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state,
						int mask) {
					return true;
				}
			}, IResource.DEPTH_ZERO), entry.getKey(), ignoreExternals);
			op.add(mainOp, new IActionOperation[] { revertOp, revertOp1, removeNonVersionedResourcesOp });
			op.add(new ClearUpdateStatusesOperation(mainOp), new IActionOperation[] { mainOp });
		}

		op.add(new RestoreProjectMetaOperation(saveOp));
		op.add(new RefreshResourcesOperation(
				resources[0]/*, IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL*/));

		return op;
	}

}
