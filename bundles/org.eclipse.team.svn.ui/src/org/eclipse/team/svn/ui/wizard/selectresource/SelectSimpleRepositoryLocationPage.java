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

package org.eclipse.team.svn.ui.wizard.selectresource;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.team.svn.ui.wizard.shareproject.SelectRepositoryLocationPage;
import org.eclipse.ui.PlatformUI;

/**
 * Select repository location wizard page It doesn't allow to create a new location in contrast to SelectRepositoryLocationPage
 * 
 * @author Igor Burilo
 */
public class SelectSimpleRepositoryLocationPage extends AbstractVerifiedWizardPage {

	protected IRepositoryLocation location;

	protected TableViewer repositoriesView;

	protected IRepositoryLocation[] repositories;

	public SelectSimpleRepositoryLocationPage(IRepositoryLocation[] repositories) {
		super(SelectSimpleRepositoryLocationPage.class.getName(),
				SVNUIMessages.SelectSimpleRepositoryLocationPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		setDescription(SVNUIMessages.SelectSimpleRepositoryLocationPage_Description);
		this.repositories = repositories;

	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Label description = new Label(composite, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.heightHint = convertHeightInCharsToPixels(2);
		description.setLayoutData(data);
		description.setText(SVNUIMessages.SelectSimpleRepositoryLocationPage_Details);

		repositoriesView = SelectRepositoryLocationPage.createRepositoriesListTable(composite, repositories);

		repositoriesView.addDoubleClickListener(event -> {
			IWizard wizard = SelectSimpleRepositoryLocationPage.this.getWizard();
			IWizardPage nextPage = wizard.getNextPage(SelectSimpleRepositoryLocationPage.this);
			if (nextPage != null) {
				wizard.getContainer().showPage(nextPage);
			}
		});

		repositoriesView.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) repositoriesView.getSelection();
			location = (IRepositoryLocation) selection.getFirstElement();
			SelectSimpleRepositoryLocationPage.this.setPageComplete(true);
		});

		IStructuredSelection selection = (IStructuredSelection) repositoriesView.getSelection();
		location = (IRepositoryLocation) selection.getFirstElement();

		//Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(composite, "org.eclipse.team.svn.help.selectRepositoryLocationContext"); //$NON-NLS-1$

		return composite;
	}

	public IRepositoryLocation getRepositoryLocation() {
		return location;
	}

}
