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
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.CreateFolderOperation;
import org.eclipse.team.svn.core.operation.remote.SetRevisionAuthorNameOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.panel.remote.CreateFolderPanel;

/**
 * Create remote folder action
 * 
 * @author Alexander Gurov
 */
public class CreateFolderAction extends AbstractRepositoryTeamAction {

	public CreateFolderAction() {
	}

	@Override
	public void runImpl(IAction action) {
		CreateFolderPanel panel = new CreateFolderPanel();
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);

		if (dialog.open() == 0) {
			IRepositoryResource[] resources = getSelectedRepositoryResources();
			String folder = panel.getResourceName();
			String message = panel.getMessage();

			CreateFolderOperation mainOp = new CreateFolderOperation(resources[0], folder, message);
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

			op.add(mainOp);
			op.add(new RefreshRemoteResourcesOperation(resources));
			op.add(new SetRevisionAuthorNameOperation(mainOp, Options.FORCE), new IActionOperation[] { mainOp });

			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		return resources.length == 1 && resources[0].getSelectedRevision().getKind() == Kind.HEAD;
	}

}
