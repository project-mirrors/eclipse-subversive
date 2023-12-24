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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
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
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.RefreshRepositoryLocationsOperation;
import org.eclipse.team.svn.ui.panel.common.InputRevisionPanel;

/**
 * Select revision for any repository resource
 * 
 * @author Alexander Gurov
 */
public class SelectResourceRevisionAction extends AbstractRepositoryTeamAction {
	public SelectResourceRevisionAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		this.runImpl(resources);
	}

	protected void runImpl(IRepositoryResource[] resources) {
		IActionOperation op = SelectResourceRevisionAction.getAddRevisionLinkOperation(resources, getShell());
		if (op != null) {
			runScheduled(op);
		}
	}

	public static IActionOperation getAddRevisionLinkOperation(IRepositoryResource[] resources, Shell shell) {
		SVNRevision selectedRevision = null;
		final String comment[] = new String[1];

		InputRevisionPanel panel = new InputRevisionPanel(resources.length == 1 ? resources[0] : null, false, null);
		DefaultDialog dialog = new DefaultDialog(shell, panel);
		if (dialog.open() == Window.OK) {
			comment[0] = panel.getRevisionComment();
			if (resources.length == 1) {
				selectedRevision = panel.getSelectedRevision();
				resources[0] = SVNUtility.copyOf(resources[0]);
				resources[0].setSelectedRevision(selectedRevision);
			}

			final LocateResourceURLInHistoryOperation locateOp = new LocateResourceURLInHistoryOperation(resources);
			AbstractActionOperation mainOp = new AddRevisionLinkOperation((IRevisionLinkProvider) () -> {
				IRepositoryResource[] resources1 = locateOp.getRepositoryResources();
				IRevisionLink[] links = new IRevisionLink[resources1.length];
				for (int i = 0; i < resources1.length; i++) {
					links[i] = SVNUtility.createRevisionLink(resources1[i]);
					links[i].setComment(comment[0]);
				}
				return links;
			}, selectedRevision);
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			op.add(locateOp);
			op.add(mainOp, new IActionOperation[] { locateOp });
			op.add(new SaveRepositoryLocationsOperation());
			HashSet<IRepositoryLocation> locations = new HashSet<>();
			for (IRepositoryResource resource : resources) {
				locations.add(resource.getRepositoryLocation());
			}
			op.add(new RefreshRepositoryLocationsOperation(locations.toArray(new IRepositoryLocation[locations.size()]),
					true));
			return op;
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return getSelectedRepositoryResources().length > 0;
	}

}
