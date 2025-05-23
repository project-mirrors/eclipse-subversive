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
import org.eclipse.core.resources.IContainer;
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
 * Compare properties action for Synchronize View
 * 
 * @author Alexei Goncharov
 */
public class ComparePropertiesAction extends AbstractSynchronizeModelAction {

	protected ComparePropertiesActionHelper actionHelper;

	public ComparePropertiesAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		actionHelper = new ComparePropertiesActionHelper(this, configuration);
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
				boolean retVal = IStateFilter.SF_EXCLUDE_DELETED.accept(incoming);
				if (incoming instanceof IResourceChange) {
					retVal &= IStateFilter.ST_DELETED != incoming.getStatus();
				}
				return retVal && (IStateFilter.SF_HAS_PROPERTIES_CHANGES.accept(incoming)
						|| IStateFilter.SF_HAS_PROPERTIES_CHANGES.accept(syncInfo.getLocalResource())
						|| incoming.getResource() instanceof IContainer);
			}
		}
		return false;
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return actionHelper.getOperation();
	}

}
