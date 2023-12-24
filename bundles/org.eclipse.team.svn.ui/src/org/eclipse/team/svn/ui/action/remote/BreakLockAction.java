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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.remote.BreakLockOperation;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;

/**
 * Break repository resources lock action
 *
 * @author Sergiy Logvin
 */
public class BreakLockAction extends AbstractRepositoryTeamAction {

	public BreakLockAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IRepositoryResource[] resources = getSelectedRepositoryResources();

		BreakLockOperation mainOp = new BreakLockOperation(resources);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(mainOp);
		op.add(new RefreshRemoteResourcesOperation(resources));

		runScheduled(op);
	}

	@Override
	public boolean isEnabled() {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		if (resources.length == 0) {
			return false;
		}
		for (IRepositoryResource element : resources) {
			if (!(element instanceof IRepositoryFile)
					|| !(element.getInfo() != null && element.getInfo().lock != null)) {
				return false;
			}
		}
		return true;
	}

}
