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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.ui.action.AbstractNonRecursiveTeamAction;
import org.eclipse.team.svn.ui.lock.LocksView;

/**
 * Scan for SVN locks action implementation
 * 
 * @author Igor Burilo
 */
public class ScanLocksAction extends AbstractNonRecursiveTeamAction {

	public void runImpl(IAction action) {
		IResource[] resources = this.getSelectedResources(IStateFilter.SF_ONREPOSITORY);
		try {
			LocksView view = (LocksView) this.getTargetPage().showView(LocksView.VIEW_ID);
			view.setResourceWithoutActionExecution(resources[0]);
			this.runScheduled(view.getUpdateViewOperation());
		} catch (Throwable e) {
			LoggedOperation.reportError(ScanLocksAction.class.getName(), e);
		}
	}

	public boolean isEnabled() {
		return this.getSelectedResources().length == 1 && this.checkForResourcesPresence(IStateFilter.SF_ONREPOSITORY);
	}
}
