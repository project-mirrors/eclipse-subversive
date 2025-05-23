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

package org.eclipse.team.svn.ui.wizard.checkoutas;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.SVNContainerSelectionGroup;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Checkout as folder into existing project
 * 
 * @author Alexander Gurov
 */
public class CheckoutAsFolderPage extends AbstractVerifiedWizardPage {
	protected IRepositoryResource[] repositoryResources;

	protected SVNContainerSelectionGroup group;

	protected IContainer targetFolder;

	public CheckoutAsFolderPage(IRepositoryResource[] repositoryResources) {
		super(CheckoutAsFolderPage.class.getName(), SVNUIMessages.CheckoutAsFolderPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		setDescription(SVNUIMessages.CheckoutAsFolderPage_Description);
		this.repositoryResources = repositoryResources;
	}

	public IContainer getTargetFolder() {
		return targetFolder;
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		setControl(composite);

		Listener listener = event -> {
			IPath path = group.getContainerFullPath();
			targetFolder = (IContainer) ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			CheckoutAsFolderPage.this.validateContent();
		};
		group = new SVNContainerSelectionGroup(composite, listener);
		attachTo(group, new SVNContainerSelectionGroup.SVNContainerCheckOutSelectionVerifier());

//		Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(composite, "org.eclipse.team.svn.help.checkoutAsAFolderContext"); //$NON-NLS-1$

		return composite;
	}

}
