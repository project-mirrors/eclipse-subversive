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

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.action.AbstractRecursiveTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.EditTreeConflictsPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Edit tree conflicting resources action implementation
 * 
 * @author Igor Burilo
 */
public class EditTreeConflictsAction extends AbstractRecursiveTeamAction {

	public EditTreeConflictsAction() {
		super();
	}

	public void runImpl(IAction action) {
		IResource[] resources = this.getSelectedResources(IStateFilter.SF_TREE_CONFLICTING);
		if (resources.length > 0) {
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resources[0]);
			if (local.hasTreeConflict()) {
				EditTreeConflictsPanel editConflictsPanel = new EditTreeConflictsPanel(local);
				DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getShell(), editConflictsPanel);
				if (dialog.open() == 0 && editConflictsPanel.getOperation() != null) {
					this.runScheduled(editConflictsPanel.getOperation());
				}
			}
		}
	}

	public boolean isEnabled() {
		return this.getSelectedResources().length == 1
				&& this.checkForResourcesPresence(IStateFilter.SF_TREE_CONFLICTING);
	}

	protected boolean needsToSaveDirtyEditors() {
		return true;
	}
}
