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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.action.local.CreatePatchAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Create patch file action for Synchronize View
 * 
 * @author Alexei Goncharov
 */
public class CreatePatchFileAction extends AbstractSynchronizeModelAction {
	public CreatePatchFileAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] { SyncInfo.OUTGOING, SyncInfo.CONFLICTING }) {
			@Override
			public boolean select(SyncInfo info) {
				ILocalResource local = ((AbstractSVNSyncInfo) info).getLocalResource();
				return super.select(info) && (IStateFilter.SF_VERSIONED.accept(local)
						|| IStateFilter.SF_ANY_CHANGE.accept(local) && local.getResource().exists());
			}
		};
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IResource[] resources = FileUtility.shrinkChildNodes(syncInfoSelector
				.getSelectedResources(new ISyncStateFilter.StateFilterWrapper(IStateFilter.SF_ANY_CHANGE, false)));
		return CreatePatchAction.getCreatePatchOperation(resources, configuration.getSite().getShell());
	}

}
