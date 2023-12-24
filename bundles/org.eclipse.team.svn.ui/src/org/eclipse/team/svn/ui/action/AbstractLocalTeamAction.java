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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.action;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.svn.core.IStateFilter;

/**
 * Local SVN team actions ancestor
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractLocalTeamAction extends AbstractSVNTeamAction {
	private static IStructuredSelection selection;

	public AbstractLocalTeamAction() {
	}

	@Override
	protected IStructuredSelection getSelection() {
		if (AbstractLocalTeamAction.selection == null) {
			AbstractLocalTeamAction.selection = StructuredSelection.EMPTY;
		}
		return AbstractLocalTeamAction.selection;
	}

	@Override
	protected void checkSelection(IStructuredSelection selection) {
		HashSet<IResource> oldSel = new HashSet<>(Arrays.asList(getSelectedResources()));
		IStructuredSelection oldSelection = getSelection();
		AbstractLocalTeamAction.selection = selection;
		HashSet<IResource> newSel = new HashSet<>(Arrays.asList(getSelectedResources()));
		AbstractLocalTeamAction.selection = oldSelection;
		if (!newSel.equals(oldSel)) {
			AbstractLocalTeamAction.selection = selection;
			FilterManager.instance().clear();
		}
	}

	protected boolean checkForResourcesPresence(IStateFilter filter) {
		return FilterManager.instance().checkForResourcesPresence(getSelectedResources(), filter, false);
	}

	protected boolean checkForResourcesPresenceRecursive(IStateFilter filter) {
		return FilterManager.instance().checkForResourcesPresenceRecursive(getSelectedResources(), filter);
	}

}
