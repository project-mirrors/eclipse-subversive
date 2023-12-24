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

package org.eclipse.team.svn.ui.action.remote.management;

import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRevisionLink;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DiscardConfirmationDialog;
import org.eclipse.team.svn.ui.operation.RefreshRepositoryLocationsOperation;
import org.eclipse.team.svn.ui.repository.model.RepositoryRevision;

/**
 * Discard revision links on the repository location
 * 
 * @author Alexander Gurov
 */
public class DiscardRevisionLinksAction extends AbstractRepositoryTeamAction {

	public DiscardRevisionLinksAction() {
	}

	@Override
	public void runImpl(IAction action) {
		final RepositoryRevision[] revisions = this.getAdaptedSelection(RepositoryRevision.class);
		DiscardConfirmationDialog dialog = new DiscardConfirmationDialog(getShell(), revisions.length == 1,
				DiscardConfirmationDialog.MSG_LINK);
		if (dialog.open() == 0) {
			HashSet<IRepositoryLocation> locations = new HashSet<>();
			for (RepositoryRevision revision : revisions) {
				locations.add(revision.getRevisionLink().getRepositoryResource().getRepositoryLocation());
			}
			AbstractActionOperation mainOp = new AbstractActionOperation("Operation_RemoveRevisionLinks", //$NON-NLS-1$
					SVNUIMessages.class) {
				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					for (RepositoryRevision revision : revisions) {
						final IRevisionLink link = revision.getRevisionLink();
						this.protectStep(monitor1 -> {
							IRepositoryLocation location = link.getRepositoryResource().getRepositoryLocation();
							location.removeRevisionLink(link);
						}, monitor, revisions.length);
					}
				}
			};
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			op.add(mainOp);
			op.add(new SaveRepositoryLocationsOperation());
			op.add(new RefreshRepositoryLocationsOperation(locations.toArray(new IRepositoryLocation[locations.size()]),
					true));

			runBusy(op);
		}
	}

	@Override
	public boolean isEnabled() {
		return this.getAdaptedSelection(RepositoryRevision.class).length > 0;
	}

}
