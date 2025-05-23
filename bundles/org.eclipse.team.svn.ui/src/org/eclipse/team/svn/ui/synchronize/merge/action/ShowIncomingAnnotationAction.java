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

package org.eclipse.team.svn.ui.synchronize.merge.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.IFileChange;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.synchronize.IMergeSyncInfo;
import org.eclipse.team.svn.ui.operation.RemoteShowAnnotationOperation;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Show annotation action implementation for Merge view
 * 
 * @author Igor Burilo
 */
public class ShowIncomingAnnotationAction extends AbstractSynchronizeModelAction {

	public ShowIncomingAnnotationAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		if (selection.size() == 1 && selection.getFirstElement() instanceof SyncInfoModelElement) {
			SyncInfo syncInfo = ((SyncInfoModelElement) selection.getFirstElement()).getSyncInfo();
			if (syncInfo instanceof IMergeSyncInfo) {
				IMergeSyncInfo mergeSyncInfo = (IMergeSyncInfo) syncInfo;
				IResourceChange change = mergeSyncInfo.getRemoteResource();
				if (change != null && change instanceof IFileChange) {
					return IStateFilter.ST_DELETED != change.getStatus();
				}
			}
		}
		return false;
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		AbstractSVNSyncInfo syncInfo = getSelectedSVNSyncInfo();
		if (syncInfo instanceof IMergeSyncInfo) {
			IResourceChange change = ((IMergeSyncInfo) syncInfo).getRemoteResource();
			return new RemoteShowAnnotationOperation(change.getOriginator());
		}
		return null;
	}
}
