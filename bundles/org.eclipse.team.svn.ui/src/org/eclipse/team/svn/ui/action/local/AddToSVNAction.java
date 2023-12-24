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
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNWithPropertiesOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.ui.action.AbstractRecursiveTeamAction;
import org.eclipse.team.svn.ui.action.QueryResourceAddition;

/**
 * Team services menu "add to SVN" action implementation
 * 
 * @author Alexander Gurov
 */
public class AddToSVNAction extends AbstractRecursiveTeamAction {
	public AddToSVNAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource[][] resources = new QueryResourceAddition(this, getShell()).queryAdditionsSeparated();
		if (resources != null) {
			AddToSVNWithPropertiesOperation mainOp = new AddToSVNWithPropertiesOperation(resources[0], false);

			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			op.add(mainOp);
			if (resources[1].length > 0) {
				op.add(new AddToSVNWithPropertiesOperation(resources[1], true));
			}
			// refresh recursivelly starting from parents
			op.add(new RefreshResourcesOperation(
					resources[2]/*, IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL*/));

			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresence(IStateFilter.SF_IGNORED_NOT_FORBIDDEN)
				|| checkForResourcesPresenceRecursive(IStateFilter.SF_NEW);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

}
