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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.wizard.copymove;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RepositoryTreeComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.panel.common.RepositoryTreePanel;
import org.eclipse.team.svn.ui.panel.common.RepositoryTreePanel.ProjectRoot;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.repository.model.RepositoryLocationFilter;
import org.eclipse.team.svn.ui.repository.model.RepositoryResource;
import org.eclipse.team.svn.ui.repository.model.RepositoryRevisions;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourceNameVerifier;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;

/**
 * A page for selecting location to copy or move resource to.
 * 
 * @author Alexei Goncharov
 */
public class SelectDestinationPage extends AbstractVerifiedWizardPage {
	protected IRepositoryResource[] resources;

	protected RepositoryTreeComposite repositoryTree;

	protected Text newResourceName;

	public SelectDestinationPage(IRepositoryResource[] resources) {
		super(SelectDestinationPage.class.getName(), SVNUIMessages.RepositoryTreePanel_Description,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		setDescription(AbstractDialogPanel.makeToBeOperatedMessage(resources) + "\r\n" //$NON-NLS-1$
				+ SVNUIMessages.AbstractCopyToMoveTo_Message);
		this.resources = resources;
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		repositoryTree = new RepositoryTreeComposite(composite, SWT.BORDER, false,
				new RepositoryTreePanel.ProjectRoot(resources[0], false));
		ProjectRoot root = (ProjectRoot) repositoryTree.getRepositoryTreeViewer().getInput();
		repositoryTree.getRepositoryTreeViewer().setExpandedElements(root.getChildren(null)[0]);
		String url = resources[0].getRepositoryLocation().getRepositoryRootUrl();
		repositoryTree.setFilter(new RepositoryLocationFilter(url) {
			@Override
			public boolean accept(Object obj) {
				if (obj instanceof RepositoryFile || obj instanceof RepositoryRevisions
						|| obj instanceof RepositoryFolder
								&& acceptYourself(((RepositoryFolder) obj).getRepositoryResource())) {
					return false;
				}
				return super.accept(obj);
			}

			private boolean acceptYourself(IRepositoryResource resource) {
				for (IRepositoryResource element : resources) {
					if (resource.equals(element)) {
						return true;
					}
				}
				return false;
			}
		});
		repositoryTree.setLayoutData(data);
		repositoryTree.getRepositoryTreeViewer().addSelectionChangedListener(event -> SelectDestinationPage.this.validateContent()
		);

		data = new GridData();
		Label name = new Label(composite, SWT.NONE);
		name.setText(
				resources.length == 1 ? SVNUIMessages.CopyMove_SubFolder_One : SVNUIMessages.CopyMove_SubFolder_Multi);
		name.setLayoutData(data);
		data = new GridData();
		data.horizontalAlignment = SWT.FILL;
		newResourceName = new Text(composite, SWT.BORDER);
		newResourceName.setLayoutData(data);
		CompositeVerifier verifier = new CompositeVerifier();
		if (resources.length == 1) {
			verifier.add(new NonEmptyFieldVerifier(name.getText()));
			newResourceName.setText(resources[0].getName());
		}
		verifier.add(new ResourceNameVerifier(name.getText(),
				CoreExtensionsManager.instance()
						.getSVNConnectorFactory()
						.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x));
		attachTo(newResourceName, verifier);

		return composite;
	}

	@Override
	public boolean isPageComplete() {
		return super.isPageComplete() && !repositoryTree.getRepositoryTreeViewer().getSelection().isEmpty();
	}

	public IRepositoryResource getDestination() {
		IStructuredSelection sel = (IStructuredSelection) repositoryTree.getRepositoryTreeViewer().getSelection();
		return ((RepositoryResource) sel.getFirstElement()).getRepositoryResource();
	}

	public String getNewResourceName() {
		return newResourceName.getText();
	}

}
