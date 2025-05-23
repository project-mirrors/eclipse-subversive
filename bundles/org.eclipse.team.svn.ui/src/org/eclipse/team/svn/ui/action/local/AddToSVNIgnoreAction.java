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

package org.eclipse.team.svn.ui.action.local;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNIgnoreOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNOperation;
import org.eclipse.team.svn.core.operation.local.ClearLocalStatusesOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.action.AbstractNonRecursiveTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.AddToSVNPanel;
import org.eclipse.team.svn.ui.panel.local.IgnoreMethodPanel;

/**
 * Team services menu "add to svn::ignore" action implementation
 * 
 * @author Alexander Gurov
 */
public class AddToSVNIgnoreAction extends AbstractNonRecursiveTeamAction {

	public AddToSVNIgnoreAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource[] resources = this.getSelectedResources(AddToSVNIgnoreAction.SF_NEW_AND_PARENT_VERSIONED);

		IResource[] operableParents = FileUtility.getOperableParents(resources, IStateFilter.SF_UNVERSIONED);
		if (operableParents.length > 0) {
			AddToSVNPanel panel = new AddToSVNPanel(operableParents);
			DefaultDialog dialog1 = new DefaultDialog(getShell(), panel);
			if (dialog1.open() != 0) {
				return;
			}
			operableParents = panel.getSelectedResources();
		}

		if (resources.length == 0) { // check bug 433287: is there a need to rework enablement/processing interaction due to possible asynchrony?
			return;
		}

		IgnoreMethodPanel panel = new IgnoreMethodPanel(resources);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == 0) {
			AddToSVNIgnoreOperation mainOp = new AddToSVNIgnoreOperation(resources, panel.getIgnoreType(),
					panel.getIgnorePattern());

			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

			if (operableParents.length > 0) {
				op.add(new AddToSVNOperation(operableParents));
				op.add(new ClearLocalStatusesOperation(operableParents));
			}

			op.add(mainOp);
			HashSet<IResource> tmp = new HashSet<>(Arrays.asList(resources));
			for (IResource element : resources) {
				tmp.add(element.getParent());
			}
			IResource[] resourcesAndParents = tmp.toArray(new IResource[tmp.size()]);
			op.add(new RefreshResourcesOperation(resourcesAndParents, IResource.DEPTH_INFINITE,
					RefreshResourcesOperation.REFRESH_ALL));

			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresence(AddToSVNIgnoreAction.SF_NEW_AND_PARENT_VERSIONED);
	}

	public static IStateFilter SF_NEW_AND_PARENT_VERSIONED = new IStateFilter.AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			if (state == IStateFilter.ST_NEW) {
				IContainer parent = resource.getParent();
				if (parent != null) {
					return IStateFilter.SF_VERSIONED.accept(SVNRemoteStorage.instance().asLocalResource(parent));
				}
			}
			return false;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state != IStateFilter.ST_IGNORED && state != IStateFilter.ST_OBSTRUCTED
					&& state != IStateFilter.ST_LINKED;
		}
	};

}
