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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.DeleteResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.SetRevisionAuthorNameOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.panel.common.CommentPanel;
import org.eclipse.team.svn.ui.repository.RepositoriesView;

/**
 * Delete remote resource action
 * 
 * @author Alexander Gurov
 */
public class DeleteAction extends AbstractRepositoryTeamAction {

	public DeleteAction() {
	}

	@Override
	public void runImpl(IAction action) {
		CommentPanel commentPanel = new CommentPanel(SVNUIMessages.DeleteAction_Comment_Title);
		DefaultDialog dialog = new DefaultDialog(getShell(), commentPanel);
		if (dialog.open() == 0) {
			IRepositoryResource[] resources = getSelectedRepositoryResources();
			IRepositoryResource[] commonParents = SVNUtility.getCommonParents(resources);

			DeleteResourcesOperation mainOp = new DeleteResourcesOperation(resources, commentPanel.getMessage());
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

			op.add(mainOp);
			op.add(new RefreshRemoteResourcesOperation(commonParents));
			op.add(new AbstractActionOperation("", SVNUIMessages.class) { //$NON-NLS-1$
				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					DeleteAction.this.getShell().getDisplay().syncExec(() -> {
						RepositoriesView view = RepositoriesView.instance();
						if (view == null) {
							return;
						}

						view.getRepositoryTree().fireEmptySelectionEvent();
					});
				}

				@Override
				public ISchedulingRule getSchedulingRule() {
					return null;
				}
			});
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
		return resources.length > 0;
	}

}
