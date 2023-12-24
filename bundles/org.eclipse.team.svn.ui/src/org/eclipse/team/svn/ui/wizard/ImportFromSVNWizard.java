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

package org.eclipse.team.svn.ui.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.wizard.checkoutas.SelectCheckoutResourcePage;
import org.eclipse.team.svn.ui.wizard.shareproject.AddRepositoryLocationPage;
import org.eclipse.team.svn.ui.wizard.shareproject.SelectRepositoryLocationPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Implements "Import from SVN" functionality
 * 
 * @author Alexander Gurov
 */
public class ImportFromSVNWizard extends AbstractSVNWizard implements INewWizard {
	protected SelectRepositoryLocationPage selectLocation;

	protected AddRepositoryLocationPage addLocation;

	protected SelectCheckoutResourcePage selectResource;

	public ImportFromSVNWizard() {
		setWindowTitle(SVNUIMessages.ImportFromSVNWizard_Title);
	}

	@Override
	public void addPages() {
		IRepositoryLocation[] locations = SVNRemoteStorage.instance().getRepositoryLocations();
		if (locations.length > 0) {
			addPage(selectLocation = new SelectRepositoryLocationPage(locations, true));
		}
		addPage(addLocation = new AddRepositoryLocationPage());
		addPage(selectResource = new SelectCheckoutResourcePage());
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage retVal = null;
		addLocation.setInitialUrl(null);
		if (page instanceof SelectRepositoryLocationPage && selectLocation.useExistingLocation()) {
			retVal = super.getNextPage(super.getNextPage(page));
		} else {
			retVal = super.getNextPage(page);
		}

		if (retVal instanceof SelectCheckoutResourcePage) {
			selectResource.setRepositoryLocation(selectLocation != null && selectLocation.useExistingLocation()
					? selectLocation.getRepositoryLocation()
					: addLocation.getRepositoryLocation());
		}

		return retVal;
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page instanceof SelectCheckoutResourcePage && selectLocation != null
				&& selectLocation.useExistingLocation()) {
			return super.getPreviousPage(super.getPreviousPage(page));
		}
		return super.getPreviousPage(page);
	}

	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		return currentPage instanceof SelectCheckoutResourcePage && selectResource.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		IRepositoryResource resource = selectResource.getSelectedResource();
		CheckoutAsWizard checkoutWizard = new CheckoutAsWizard(new IRepositoryResource[] { resource },
				addLocation.getOperationToPeform());
		WizardDialog dialog = new WizardDialog(getShell(), checkoutWizard);
		dialog.create();
		dialog.getShell()
				.setSize(Math.max(CheckoutAsWizard.SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
						CheckoutAsWizard.SIZING_WIZARD_HEIGHT);
		return dialog.open() == 0 ? true : false;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {

	}

}
