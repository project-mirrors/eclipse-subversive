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

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.remote.GetRemotePropertiesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.operation.ShowPropertiesOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.PlatformUI;

/**
 * Show properties action helper implementation for Synchronize view.
 * 
 * @author Igor Burilo
 */
public class ShowIncomingPropertiesActionHelper extends AbstractActionHelper {

	public ShowIncomingPropertiesActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	@Override
	public IActionOperation getOperation() {
		AbstractSVNSyncInfo info = getSelectedSVNSyncInfo();
		if (info == null) {
			return null;
		}
		IResourceChange change = (IResourceChange) info.getRemoteChangeResource();
		IRepositoryResource remote = change.getOriginator();
		IResourcePropertyProvider provider = new GetRemotePropertiesOperation(remote);
		ShowPropertiesOperation op = new ShowPropertiesOperation(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), remote, provider);
		return op;
	}

}
