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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.OpenRemoteFileOperation;

/**
 * The action will open the selected file with the system external editor if available
 * 
 * @author Alexander Gurov
 */
public class OpenFileWithExternalAction extends AbstractRepositoryTeamAction {
	public OpenFileWithExternalAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		runScheduled(new OpenRemoteFileOperation(new IRepositoryFile[] { (IRepositoryFile) resources[0] },
				OpenRemoteFileOperation.OPEN_EXTERNAL));
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		if (isEnabled()) {
			IRepositoryResource[] resources = getSelectedRepositoryResources();
			action.setImageDescriptor(SVNTeamUIPlugin.instance()
					.getWorkbench()
					.getEditorRegistry()
					.getSystemExternalEditorImageDescriptor(resources[0].getName()));
		} else {
			action.setImageDescriptor(null);
		}
	}

	@Override
	public boolean isEnabled() {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		if (resources.length != 1 || !(resources[0] instanceof IRepositoryFile)) {
			return false;
		}
		return SVNTeamUIPlugin.instance()
				.getWorkbench()
				.getEditorRegistry()
				.isSystemExternalEditorAvailable(resources[0].getName());
	}

}
