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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.svn.ui.synchronize.action.CleanUpActionHelper;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Cleanup logical model action implementation for Synchronize view
 * 
 * @author Igor Burilo
 */
public class CleanUpModelAction extends AbstractSynchronizeLogicalModelAction {

	protected CleanUpActionHelper actionHelper;

	public CleanUpModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		actionHelper = new CleanUpActionHelper(this, configuration);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);

		IResource[] selectedResources = getAllSelectedResources();
		if (FileUtility.checkForResourcesPresence(selectedResources, IStateFilter.SF_VERSIONED_FOLDERS,
				IResource.DEPTH_ZERO)) {
			return true;
		}
		return false;
	}

	@Override
	protected IActionOperation getOperation() {
		return actionHelper.getOperation();
	}

}
