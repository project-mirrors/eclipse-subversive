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

package org.eclipse.team.svn.ui.wizard.createpatch;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RepositoryResourceSelectionComposite;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Select the URL in compare with patch will be created
 * 
 * @author Alexander Gurov
 */
public class SelectRepositoryResourcePage extends AbstractVerifiedWizardPage {
	protected RepositoryResourceSelectionComposite selectComposite;

	protected IRepositoryResource baseResource;

	protected IRepositoryResource selectedResource;

	public SelectRepositoryResourcePage(IRepositoryResource baseResource) {
		super(
				SelectRepositoryResourcePage.class.getName(), SVNUIMessages.SelectRepositoryResourcePage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		setDescription(SVNUIMessages.SelectRepositoryResourcePage_Description);
		selectedResource = this.baseResource = baseResource;
	}

	public IRepositoryResource getSelectedResource() {
		return selectedResource;
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		GridData data = null;
		GridLayout layout = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 4;
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		selectComposite = new RepositoryResourceSelectionComposite(
				composite, SWT.NONE, this, "patchUrl", baseResource, true, //$NON-NLS-1$
				SVNUIMessages.SelectRepositoryResourcePage_Select_Title,
				SVNUIMessages.SelectRepositoryResourcePage_Select_Description,
				RepositoryResourceSelectionComposite.MODE_DEFAULT, RepositoryResourceSelectionComposite.TEXT_BASE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 550;
		selectComposite.setLayoutData(data);

//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, "org.eclipse.team.svn.help.patchRemoteContext"); //$NON-NLS-1$

		return composite;
	}

	@Override
	public IWizardPage getNextPage() {
		selectedResource = selectComposite.getSelectedResource();
		selectComposite.saveHistory();
		return super.getNextPage();
	}

}
