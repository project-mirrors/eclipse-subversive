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

package org.eclipse.team.svn.ui.action.local.management;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.NotifyProjectStatesChangedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.management.DisconnectOperation;
import org.eclipse.team.svn.core.resource.events.ProjectStatesChangedEvent;
import org.eclipse.team.svn.ui.action.AbstractLocalTeamAction;
import org.eclipse.team.svn.ui.dialog.DisconnectDialog;

/**
 * Disconnect action implementation
 * 
 * @author Alexander Gurov
 */
public class DisconnectAction extends AbstractLocalTeamAction {

	protected static final int OP_CANCEL = 0;

	protected static final int OP_DROP = 1;

	protected static final int OP_LEAVE = 2;

	public DisconnectAction() {
	}

	@Override
	public void runImpl(IAction action) {
		int opType = selectOperationType();
		if (opType != DisconnectAction.OP_CANCEL) {
			IProject[] projects = getSelectedProjects();

			DisconnectOperation mainOp = new DisconnectOperation(projects, opType == DisconnectAction.OP_DROP);

			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

			op.add(new NotifyProjectStatesChangedOperation(projects, ProjectStatesChangedEvent.ST_PRE_DISCONNECTED));
			op.add(mainOp);
			op.add(new RefreshResourcesOperation(projects, IResource.DEPTH_INFINITE,
					RefreshResourcesOperation.REFRESH_ALL));
			op.add(new NotifyProjectStatesChangedOperation(projects, ProjectStatesChangedEvent.ST_POST_DISCONNECTED));

			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresence(IStateFilter.SF_ONREPOSITORY);
	}

	protected int selectOperationType() {
		DisconnectDialog dialog = new DisconnectDialog(getShell(), getSelectedProjects());
		if (dialog.open() == 0) {
			return dialog.dropSVNFolders() ? DisconnectAction.OP_DROP : DisconnectAction.OP_LEAVE;
		}
		return DisconnectAction.OP_CANCEL;
	}

}
