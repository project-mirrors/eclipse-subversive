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

import org.eclipse.team.svn.core.operation.remote.AbstractCopyMoveResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.CopyResourcesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;

/**
 * Copy remote resource to the destination, selected on the dialog
 *
 * @author Sergiy Logvin
 */
public class CopyToAction extends AbstractCopyMoveAction {
	public CopyToAction() {
		super("CopyToAction");
	}

	@Override
	protected AbstractCopyMoveResourcesOperation makeCopyOperation(IRepositoryResource destination,
			IRepositoryResource[] selected, String message, String name) {
		return new CopyResourcesOperation(destination, selected, message, name);
	}

	@Override
	public boolean isEnabled() {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		if (resources.length == 0) {
			return false;
		}
		//disable transfer between different repositories
		IRepositoryLocation first = resources[0].getRepositoryLocation();
		for (IRepositoryResource element : resources) {
			IRepositoryLocation location = element.getRepositoryLocation();
			if (first != location) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected RefreshRemoteResourcesOperation makeRefreshOperation(IRepositoryResource destination,
			IRepositoryResource[] selected) {
		return new RefreshRemoteResourcesOperation(new IRepositoryResource[] { destination });
	}

}
