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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.svn.ui.synchronize.action.EditTreeConflictsActionHelper;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Edit tree conflicts logical model action implementation for Synchronize view
 * 
 * @author Igor Burilo
 */
public class EditTreeConflictsModelAction extends AbstractSynchronizeLogicalModelAction {

	protected EditTreeConflictsActionHelper actionHelper;
	
	public EditTreeConflictsModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		this.actionHelper = new EditTreeConflictsActionHelper(this, configuration);
	}
	
	protected boolean updateSelection(IStructuredSelection selection) {
		if (super.updateSelection(selection) && selection.size() == 1) {
			AbstractSVNSyncInfo syncInfo = this.getSelectedSVNSyncInfo();
			if (syncInfo != null && IStateFilter.SF_TREE_CONFLICTING.accept(syncInfo.getLocalResource())) {
				return true;
			}
		}
		return false;
	}
	
	protected IActionOperation getOperation() {		
		return this.actionHelper.getOperation();
	}

}
