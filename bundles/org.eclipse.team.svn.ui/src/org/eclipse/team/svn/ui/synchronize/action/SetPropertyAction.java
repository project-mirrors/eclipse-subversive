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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Set property action implementation for Synchronize view
 * 
 * @author Alexei Goncharov
 */
public class SetPropertyAction extends AbstractSynchronizeModelAction {

	protected SetPropertyActionHelper actionHelper;

	public SetPropertyAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		actionHelper = new SetPropertyActionHelper(this, configuration);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);

		IResource[] selectedResources = getAllSelectedResources();
		if (FileUtility.checkForResourcesPresence(selectedResources, IStateFilter.SF_VERSIONED, IResource.DEPTH_ZERO)) {
			return true;
		}

		return false;
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return actionHelper.getOperation();
	}

}
