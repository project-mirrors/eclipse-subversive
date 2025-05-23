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
import org.eclipse.team.svn.core.operation.remote.RenameResourceOperation;
import org.eclipse.team.svn.core.operation.remote.SetRevisionAuthorNameOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.panel.remote.RenameResourcePanel;

/**
 * Rename remote resource action implementation
 * 
 * @author Alexander Gurov
 */
public class RenameAction extends AbstractRepositoryTeamAction {

	public RenameAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		RenameResourcePanel panel = new RenameResourcePanel(resources[0].getName());
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);

		if (dialog.open() == 0) {
			RenameResourceOperation mainOp = new RenameResourceOperation(resources[0], panel.getResourceName(),
					panel.getMessage());

			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

			op.add(mainOp);
			op.add(new RefreshRemoteResourcesOperation(SVNUtility.getCommonParents(resources)));
			op.add(new SetRevisionAuthorNameOperation(mainOp, Options.FORCE), new IActionOperation[] { mainOp });

			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		for (IRepositoryResource element : resources) {
			IRepositoryLocation location = element.getRepositoryLocation();
			if (element.getUrl().equals(location.getRoot().getUrl())
					|| element.getSelectedRevision().getKind() != Kind.HEAD
					|| element instanceof IRepositoryRoot
							&& (((IRepositoryRoot) element).getKind() == IRepositoryRoot.KIND_ROOT
									|| ((IRepositoryRoot) element).getKind() == IRepositoryRoot.KIND_LOCATION_ROOT)) {
				return false;
			}
		}
		return resources.length == 1;
	}

}
