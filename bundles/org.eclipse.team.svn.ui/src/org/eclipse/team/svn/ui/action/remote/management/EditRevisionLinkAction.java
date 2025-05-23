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

package org.eclipse.team.svn.ui.action.remote.management;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.LocateResourceURLInHistoryOperation;
import org.eclipse.team.svn.core.operation.remote.management.AddRevisionLinkOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRevisionLink;
import org.eclipse.team.svn.core.resource.IRevisionLinkProvider;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.RefreshRepositoryLocationsOperation;
import org.eclipse.team.svn.ui.panel.common.InputRevisionPanel;
import org.eclipse.team.svn.ui.repository.model.RepositoryRevision;

/**
 * Edit revision link
 * 
 * @author Igor Burilo
 */
public class EditRevisionLinkAction extends AbstractRepositoryTeamAction {

	public EditRevisionLinkAction() {
	}

	@Override
	public void runImpl(IAction action) {
		RepositoryRevision revision = this.getAdaptedSelection(RepositoryRevision.class)[0];
		final IRevisionLink oldLink = revision.getRevisionLink();
		IRepositoryResource resource = oldLink.getRepositoryResource();
		SVNRevision oldRevision = revision.getRevision();

		InputRevisionPanel panel = new InputRevisionPanel(oldLink.getRepositoryResource(), true, oldLink.getComment());
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == Window.OK) {
			SVNRevision selectedRevision = panel.getSelectedRevision();
			final String comment = panel.getRevisionComment();

			CompositeOperation op = new CompositeOperation("Operation_EditRevisionLink", SVNUIMessages.class); //$NON-NLS-1$
			if (!oldRevision.equals(selectedRevision)) {
				//delete old link and re-create new link
				IActionOperation mainOp = new AbstractActionOperation("Operation_EditRevisionLink", //$NON-NLS-1$
						SVNUIMessages.class) {
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						oldLink.getRepositoryResource().getRepositoryLocation().removeRevisionLink(oldLink);
					}
				};

				resource = SVNUtility.copyOf(resource);
				resource.setSelectedRevision(selectedRevision);

				final LocateResourceURLInHistoryOperation locateOp = new LocateResourceURLInHistoryOperation(
						new IRepositoryResource[] { resource });

				AddRevisionLinkOperation addOp = new AddRevisionLinkOperation((IRevisionLinkProvider) () -> {
					IRepositoryResource[] resources = locateOp.getRepositoryResources();
					IRevisionLink[] links = new IRevisionLink[resources.length];
					for (int i = 0; i < resources.length; i++) {
						links[i] = SVNUtility.createRevisionLink(resources[i]);
						links[i].setComment(comment);
					}
					return links;
				}, selectedRevision);

				op.add(mainOp);
				op.add(locateOp, new IActionOperation[] { mainOp });
				op.add(addOp, new IActionOperation[] { mainOp, locateOp });
			} else {
				//change link comment
				IActionOperation mainOp = new AbstractActionOperation("Operation_EditRevisionLink", //$NON-NLS-1$
						SVNUIMessages.class) {
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						oldLink.setComment(comment);
					}
				};
				op.add(mainOp);
			}

			op.add(new SaveRepositoryLocationsOperation());
			op.add(new RefreshRepositoryLocationsOperation(
					new IRepositoryLocation[] { resource.getRepositoryLocation() }, true));
			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		return this.getAdaptedSelection(RepositoryRevision.class).length == 1;
	}
}
