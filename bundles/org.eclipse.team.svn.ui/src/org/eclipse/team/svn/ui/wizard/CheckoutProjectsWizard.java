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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.wizard;

import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.remote.CheckoutAction;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.utility.ArrayStructuredContentProvider;
import org.eclipse.team.svn.ui.wizard.checkoutas.CheckoutAsFolderPage;
import org.eclipse.team.svn.ui.wizard.checkoutas.ProjectLocationSelectionPage;
import org.eclipse.team.svn.ui.wizard.checkoutas.ProjectsSelectionPage;

/**
 * Checkout projects wizard
 *
 * @author Sergiy Logvin
 */
public class CheckoutProjectsWizard extends AbstractSVNWizard {

	protected IRepositoryResource[] projects;

	protected ProjectsSelectionPage projectsSelectionPage;

	protected ProjectLocationSelectionPage locationSelectionPage;

	protected CheckoutAsFolderPage selectFolderPage;

	protected HashMap name2resources;

	protected boolean respectHierarchy;

	public CheckoutProjectsWizard(IRepositoryResource[] projects, HashMap name2resources) {
		setWindowTitle(SVNUIMessages.CheckoutProjectsWizard_Title);
		this.projects = projects;
		this.name2resources = name2resources;
	}

	public String getWorkingSetName() {
		return locationSelectionPage.getWorkingSetName();
	}

	public String getLocation() {
		return locationSelectionPage.getLocation();
	}

	public boolean isCheckoutAsFoldersSelected() {
		return projectsSelectionPage.isCheckoutAsFoldersSelected();
	}

	public IContainer getTargetFolder() {
		return selectFolderPage.getTargetFolder();
	}

	@Override
	public void addPages() {
		addPage(projectsSelectionPage = new ProjectsSelectionPage());
		locationSelectionPage = new ProjectLocationSelectionPage(true, projectsSelectionPage);
		locationSelectionPage.setTitle(SVNUIMessages.CheckoutProjectsWizard_SelectLocation_Title);
		addPage(selectFolderPage = new CheckoutAsFolderPage(projects));
		addPage(locationSelectionPage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == selectFolderPage) {
			return null;
		}
		if (page == projectsSelectionPage && !projectsSelectionPage.isCheckoutAsFoldersSelected()) {
			return super.getNextPage(super.getNextPage(page));
		}
		return super.getNextPage(page);
	}

	public void postInit() {
		IStructuredContentProvider contentProvider = new ArrayStructuredContentProvider();
		HashMap resource2name = CheckoutAction.getResources2Names(name2resources);
		ITableLabelProvider labelProvider = ExtensionsManager.getInstance()
				.getCurrentCheckoutFactory()
				.getLabelProvider(resource2name);
		projectsSelectionPage.postInit(locationSelectionPage,
				(IRepositoryResource[]) resource2name.keySet()
						.toArray(new IRepositoryResource[resource2name.size()]),
				labelProvider, contentProvider);
	}

	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage instanceof ProjectsSelectionPage && isCheckoutAsFoldersSelected()) {
			return false;
		}
		return super.canFinish();
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	public boolean isRespectHierarchy() {
		return projectsSelectionPage.isRespectHierarchy();
	}

	public List getResultSelections() {
		return projectsSelectionPage.getSelectedProjects();
	}

}
