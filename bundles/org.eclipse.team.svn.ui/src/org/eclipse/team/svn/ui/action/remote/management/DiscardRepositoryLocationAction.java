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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.SVNTeamProvider;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.operation.local.NotifyProjectStatesChangedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.management.DisconnectOperation;
import org.eclipse.team.svn.core.operation.remote.management.DiscardRepositoryLocationsOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.events.ProjectStatesChangedEvent;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.dialog.DiscardConfirmationDialog;
import org.eclipse.team.svn.ui.operation.RefreshRepositoryLocationsOperation;
import org.eclipse.team.svn.ui.panel.remote.DiscardLocationFailurePanel;

/**
 * Discard location action
 * 
 * @author Alexander Gurov
 */
public class DiscardRepositoryLocationAction extends AbstractRepositoryTeamAction {

	public DiscardRepositoryLocationAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IRepositoryLocation[] locations = getSelectedRepositoryLocations();
		List<IRepositoryLocation> selection = Arrays.asList(locations);
		List<IRepositoryLocation> operateLocations = new ArrayList<>(Arrays.asList(locations));
		ArrayList<IProject> connectedProjects = new ArrayList<>();
		HashSet<IRepositoryLocation> connectedLocations = new HashSet<>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			RepositoryProvider tmp = RepositoryProvider.getProvider(project);
			if (tmp != null && SVNTeamPlugin.NATURE_ID.equals(tmp.getID())) {
				SVNTeamProvider provider = (SVNTeamProvider) tmp;
				if (selection.contains(provider.getRepositoryLocation())) {
					connectedProjects.add(project);
					connectedLocations.add(provider.getRepositoryLocation());
					operateLocations.remove(provider.getRepositoryLocation());
				}
			}
		}

		if (operateLocations.size() > 0) {
			locations = operateLocations.toArray(new IRepositoryLocation[operateLocations.size()]);
			DiscardConfirmationDialog dialog = new DiscardConfirmationDialog(getShell(), locations.length == 1,
					DiscardConfirmationDialog.MSG_LOCATION);
			if (dialog.open() == 0) {
				doDiscard(locations, null);
			}
		}
		if (connectedProjects.size() > 0) {
			ArrayList<String> locationsList = new ArrayList<>();
			for (IRepositoryLocation location : connectedLocations) {
				locationsList.add(location.getLabel());
			}
			IProject[] tmp = connectedProjects.toArray(new IProject[connectedProjects.size()]);
			DiscardLocationFailurePanel panel = new DiscardLocationFailurePanel(
					locationsList.toArray(new String[locationsList.size()]), tmp);
			int retVal = new DefaultDialog(getShell(), panel).open();
			if (retVal == 0 || retVal == 1) {
				DisconnectOperation disconnectOp = new DisconnectOperation(tmp, false);
				CompositeOperation op = new CompositeOperation(disconnectOp.getId(), disconnectOp.getMessagesClass());
				op.add(new NotifyProjectStatesChangedOperation(tmp, ProjectStatesChangedEvent.ST_PRE_DISCONNECTED));
				op.add(disconnectOp);

				if (retVal == 0) {
					op.add(new RefreshResourcesOperation(tmp, IResource.DEPTH_INFINITE,
							RefreshResourcesOperation.REFRESH_ALL));
				} else {
					op.add(new NotifyProjectStatesChangedOperation(tmp, ProjectStatesChangedEvent.ST_PRE_DELETED));
					op.add(new AbstractWorkingCopyOperation("Operation_DeleteProjects", SVNUIMessages.class, tmp) { //$NON-NLS-1$
						@Override
						protected void runImpl(IProgressMonitor monitor) throws Exception {
							IProject[] projects = (IProject[]) operableData();
							for (int i = 0; i < projects.length && !monitor.isCanceled(); i++) {
								final IProject current = projects[i];
								this.protectStep(monitor1 -> current.delete(true, monitor1), monitor, projects.length);
							}
						}
					});
				}
				doDiscard(locations, op);
			}
		}
	}

	protected void doDiscard(IRepositoryLocation[] locations, IActionOperation disconnectOp) {
		DiscardRepositoryLocationsOperation mainOp = new DiscardRepositoryLocationsOperation(locations);

		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

		if (disconnectOp != null) {
			op.add(disconnectOp);
			op.add(mainOp, new IActionOperation[] { disconnectOp });
		} else {
			op.add(mainOp);
		}
		op.add(new SaveRepositoryLocationsOperation());
		op.add(new RefreshRepositoryLocationsOperation(false));

		runScheduled(op);
	}

	@Override
	public boolean isEnabled() {
		return getSelectedRepositoryLocations().length > 0;
	}

}
