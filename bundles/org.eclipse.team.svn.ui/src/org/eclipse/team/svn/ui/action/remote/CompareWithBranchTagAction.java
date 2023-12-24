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

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.composite.BranchTagSelectionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.remote.CompareBranchTagPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Compare with branch /tag action implementation
 * 
 * @author Alexander Gurov
 */
public class CompareWithBranchTagAction extends CompareAction {
	protected int type;

	public CompareWithBranchTagAction(int type) {
		super();
		this.type = type;
	}

	public boolean isEnabled() {
		if (super.isEnabled()) {
			IRepositoryResource first = this.getSelectedRepositoryResources()[0];
			return first.getRepositoryLocation().isStructureEnabled()
					&& SVNTeamPreferences.getRepositoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
							SVNTeamPreferences.BRANCH_TAG_CONSIDER_STRUCTURE_NAME);
		}
		return false;
	}

	public void runImpl(IAction action) {
		IRepositoryResource first = this.getSelectedRepositoryResources()[0];
		boolean considerStructure = BranchTagSelectionComposite.considerStructure(first);
		IRepositoryResource[] branchTagResources = considerStructure
				? BranchTagSelectionComposite.calculateBranchTagResources(first, this.type)
				: new IRepositoryResource[0];
		if (!(considerStructure && branchTagResources.length == 0)) {
			CompareBranchTagPanel panel = new CompareBranchTagPanel(first, this.type, branchTagResources);
			DefaultDialog dlg = new DefaultDialog(this.getShell(), panel);
			if (dlg.open() == 0 && panel.getResourceToCompareWith() != null) {
				this.doCompare(first, panel.getResourceToCompareWith(), panel.getDiffOptions());
			}
		}
	}

}
