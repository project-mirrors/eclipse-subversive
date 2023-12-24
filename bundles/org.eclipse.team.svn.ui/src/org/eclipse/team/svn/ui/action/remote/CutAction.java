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

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.ui.RemoteResourceTransferrable;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.PrepareRemoteResourcesTransferrableOperation;

/**
 * Cut remote resource into clipboard action implementation
 * 
 * @author Alexander Gurov
 */
public class CutAction extends AbstractRepositoryTeamAction {

	public CutAction() {
	}

	@Override
	public void runImpl(IAction action) {
		runBusy(new PrepareRemoteResourcesTransferrableOperation(
				getSelectedRepositoryResources(), RemoteResourceTransferrable.OP_CUT, getShell().getDisplay()
		));
	}

	@Override
	public boolean isEnabled() {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		if (resources.length == 0) {
			return false;
		}
		// disable transfer between different repositories
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
