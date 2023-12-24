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
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;

/**
 * Refresh repository tree action
 * 
 * @author Alexander Gurov
 */
public class RefreshAction extends AbstractRepositoryTeamAction {

	public RefreshAction() {
		super();
	}
	
	public void runImpl(IAction action) {
		this.runBusy(new RefreshRemoteResourcesOperation(this.getSelectedRepositoryResources()));
	}

	public boolean isEnabled() {
		return true;
	}

}
