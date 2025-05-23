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

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.action.AbstractRecursiveTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.UpdateToRevisionPanel;
import org.eclipse.team.svn.ui.utility.UnacceptableOperationNotificator;

/**
 * Team services menu update to revision action implementation Allows to specify revision and depth for update
 * 
 * @author Igor Burilo
 */
public class UpdateToRevisionAction extends AbstractRecursiveTeamAction {

	@Override
	public void runImpl(IAction action) {
		IResource[] resources = UnacceptableOperationNotificator.shrinkResourcesWithNotOnRespositoryParents(
				getShell(), this.getSelectedResources(IStateFilter.SF_ONREPOSITORY));
		if (resources == null || resources.length == 0) {
			return;
		}

		if (checkForResourcesPresenceRecursive(IStateFilter.SF_REVERTABLE)) {
			IResource[] missing = this.getSelectedResourcesRecursive(UpdateAction.SF_MISSING_RESOURCES);
			if (missing.length > 0 && !UpdateAction.updateMissing(getShell(), missing)) {
				return;
			}
		}

		//get revision and depth
		IResource resourceForRevisionSelection;
		if (resources.length > 1) {
			//at first try to shrink
			IResource[] shrinkedResources = FileUtility.shrinkChildNodes(resources);
			resourceForRevisionSelection = shrinkedResources.length > 1
					? shrinkedResources[0].getProject()
					: shrinkedResources[0];
		} else {
			resourceForRevisionSelection = resources[0];
		}

		boolean canShowUpdateDepthPath = resources.length == 1;
		IRepositoryResource repositoryResource = SVNRemoteStorage.instance()
				.asRepositoryResource(resourceForRevisionSelection);
		UpdateToRevisionPanel panel = new UpdateToRevisionPanel(repositoryResource, canShowUpdateDepthPath);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == 0) {
			runScheduled(UpdateAction.getUpdateOperation(resources, panel.getRevision(), panel.getDepth(),
					panel.isStickyDepth(), panel.getUpdateDepthPath()));
		}
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresence(IStateFilter.SF_ONREPOSITORY);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

}
