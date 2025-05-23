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

import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.remote.AbstractCopyMoveResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.MoveResourcesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;

/**
 * Move repository resource action implementation
 *
 * @author Sergiy Logvin
 */
public class MoveToAction extends AbstractCopyMoveAction {
	public MoveToAction() {
		super("MoveToAction");
	}

	@Override
	protected AbstractCopyMoveResourcesOperation makeCopyOperation(IRepositoryResource destination,
			IRepositoryResource[] selected, String message, String name) {
		return new MoveResourcesOperation(destination, selected, message, name);
	}

	@Override
	protected RefreshRemoteResourcesOperation makeRefreshOperation(IRepositoryResource destination,
			IRepositoryResource[] selected) {
		IRepositoryResource[] toRefresh = new IRepositoryResource[selected.length + 1];
		System.arraycopy(selected, 0, toRefresh, 0, selected.length);
		toRefresh[selected.length] = destination;
		return new RefreshRemoteResourcesOperation(SVNUtility.getCommonParents(toRefresh));
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
			if (first != location || element.getSelectedRevision().getKind() != Kind.HEAD
					|| element instanceof IRepositoryRoot
							&& (((IRepositoryRoot) element).getKind() == IRepositoryRoot.KIND_ROOT
									|| ((IRepositoryRoot) element).getKind() == IRepositoryRoot.KIND_LOCATION_ROOT)) {
				return false;
			}
		}
		return true;
	}

}
