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
 *    Alexander Gurov - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.operation.ShowPropertiesOperation;

/**
 * Team services menu "edit resource properties" action implementation
 * 
 * @author Alexander Gurov
 */
public class EditPropertiesAction extends AbstractWorkingCopyAction {

	public EditPropertiesAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource[] resources = this.getSelectedResources(IStateFilter.SF_EXCLUDE_PREREPLACED_AND_DELETED);
		IResourcePropertyProvider provider = new GetPropertiesOperation(resources[0]);
		ShowPropertiesOperation op = new ShowPropertiesOperation(getTargetPage(), resources[0], provider);
		runScheduled(op);
	}

	@Override
	public boolean isEnabled() {
		return this.getSelectedResources().length == 1
				&& checkForResourcesPresence(IStateFilter.SF_EXCLUDE_PREREPLACED_AND_DELETED);
	}

}
