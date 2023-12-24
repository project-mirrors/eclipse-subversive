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
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.ShowHistoryViewOperation;

/**
 * Show history for remote resource action implementation
 * 
 * @author Alexander Gurov
 */
public class ShowHistoryAction extends AbstractRepositoryTeamAction {

	public ShowHistoryAction() {
		super();
	}

	public void runImpl(IAction action) {
		IRepositoryResource resource = this.getSelectedRepositoryResources()[0];
		this.runBusy(new ShowHistoryViewOperation(resource, 0, 0));
	}

	public boolean isEnabled() {
		return this.getSelectedRepositoryResources().length == 1;
	}

}
