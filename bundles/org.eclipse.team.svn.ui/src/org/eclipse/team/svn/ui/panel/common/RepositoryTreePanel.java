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
 *    Rene Link - [patch] NPE in Interactive Merge UI
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.common;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.resource.IRepositoryBase;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RepositoryTreeComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.repository.model.IParentTreeNode;
import org.eclipse.team.svn.ui.repository.model.IResourceTreeNode;
import org.eclipse.team.svn.ui.repository.model.RepositoryFictiveNode;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.repository.model.RepositoryLocationFilter;
import org.eclipse.team.svn.ui.repository.model.RepositoryResource;
import org.eclipse.team.svn.ui.repository.model.RepositoryRevisions;

/**
 * Repository tree panel
 *
 * @author Sergiy Logvin
 */
public class RepositoryTreePanel extends AbstractDialogPanel {
	protected RepositoryTreeComposite repositoryTree;

	protected IRepositoryBase root;

	protected IRepositoryResource selectedResource;

	protected IRepositoryResource[] selectedResources;

	protected boolean allowSourcesInTree;

	protected boolean allowFiles;

	protected boolean autoExpandFirstLevel;

	protected boolean showRevisionLinks;

	public RepositoryTreePanel(String title, IRepositoryResource[] resources, boolean allowSourcesInTree,
			boolean showRevisionLinks) {
		this(title, SVNUIMessages.RepositoryTreePanel_Description,
				AbstractDialogPanel.makeToBeOperatedMessage(resources), resources, allowSourcesInTree,
				showRevisionLinks);
	}

	public RepositoryTreePanel(String title, String description, String message, IRepositoryResource[] resources,
			boolean allowSourcesInTree, boolean showRevisionLinks) {
		this(title, description, message, resources, allowSourcesInTree, null, showRevisionLinks);
	}

	public RepositoryTreePanel(String title, String description, String message, IRepositoryResource[] resources,
			boolean allowSourcesInTree, IRepositoryBase root, boolean showRevisionLinks) {
		dialogTitle = title;
		dialogDescription = description;
		defaultMessage = message;
		selectedResources = resources;
		this.allowSourcesInTree = allowSourcesInTree;
		allowFiles = false;
		this.root = root;
		this.showRevisionLinks = showRevisionLinks;
	}

	public void setAutoExpandFirstLevel(boolean autoExpandFirstLevel) {
		this.autoExpandFirstLevel = autoExpandFirstLevel;
	}

	public boolean isAllowFiles() {
		return allowFiles;
	}

	public void setAllowFiles(boolean allowFiles) {
		this.allowFiles = allowFiles;
	}

	public IRepositoryResource getSelectedResource() {
		return selectedResource;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		if (root != null) {
			repositoryTree = new RepositoryTreeComposite(parent, SWT.BORDER, false, root);
		} else if (selectedResources.length > 0) {
			repositoryTree = new RepositoryTreeComposite(parent, SWT.BORDER, false,
					new ProjectRoot(selectedResources[0], showRevisionLinks));
		} else {
			repositoryTree = new RepositoryTreeComposite(parent, SWT.BORDER);
		}
		repositoryTree.setAutoExpandFirstLevel(autoExpandFirstLevel);
		if (repositoryTree.getRepositoryTreeViewer().getInput() instanceof ProjectRoot) {
			ProjectRoot root = (ProjectRoot) repositoryTree.getRepositoryTreeViewer().getInput();
			repositoryTree.getRepositoryTreeViewer().setExpandedElements(root.getChildren(null)[0]);
		}
		if (root == null && selectedResources.length > 0) {
			String url = selectedResources[0].getRepositoryLocation().getRepositoryRootUrl();
			repositoryTree.setFilter(new RepositoryLocationFilter(url) {
				@Override
				public boolean accept(Object obj) {
					if (obj instanceof RepositoryFile && !allowFiles
							|| !showRevisionLinks && obj instanceof RepositoryRevisions
							|| obj instanceof RepositoryFolder && RepositoryTreePanel.this
									.isSource(((RepositoryFolder) obj).getRepositoryResource())) {
						return false;
					}
					return super.accept(obj);
				}
			});
		}
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 185;
		repositoryTree.setLayoutData(data);
		repositoryTree.getRepositoryTreeViewer().addSelectionChangedListener(event -> {
			if (RepositoryTreePanel.this.manager != null) {
				IStructuredSelection selection = (IStructuredSelection) repositoryTree.getRepositoryTreeViewer()
						.getSelection();
				RepositoryTreePanel.this.manager.setButtonEnabled(0,
						!selection.isEmpty() && selection.getFirstElement() instanceof IResourceTreeNode);
			}
		});
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.copyMoveToDialogContext"; //$NON-NLS-1$
	}

	@Override
	protected void saveChangesImpl() {
		IStructuredSelection selection = (IStructuredSelection) repositoryTree.getRepositoryTreeViewer().getSelection();
		if (!selection.isEmpty() && selection.getFirstElement() instanceof RepositoryResource) {
			selectedResource = ((IResourceTreeNode) selection.getFirstElement()).getRepositoryResource();
		}
	}

	@Override
	protected void cancelChangesImpl() {
	}

	protected boolean isSource(IRepositoryResource resource) {
		if (!allowSourcesInTree) {
			for (IRepositoryResource element : selectedResources) {
				if (resource.equals(element)) {
					return true;
				}
			}
		}
		return false;
	}

	public static class ProjectRoot extends RepositoryFictiveNode implements IParentTreeNode {
		protected Object[] children;

		public ProjectRoot(IRepositoryResource resource, boolean showRevisionLinks) {
			IRepositoryResource projectRoot = SVNUtility.getTrunkLocation(resource);
			if (((IRepositoryRoot) projectRoot).getKind() == IRepositoryRoot.KIND_TRUNK) {
				projectRoot = projectRoot.getParent();
			}
			children = new Object[showRevisionLinks ? 3 : 2];
			children[0] = RepositoryFolder.wrapChild(null, projectRoot, null);
			children[1] = RepositoryFolder.wrapChild(null, resource.getRepositoryLocation().getRepositoryRoot(), null);
			if (showRevisionLinks) {
				children[2] = new RepositoryRevisions(resource.getRepositoryLocation());
			}
		}

		@Override
		public boolean hasChildren() {
			return true;
		}

		@Override
		public Object[] getChildren(Object o) {
			return children;
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			return null;
		}

		@Override
		public String getLabel(Object o) {
			return null;
		}

	}

}
