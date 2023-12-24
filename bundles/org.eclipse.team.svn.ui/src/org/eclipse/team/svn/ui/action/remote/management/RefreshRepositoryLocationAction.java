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

package org.eclipse.team.svn.ui.action.remote.management;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.RefreshRepositoryLocationsOperation;

/**
 * Refresh repository location in the repository tree action
 * 
 * @author Alexander Gurov
 */
public class RefreshRepositoryLocationAction extends AbstractRepositoryTeamAction {

	public RefreshRepositoryLocationAction() {
	}

	@Override
	public void runImpl(IAction action) {
		runBusy(getSelection().isEmpty()
				? new RefreshRepositoryLocationsOperation(true)
				: new RefreshRepositoryLocationsOperation(getSelectedRepositoryLocations(), true));
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
