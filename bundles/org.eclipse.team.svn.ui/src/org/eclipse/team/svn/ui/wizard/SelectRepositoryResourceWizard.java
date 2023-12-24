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

package org.eclipse.team.svn.ui.wizard;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.wizard.selectresource.SelectResourceLocationPage;
import org.eclipse.team.svn.ui.wizard.selectresource.SelectSimpleRepositoryLocationPage;

/**
 * Select repository resource wizard It allows to select resource through registered repositories: at first select repository, then resource
 * inside repository
 * 
 * @author Igor Burilo
 */
public class SelectRepositoryResourceWizard extends AbstractSVNWizard {

	protected SelectSimpleRepositoryLocationPage selectLocationPage;

	protected SelectResourceLocationPage selectResourcePage;

	protected IRepositoryLocation location;

	protected boolean showFolders;

	protected IRepositoryResource selectedResource;

	public SelectRepositoryResourceWizard(boolean showFolders) {
		this(showFolders, null);
	}

	public SelectRepositoryResourceWizard(boolean showFolders, IRepositoryLocation location) {
		this.showFolders = showFolders;
		this.location = location;
		setWindowTitle(SVNUIMessages.SelectRepositoryResourceWizard_Title);
	}

	@Override
	public void addPages() {
		if (location == null) {
			IRepositoryLocation[] locations = SVNRemoteStorage.instance().getRepositoryLocations();
			addPage(selectLocationPage = new SelectSimpleRepositoryLocationPage(locations));

			addPage(selectResourcePage = new SelectResourceLocationPage(showFolders, null));
		} else {
			addPage(selectResourcePage = new SelectResourceLocationPage(showFolders, location));
		}
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page instanceof SelectSimpleRepositoryLocationPage) {
			selectResourcePage.setModelRoot(selectLocationPage.getRepositoryLocation());
			return selectResourcePage;
		}
		return null;
	}

	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage instanceof SelectResourceLocationPage) {
			return selectResourcePage.getRepositoryResource() != null;
		}
		return false;
	}

	@Override
	public boolean performFinish() {
		selectedResource = selectResourcePage.getRepositoryResource();
		return true;
	}

	public IRepositoryResource getSelectedResource() {
		return selectedResource;
	}

}
