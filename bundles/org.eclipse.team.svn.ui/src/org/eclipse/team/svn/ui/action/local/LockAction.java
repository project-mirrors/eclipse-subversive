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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.action.AbstractRecursiveTeamAction;
import org.eclipse.team.svn.ui.utility.LockProposeUtility;

/**
 * Lock action implementation
 * 
 * @author Alexander Gurov
 */
public class LockAction extends AbstractRecursiveTeamAction {

	public LockAction() {
	}

	@Override
	public void runImpl(IAction action) {
		//get resources which can be locked
		List<IResource> filteredResourcesList = new ArrayList<>();
		IResource[] filteredResources = this.getSelectedResourcesRecursive(IStateFilter.SF_VERSIONED);
		for (IResource filteredResource : filteredResources) {
			if (filteredResource.getType() == IResource.FILE && filteredResource.getLocation() != null) {
				filteredResourcesList.add(filteredResource);
			}
		}

		IActionOperation op = LockProposeUtility.performLockAction(filteredResourcesList.toArray(new IResource[0]),
				false, getShell());
		if (op != null) {
			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresenceRecursive(IStateFilter.SF_READY_TO_LOCK);
	}

}
