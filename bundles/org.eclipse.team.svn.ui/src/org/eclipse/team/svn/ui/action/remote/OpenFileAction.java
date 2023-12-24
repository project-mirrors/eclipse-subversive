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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.OpenRemoteFileOperation;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;

/**
 * Open remote file action implementation
 * 
 * @author Alexander Gurov
 */
public class OpenFileAction extends AbstractRepositoryTeamAction {
	public OpenFileAction() {
	}

	@Override
	public void runImpl(IAction action) {
		RepositoryFile[] resources = this.getAdaptedSelection(RepositoryFile.class);
		IRepositoryFile[] files = new IRepositoryFile[resources.length];
		for (int i = 0; i < resources.length; i++) {
			files[i] = (IRepositoryFile) resources[i].getRepositoryResource();
		}
		runScheduled(new OpenRemoteFileOperation(files, OpenRemoteFileOperation.OPEN_DEFAULT));
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		if (isEnabled()) {
			IRepositoryResource[] resources = getSelectedRepositoryResources();
			action.setImageDescriptor(SVNTeamUIPlugin.instance()
					.getWorkbench()
					.getEditorRegistry()
					.getImageDescriptor(resources[0].getName()));
		} else {
			action.setImageDescriptor(null);
		}
	}

	@Override
	public boolean isEnabled() {
		return this.getAdaptedSelection(RepositoryFile.class).length > 0;
	}

}
