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

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.ExtractToOperationLocal;
import org.eclipse.team.svn.core.operation.local.FiniExtractLogOperation;
import org.eclipse.team.svn.core.operation.local.InitExtractLogOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Outgoing Extract To action helper for Synchronize View
 * 
 * @author Igor Burilo
 *
 */
public class ExtractOutgoingToActionHelper extends AbstractActionHelper {

	public ExtractOutgoingToActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	@Override
	public FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] { SyncInfo.OUTGOING, SyncInfo.CONFLICTING }) {
			@Override
			public boolean select(SyncInfo info) {
				if (super.select(info)) {
					AbstractSVNSyncInfo syncInfo = (AbstractSVNSyncInfo) info;
					ILocalResource local = syncInfo.getLocalResource();
					//for resources with tree conflicts check that they exist locally
					return IStateFilter.SF_TREE_CONFLICTING.accept(local) ? syncInfo.getLocal().exists() : true;
				}
				return false;
			}
		};
	}

	@Override
	public IActionOperation getOperation() {
		DirectoryDialog fileDialog = new DirectoryDialog(configuration.getSite().getShell());
		fileDialog.setText(SVNUIMessages.ExtractToAction_Select_Title);
		fileDialog.setMessage(SVNUIMessages.ExtractToAction_Select_Description);
		String path = fileDialog.open();
		if (path == null) {
			return null;
		}
		IResource[] selectedOutgoingResources = getSyncInfoSelector()
				.getSelectedResources(new ISyncStateFilter.StateFilterWrapper(IStateFilter.SF_ANY_CHANGE, true));
		HashSet<IResource> outgoingResources = new HashSet<>(Arrays.asList(selectedOutgoingResources));
		for (IResource current : selectedOutgoingResources) {
			outgoingResources.add(current.getProject());
		}
		InitExtractLogOperation logger = new InitExtractLogOperation(path);
		ExtractToOperationLocal mainOp = new ExtractToOperationLocal(
				outgoingResources.toArray(new IResource[outgoingResources.size()]), path, true, logger);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(logger);
		op.add(mainOp);
		op.add(new FiniExtractLogOperation(logger));
		return op;
	}

}
