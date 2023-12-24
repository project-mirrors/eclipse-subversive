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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RepositoryTreeComposite;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.team.svn.ui.repository.model.RepositoryResource;
import org.eclipse.team.svn.ui.repository.model.RepositoryRevisions;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Select repository resource wizard page
 * 
 * @author Igor Burilo
 */
public class SelectResourceLocationPage extends AbstractVerifiedWizardPage {

	protected RepositoryTreeComposite repositoryTree;

	protected IRepositoryResource resource;

	protected Object modelRoot;

	//if 'true',  then don't show files
	//if 'false', then don't allow to select folders
	protected boolean showOnlyFolders;

	public SelectResourceLocationPage(boolean showOnlyFolders, Object modelRoot) {
		super(SelectResourceLocationPage.class.getName(), SVNUIMessages.SelectResourceLocationPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		setDescription(SVNUIMessages.SelectResourceLocationPage_Description);
		this.showOnlyFolders = showOnlyFolders;
		this.modelRoot = modelRoot;
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

		repositoryTree = new RepositoryTreeComposite(composite, SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		repositoryTree.setLayoutData(data);

		repositoryTree.getRepositoryTreeViewer().addSelectionChangedListener(event -> {
			resource = null;
			IStructuredSelection sel = (IStructuredSelection) repositoryTree.getRepositoryTreeViewer()
					.getSelection();
			IRepositoryResource rr;
			if (!sel.isEmpty() && sel.getFirstElement() instanceof RepositoryResource) {
				rr = ((RepositoryResource) sel.getFirstElement()).getRepositoryResource();
				if (showOnlyFolders && !(rr instanceof IRepositoryFile)) {
					resource = rr;
				} else if (!showOnlyFolders && rr instanceof IRepositoryFile) {
					resource = rr;
				}
			}
			SelectResourceLocationPage.this.validateContent();
		});

		//filter out revisions and files (if necessary)
		repositoryTree.setFilter(obj -> {
			if (obj instanceof RepositoryRevisions || showOnlyFolders && obj instanceof RepositoryFile) {
				return false;
			}
			return true;
		});

		if (modelRoot != null) {
			repositoryTree.setModelRoot(modelRoot);
		}

		//Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, "org.eclipse.team.svn.help.selectResourceContext"); //$NON-NLS-1$

		return composite;
	}

	public void setModelRoot(Object modelRoot) {
		this.modelRoot = modelRoot;
		repositoryTree.setModelRoot(this.modelRoot);
	}

	public IRepositoryResource getRepositoryResource() {
		return resource;
	}

}
