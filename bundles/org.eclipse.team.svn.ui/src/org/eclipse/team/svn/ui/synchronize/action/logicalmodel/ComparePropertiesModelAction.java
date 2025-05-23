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

package org.eclipse.team.svn.ui.synchronize.action.logicalmodel;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.svn.ui.synchronize.action.ComparePropertiesActionHelper;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Compare properties logical model action for Synchronize View
 * 
 * @author Igor Burilo
 */
public class ComparePropertiesModelAction extends AbstractSynchronizeLogicalModelAction {

	protected ComparePropertiesActionHelper actionHelper;

	public ComparePropertiesModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		actionHelper = new ComparePropertiesActionHelper(this, configuration);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected IActionOperation getOperation() {
		return actionHelper.getOperation();
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		if (selection.size() == 1) {
			AbstractSVNSyncInfo syncInfo = getSelectedSVNSyncInfo();
			if (syncInfo != null && syncInfo.getKind() != SyncInfo.IN_SYNC) {
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

}
