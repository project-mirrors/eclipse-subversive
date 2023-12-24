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

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.action.remote.management.SelectResourceRevisionAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Add revision link action for Synchronize View
 * 
 * @author Igor Burilo
 */
public class AddRevisionLinkAction extends AbstractSynchronizeModelAction {

	public AddRevisionLinkAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			@Override
			public boolean select(SyncInfo info) {
				return IStateFilter.SF_ONREPOSITORY.accept(((AbstractSVNSyncInfo) info).getLocalResource());
			}
		};
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IResource[] selectedResources = FileUtility.getResourcesRecursive(getAllSelectedResources(),
				IStateFilter.SF_ONREPOSITORY, IResource.DEPTH_ZERO);
		IRepositoryResource[] resources = new IRepositoryResource[selectedResources.length];
		for (int i = 0; i < selectedResources.length; i++) {
			resources[i] = SVNRemoteStorage.instance().asRepositoryResource(selectedResources[i]);
			//create revision link for revision from working copy even if there are incoming changes
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(selectedResources[i]);
			resources[i].setSelectedRevision(SVNRevision.fromNumber(local.getRevision()));
		}
		return SelectResourceRevisionAction.getAddRevisionLinkOperation(resources,
				getConfiguration().getSite().getShell());
	}

}
