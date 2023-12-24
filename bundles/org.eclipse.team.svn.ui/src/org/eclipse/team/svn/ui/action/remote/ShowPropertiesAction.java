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
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.remote.GetRemotePropertiesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.ShowPropertiesOperation;
import org.eclipse.ui.PlatformUI;

/**
 * Show properties action imlementation
 * 
 * @author Sergiy Logvin
 */
public class ShowPropertiesAction extends AbstractRepositoryTeamAction {

	public ShowPropertiesAction() {
		super();
	}

	public void runImpl(IAction action) {
		IRepositoryResource resource = this.getSelectedRepositoryResources()[0];
		IResourcePropertyProvider provider = new GetRemotePropertiesOperation(resource);
		ShowPropertiesOperation op = new ShowPropertiesOperation(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), resource, provider);
		this.runScheduled(op);
	}

	public boolean isEnabled() {
		return this.getSelectedRepositoryResources().length == 1;
	}

}
