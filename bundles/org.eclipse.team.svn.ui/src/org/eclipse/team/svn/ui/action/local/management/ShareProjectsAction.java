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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.svn.ui.action.AbstractLocalTeamAction;
import org.eclipse.team.svn.ui.wizard.ShareProjectWizard;

/**
 * Share multiple projects at once
 * 
 * @author Alexander Gurov
 */
public class ShareProjectsAction extends AbstractLocalTeamAction {
	public ShareProjectsAction() {
		super();
	}
	
	public void runImpl(IAction action) {
		IProject []projects = this.getProjectsToShare();
		
		ShareProjectWizard wizard = new ShareProjectWizard();
		wizard.init(projects);
		WizardDialog dialog = new WizardDialog(this.getShell(), wizard);
		dialog.setPageSize(500, 400);
		dialog.open();
	}

	protected IProject []getProjectsToShare() {
		HashSet<IProject> projects = new HashSet<IProject>(Arrays.asList(this.getSelectedProjects()));
		for (Iterator<IProject> it = projects.iterator(); it.hasNext(); ) {
			IProject project = it.next();
			if (!project.isAccessible() || RepositoryProvider.getProvider(project) != null) {
				it.remove();
			}
		}
		return projects.toArray(new IProject[projects.size()]);
	}
	
	public boolean isEnabled() {
		IProject []projects = this.getSelectedProjects();
		for (int i = 0; i < projects.length; i++) {
			if (projects[i].isAccessible() && RepositoryProvider.getProvider(projects[i]) == null) {
				return true;
			}
		}
		return false;
	}

}
