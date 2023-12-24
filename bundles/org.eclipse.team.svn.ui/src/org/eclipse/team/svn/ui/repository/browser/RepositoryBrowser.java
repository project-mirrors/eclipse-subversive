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

package org.eclipse.team.svn.ui.repository.browser;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.AbstractSVNView;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractSVNTeamAction;
import org.eclipse.team.svn.ui.action.remote.DeleteAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileAction;
import org.eclipse.team.svn.ui.action.remote.management.DiscardRepositoryLocationAction;
import org.eclipse.team.svn.ui.action.remote.management.DiscardRevisionLinksAction;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.repository.RepositoriesView;
import org.eclipse.team.svn.ui.repository.RepositoryTreeViewer;
import org.eclipse.team.svn.ui.repository.model.IParentTreeNode;
import org.eclipse.team.svn.ui.repository.model.IResourceTreeNode;
import org.eclipse.team.svn.ui.repository.model.RepositoryError;
import org.eclipse.team.svn.ui.repository.model.RepositoryFictiveNode;
import org.eclipse.team.svn.ui.repository.model.RepositoryFictiveWorkingDirectory;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.repository.model.RepositoryLocation;
import org.eclipse.team.svn.ui.repository.model.RepositoryPending;
import org.eclipse.team.svn.ui.repository.model.RepositoryResource;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

/**
 * Repository browser
 *
 * @author Sergiy Logvin
 */
public class RepositoryBrowser extends AbstractSVNView
		implements ISelectionChangedListener, RepositoryTreeViewer.IRefreshListener {
	public static final String VIEW_ID = RepositoryBrowser.class.getName();

	protected static RepositoryBrowser instance = null;

	protected IPartListener2 partListener;

	protected RepositoryResource inputElement;

	protected Object rawInputElement;

	protected RepositoryFile selectedFile;

	protected IRepositoryLocation location;

	protected RepositoryBrowserTableViewer tableViewer;

	protected RepositoryBrowserContentProvider contentProvider;

	public RepositoryBrowser() {
		super(SVNUIMessages.RepositoriesView_Browser_Description);
		RepositoryBrowser.instance = this;
	}

	public IRepositoryResource getResource() {
		return repositoryResource;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object firstElement = getSelectedElement(event.getSelection());
		if (firstElement != null) {
			boolean selectedFileChanged = false;
			if (firstElement instanceof RepositoryFile) {
				if (!((RepositoryFile) firstElement).equals(selectedFile)) {
					selectedFileChanged = true;
					selectedFile = (RepositoryFile) firstElement;
				}
				RepositoriesView instance = RepositoriesView.instance();
				if (instance != null) {
					RepositoryTreeViewer repositoryTree = instance.getRepositoryTree();
					TreeItem[] items = repositoryTree.getIdenticalNodes(firstElement, true);
					if (items != null && items.length != 0 && items[0] != null) {
						TreeItem item = items[0].getParentItem();
						if (item != null) {
							firstElement = item.getData();
						} else {
							firstElement = repositoryTree.getInput();
						}
					}
				}
			} else if (selectedFile != null) {
				selectedFile = null;
				selectedFileChanged = true;
			}

			if (firstElement instanceof RepositoryLocation) {
				rawInputElement = firstElement;
				firstElement = ((RepositoryLocation) firstElement).getResourceWrapper();
			} else {
				rawInputElement = null;
			}
			if (firstElement instanceof RepositoryResource) {
				if (getResource() == null
						|| !getResource().equals(((RepositoryResource) firstElement).getRepositoryResource())
						|| selectedFileChanged) {
					connectTo((RepositoryResource) firstElement);
					refreshTableView();
				}
				return;
			}
		}
		disconnect();
	}

	@Override
	public void refreshed(Object data) {
		refreshTableView();
	}

	@Override
	public void setFocus() {

	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		tableViewer = new RepositoryBrowserTableViewer(parent,
				SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		tableViewer.initialize();
		contentProvider = new RepositoryBrowserContentProvider();
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setLabelProvider(new RepositoryBrowserLabelProvider(tableViewer));

		MenuManager menuMgr = RepositoriesView.newMenuInstance(tableViewer);
		tableViewer.getTable().setMenu(menuMgr.createContextMenu(tableViewer.getTable()));
		getSite().registerContextMenu(menuMgr, tableViewer);

		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager tbm = actionBars.getToolBarManager();
		Action refreshAction = new Action(SVNUIMessages.SVNView_Refresh_Label) {
			@Override
			public void run() {
				RepositoryBrowser.this.handleRefresh();
			}
		};
		refreshAction.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/refresh.gif")); //$NON-NLS-1$
		refreshAction.setToolTipText(SVNUIMessages.SVNView_Refresh_ToolTip);
		tbm.add(refreshAction);

		tableViewer.getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.F5) {
					RepositoryBrowser.this.handleRefresh();
				}
				if (tableViewer.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
					if (event.keyCode == SWT.DEL) {
						RepositoryBrowser.this.handleDeleteKey(selection);
						RepositoriesView.refresh(RepositoryBrowser.this.repositoryResource);
					}
				}
				if (event.keyCode == SWT.BS) {
					RepositoryBrowser.this.handleBackspaceKey();
				}
			}
		});
		tableViewer.addDoubleClickListener(e -> {
			ISelection selection = e.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structured = (IStructuredSelection) selection;
				if (structured.size() == 1) {
					RepositoryBrowser.this.handleDoubleClick(structured);
				}
			}
		});
		tableViewer.refresh();
		showResourceLabel();
		partListener = new IPartListener2() {
			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				this.setViewState(partRef, true);
			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
				this.setViewState(partRef, false);
			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals(RepositoryBrowser.VIEW_ID)) {
					RepositoryBrowser.this.getViewSite().getPage().removePartListener(this);
				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
			}

			protected void setViewState(IWorkbenchPartReference partRef, boolean visible) {
				if (partRef.getId().equals(RepositoryBrowser.VIEW_ID)
						|| partRef.getId().equals(RepositoriesView.VIEW_ID)) {
					if (partRef.getId().equals(RepositoryBrowser.VIEW_ID)) {
						IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
						SVNTeamPreferences.setRepositoryBoolean(store, SVNTeamPreferences.REPOSITORY_SHOW_BROWSER_NAME,
								visible);
					}
					RepositoryBrowser.this.setViewState(visible);
				}
			}
		};

		getViewSite().getPage().addPartListener(partListener);

		//Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(parent, "org.eclipse.team.svn.help.repositoryBrowserViewContext"); //$NON-NLS-1$
	}

	protected void connectTo(RepositoryResource inputElement) {
		this.inputElement = inputElement;
		repositoryResource = inputElement.getRepositoryResource();
		showResourceLabel();
	}

	protected void refreshTableView() {
		if (!tableViewer.getTable().isDisposed()) {
			boolean hideGrid = false;
			if (inputElement instanceof RepositoryFolder) {
				Object[] children = ((RepositoryFolder) inputElement).peekChildren(null);
				hideGrid = children != null && children.length > 0 && children[0] instanceof RepositoryFictiveNode;
			}
			tableViewer.getTable().setLinesVisible(!hideGrid);
			tableViewer.setInput(inputElement);
			if (selectedFile != null) {
				tableViewer.setSelection(new StructuredSelection(selectedFile), true);
			} else {
				tableViewer.getTable().setSelection(0);
			}
		}
	}

	protected void setViewState(boolean visible) {
		RepositoriesView instance = RepositoriesView.instance();
		if (instance != null) {
			instance.refreshButtonsState();
			RepositoryTreeViewer viewer = instance.getRepositoryTree();
			if (visible) {
				selectionChanged(new SelectionChangedEvent(viewer, viewer.getSelection()));
				viewer.addSelectionChangedListener(this);
				viewer.addRefreshListener(this);
			} else {
				viewer.removeSelectionChangedListener(this);
				viewer.removeRefreshListener(this);
			}
		}
		if (!visible) {
			disconnect();
		}
	}

	protected void handleRefresh() {
		if (repositoryResource != null) {
			UIMonitorUtility.doTaskBusyDefault(
					new RefreshRemoteResourcesOperation(new IRepositoryResource[] { repositoryResource }));
		}
	}

	protected void handleDeleteKey(IStructuredSelection selection) {
		Action tmp = new Action() {
		};
		AbstractSVNTeamAction action = new DeleteAction();
		action.selectionChanged(tmp, selection);
		action.setActivePart(tmp, RepositoryBrowser.this);
		if (tmp.isEnabled()) {
			action.run(tmp);
		} else {
			action = new DiscardRevisionLinksAction();
			action.selectionChanged(tmp, selection);
			action.setActivePart(tmp, RepositoryBrowser.this);
			if (tmp.isEnabled()) {
				action.run(tmp);
			} else {
				action = new DiscardRepositoryLocationAction();
				action.selectionChanged(tmp, selection);
				action.setActivePart(tmp, RepositoryBrowser.this);
				if (tmp.isEnabled()) {
					action.run(tmp);
				}
			}
		}
	}

	protected void handleBackspaceKey() {
		Table table = tableViewer.getTable();
		TableItem[] items = table.getItems();
		if (items != null && items.length != 0 && items[0] != null) {
			Object data = items[0].getData();
			if (!(data instanceof RepositoryError || data instanceof RepositoryPending)) {
				goUp();
			}
		}
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		Action tmp = new Action() {
		};
		AbstractSVNTeamAction action = new OpenFileAction();
		action.selectionChanged(tmp, selection);
		action.setActivePart(tmp, RepositoryBrowser.this);
		if (tmp.isEnabled()) {
			action.run(tmp);
		} else {
			Object node = selection.getFirstElement();
			if (node instanceof RepositoryFictiveWorkingDirectory) {
				goUp();
			} else if (node instanceof IParentTreeNode) {
				goDown(node);
			}
		}
	}

	protected void goUp() {
		RepositoriesView view = RepositoriesView.instance();
		RepositoryResource node = inputElement;
		if (view == null || node == null) {
			return;
		}

		Object parent = node.getParent();
		if (parent != null) {
			Object oldInput = node;
			if (isTopLevelNode(view.getRepositoryTree(), node)) {
				goBack(view, node);
			} else {
				RepositoryTreeViewer treeViewer = view.getRepositoryTree();
				TreeItem[] items = treeViewer.getIdenticalNodes(node, false);
				if (items != null && items.length != 0 && items[0] != null && items != parent) {
					parent = items[0].getParentItem().getData();
				}
				treeViewer.setExpandedState(parent, true);
				treeViewer.setSelection(new StructuredSelection(parent));
			}
			if (inputElement != null) {
				Table table = tableViewer.getTable();
				TableItem[] items = table.getItems();
				if (items != null && items.length != 0) {
					for (TableItem item : items) {
						Object data = item.getData();
						if (oldInput.equals(data)) {
							tableViewer.setSelection(new StructuredSelection(oldInput), true);
							break;
						}
					}
				}
			}
		}
	}

	protected void goBack(RepositoriesView view, Object node) {
		if (!view.canGoBack()) {
			return;
		}
		view.goBack();
		if (inputElement != null) {
			view.getRepositoryTree().setExpandedState(inputElement, true);
			if (node.equals(inputElement)) {
				RepositoryTreeViewer treeViewer = view.getRepositoryTree();
				TreeItem[] items = treeViewer.getIdenticalNodes(inputElement, false);
				if (items != null && items.length != 0 && items[0] != null) {
					if (items[0].getParentItem() != null) {
						Object newSelectedElement = items[0].getParentItem().getData();
						if (newSelectedElement != null) {
							treeViewer.setSelection(new StructuredSelection(newSelectedElement));
						}
					} else {
						goBack(view, node);
					}
				}
			}
		}
	}

	protected void goDown(Object node) {
		RepositoryTreeViewer treeViewer;
		RepositoriesView view = RepositoriesView.instance();
		if (view == null || !(node instanceof IResourceTreeNode)) {
			return;
		}
		treeViewer = view.getRepositoryTree();
		treeViewer.setExpandedState(rawInputElement == null ? inputElement : rawInputElement, true);
		treeViewer.setExpandedState(node, true);
		treeViewer.setSelection(new StructuredSelection(node));
	}

	protected boolean isTopLevelNode(RepositoryTreeViewer tree, Object node) {
		TreeItem[] items = tree.getIdenticalNodes(node, false);
		if (items != null && items.length != 0 && items[0] != null) {
			return items[0].getParentItem() == null;
		}
		return true;
	}

	protected void disconnect() {
		repositoryResource = null;
		inputElement = null;
		showResourceLabel();
		tableViewer.setInput(null);
		tableViewer.getTable().setLinesVisible(false);
	}

	@Override
	protected void disconnectView() {
	}

	@Override
	public void refresh() {
	}

	@Override
	protected boolean needsLinkWithEditorAndSelection() {
		return false;
	}

	protected Object getSelectedElement(ISelection selection) {
		Object selectedElement = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			if (structured.size() == 1) {
				selectedElement = structured.getFirstElement();
			}
		}
		return selectedElement;
	}

}
