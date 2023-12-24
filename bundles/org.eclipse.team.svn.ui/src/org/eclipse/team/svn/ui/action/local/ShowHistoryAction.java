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
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.operation.ShowHistoryViewOperation;

/**
 * Team services menu "show resource history" action implementation
 * 
 * @author Alexander Gurov
 */
public class ShowHistoryAction extends AbstractWorkingCopyAction {

	public ShowHistoryAction() {
		super();
	}

	public void runImpl(IAction action) {
		IResource resource = this.getSelectedResources()[0];
		this.runBusy(new ShowHistoryViewOperation(resource, 0, 0));
	}

	public boolean isEnabled() {
		return this.getSelectedResources().length == 1 && this.checkForResourcesPresence(IStateFilter.SF_VERSIONED);
	}

}
