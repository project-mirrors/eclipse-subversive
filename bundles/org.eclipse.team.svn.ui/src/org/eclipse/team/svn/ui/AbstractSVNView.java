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

package org.eclipse.team.svn.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.repository.RepositoryFileEditorInput;
import org.eclipse.team.svn.ui.repository.model.IResourceTreeNode;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

/**
 * Abstract SVN view implementation
 * 
 * @author Sergiy Logvin
 */
public abstract class AbstractSVNView extends ViewPart implements IResourceStatesListener {

	protected IResource wcResource;

	protected IRepositoryResource repositoryResource;

	protected String viewDescription;

	protected Object lastSelectedElement;

	protected boolean isLinkWithEditorEnabled;

	protected IPartListener partListener = new IPartListener() {
		@Override
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IEditorPart) {
				AbstractSVNView.this.editorActivated((IEditorPart) part);
			}
		}

		@Override
		public void partBroughtToTop(IWorkbenchPart part) {
			if (part == AbstractSVNView.this) {
				AbstractSVNView.this.editorActivated(AbstractSVNView.this.getViewSite().getPage().getActiveEditor());
			}
		}

		@Override
		public void partOpened(IWorkbenchPart part) {
			if (part == AbstractSVNView.this) {
				AbstractSVNView.this.editorActivated(AbstractSVNView.this.getViewSite().getPage().getActiveEditor());
			}
		}

		@Override
		public void partClosed(IWorkbenchPart part) {
		}

		@Override
		public void partDeactivated(IWorkbenchPart part) {
		}
	};

	protected IPartListener2 partListener2 = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference ref) {
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference ref) {
		}

		@Override
		public void partClosed(IWorkbenchPartReference ref) {
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference ref) {
		}

		@Override
		public void partOpened(IWorkbenchPartReference ref) {
		}

		@Override
		public void partHidden(IWorkbenchPartReference ref) {
		}

		@Override
		public void partVisible(IWorkbenchPartReference ref) {
			if (ref.getPart(true) == AbstractSVNView.this) {
				AbstractSVNView.this.editorActivated(
						AbstractSVNView.this.getViewSite().getPage().getActiveEditor());
			}
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference ref) {
		}
	};

	protected ISelectionListener selectionListener = (part, selection) -> {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structSelection = (IStructuredSelection) selection;
			lastSelectedElement = structSelection.getFirstElement();

			if (!isLinkWithEditorEnabled || !this.isPageVisible()) {
				return;
			}

			if (lastSelectedElement != null) {

				if (lastSelectedElement instanceof IResource) {
					final IResource resource = (IResource) lastSelectedElement;
					this.updateViewInput(resource);
					lastSelectedElement = null;
				} else if (lastSelectedElement instanceof IAdaptable) {
					if (lastSelectedElement instanceof IResourceTreeNode) {
						IResourceTreeNode remote = (IResourceTreeNode) lastSelectedElement;
						this.updateViewInput(remote.getRepositoryResource());
					} else {
						Object adapted = ((IAdaptable) lastSelectedElement).getAdapter(IResource.class);
						if (adapted instanceof IResource) {
							this.updateViewInput((IResource) adapted);
						}
					}
				}
			}
		}
	};

	public AbstractSVNView(String viewDescription) {
		this.viewDescription = viewDescription;
		SVNRemoteStorage.instance().addResourceStatesListener(ResourceStatesChangedEvent.class, this);
	}

	@Override
	public void createPartControl(Composite parent) {
		if (needsLinkWithEditorAndSelection()) {
			getSite().getPage().addPartListener(partListener);
			getSite().getPage().addPartListener(partListener2);
			getSite().getPage().addSelectionListener(selectionListener);
		}
	}

	@Override
	public void resourcesStateChanged(ResourceStatesChangedEvent event) {
		if (wcResource == null) {
			return;
		}
		if (event.contains(wcResource) || event.contains(wcResource.getProject())) {
			if (!wcResource.exists() || !FileUtility.isConnected(wcResource)) {
				disconnectView();
			} else {
				ILocalResource local = SVNRemoteStorage.instance().asLocalResource(wcResource);
				if (IStateFilter.SF_UNVERSIONED.accept(local) || IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
					disconnectView();
				}
			}
			refresh();
		}
	}

	protected void showResourceLabel() {
		String resourceName;
		if (wcResource != null) {
			String path = wcResource.getFullPath().toString();
			if (path.startsWith("/")) { //$NON-NLS-1$
				path = path.substring(1);
			}
			resourceName = path;
		} else if (repositoryResource != null) {
			resourceName = repositoryResource.getUrl();
		} else {
			resourceName = SVNUIMessages.SVNView_ResourceNotSelected;
		}
		setContentDescription(resourceName);
	}

	protected boolean isPageVisible() {
		return getViewSite().getPage().isPartVisible(this);
	}

	@Override
	public void dispose() {
		if (needsLinkWithEditorAndSelection()) {
			getSite().getPage().removePartListener(partListener);
			getSite().getPage().removePartListener(partListener2);
			getSite().getPage().removeSelectionListener(selectionListener);
		}
		SVNRemoteStorage.instance().removeResourceStatesListener(ResourceStatesChangedEvent.class, this);
		super.dispose();
	}

	public abstract void refresh();

	protected void editorActivated(IEditorPart editor) {
		if (editor != null && !isPageVisible()) {
			lastSelectedElement = editor;
		}

		if (editor == null || !isLinkWithEditorEnabled || !isPageVisible()) {
			return;
		}
		IEditorInput input = editor.getEditorInput();

		if (input != null) {
			if (input instanceof IFileEditorInput || input instanceof SyncInfoCompareInput) {
				Object adapter = input.getAdapter(IFile.class);
				if (adapter instanceof IFile) {
					this.updateViewInput((IFile) adapter);
				}
			} else if (input instanceof RepositoryFileEditorInput) {
				this.updateViewInput(((RepositoryFileEditorInput) input).getRepositoryResource());
			}
		}
	}

	protected void updateViewInput(IResource resource) {
	}

	protected void updateViewInput(IRepositoryResource resource) {
	}

	protected abstract boolean needsLinkWithEditorAndSelection();

	protected abstract void disconnectView();
}
