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
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Show properties action implementation for Synchronize view.
 * 
 * @author Alexei Goncharov
 */
public class ShowIncomingPropertiesAction extends AbstractSynchronizeModelAction {

	protected ShowIncomingPropertiesActionHelper actionHelper;

	public ShowIncomingPropertiesAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		actionHelper = new ShowIncomingPropertiesActionHelper(this, configuration);
	}

	public ShowIncomingPropertiesAction(String text, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		actionHelper = new ShowIncomingPropertiesActionHelper(this, configuration);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		if (selection.size() == 1 && selection.getFirstElement() instanceof SyncInfoModelElement) {
			ISynchronizeModelElement element = (ISynchronizeModelElement) selection.getFirstElement();
			if (element instanceof SyncInfoModelElement) {
				AbstractSVNSyncInfo syncInfo = (AbstractSVNSyncInfo) ((SyncInfoModelElement) selection
						.getFirstElement()).getSyncInfo();
				ILocalResource incoming = syncInfo.getRemoteChangeResource();
				if (incoming instanceof IResourceChange) {
					return IStateFilter.SF_TREE_CONFLICTING.accept(incoming)
							? IStateFilter.SF_TREE_CONFLICTING_REPOSITORY_EXIST.accept(incoming)
							: IStateFilter.ST_DELETED != incoming.getStatus();
				}
			}
		}
		return false;
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return actionHelper.getOperation();
	}

}
