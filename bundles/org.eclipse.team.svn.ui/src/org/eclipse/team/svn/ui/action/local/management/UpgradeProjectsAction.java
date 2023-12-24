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

package org.eclipse.team.svn.ui.action.local.management;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.SVNTeamProvider;
import org.eclipse.team.svn.core.operation.local.UpgradeWorkingCopyOperation;
import org.eclipse.team.svn.ui.action.AbstractLocalTeamAction;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Share multiple projects at once
 * 
 * @author Alexander Gurov
 */
public class UpgradeProjectsAction extends AbstractLocalTeamAction {
	public UpgradeProjectsAction() {
		super();
	}

	public void runImpl(IAction action) {
		IProject[] projects = this.getProjectsToUpgrade();
		UIMonitorUtility.doTaskScheduledWorkspaceModify(new UpgradeWorkingCopyOperation(projects));
	}

	protected IProject[] getProjectsToUpgrade() {
		HashSet<IProject> projects = new HashSet<IProject>(Arrays.asList(this.getSelectedProjects()));
		for (Iterator<IProject> it = projects.iterator(); it.hasNext();) {
			IProject project = it.next();
			if (!project.isAccessible() || !SVNTeamProvider.requiresUpgrade(project)) {
				it.remove();
			}
		}
		return projects.toArray(new IProject[projects.size()]);
	}

	public boolean isEnabled() {
		IProject[] projects = this.getSelectedProjects();
		for (int i = 0; i < projects.length; i++) {
			if (projects[i].isAccessible() && SVNTeamProvider.requiresUpgrade(projects[i])) {
				return true;
			}
		}
		return false;
	}

}
