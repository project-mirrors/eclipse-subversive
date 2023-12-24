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

package org.eclipse.team.svn.ui.repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.svnstorage.events.RepositoriesStateChangedEvent;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractSVNTeamAction;
import org.eclipse.team.svn.ui.action.local.management.NewRepositoryAction;
import org.eclipse.team.svn.ui.action.remote.DeleteAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileAction;
import org.eclipse.team.svn.ui.action.remote.OpenFileWithAction;
import org.eclipse.team.svn.ui.action.remote.RefreshAction;
import org.eclipse.team.svn.ui.action.remote.RenameAction;
import org.eclipse.team.svn.ui.action.remote.management.DiscardRepositoryLocationAction;
import org.eclipse.team.svn.ui.action.remote.management.DiscardRevisionLinksAction;
import org.eclipse.team.svn.ui.action.remote.management.EditRevisionLinkAction;
import org.eclipse.team.svn.ui.action.remote.management.NewRepositoryLocationAction;
import org.eclipse.team.svn.ui.action.remote.management.RefreshRepositoryLocationAction;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.repository.browser.RepositoryBrowser;
import org.eclipse.team.svn.ui.repository.model.RepositoriesRoot;
import org.eclipse.team.svn.ui.repository.model.RepositoryContentProvider;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

/**
 * Repository view implementation
 * 
 * @author Alexander Gurov
 */
public class RepositoriesView extends ViewPart {
	public static final String VIEW_ID = RepositoriesView.class.getName();

	protected RepositoryTreeViewer repositoryTree;

	protected RepositoriesRoot root;

	protected DrillDownAdapter ddAdapter;

	protected Action showBrowserAction;

	protected IPartListener2 partListener;

	protected IPropertyChangeListener prefsPropertyListener;

	public RepositoriesView() {
	}

	public static MenuManager newMenuInstance(final ISelectionProvider provider) {
		MenuManager menuMgr = new MenuManager();
		menuMgr.addMenuListener(manager -> {
			MenuManager sub = new MenuManager(SVNUIMessages.RepositoriesView_New, "addMenu"); //$NON-NLS-1$
			sub.add(new Separator("mainGroup")); //$NON-NLS-1$
			sub.add(new Separator("managementGroup")); //$NON-NLS-1$
			sub.add(new Separator("repositoryGroup")); //$NON-NLS-1$
			sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

			//new repository location
			Action newRepositoryLocation = new Action(SVNUIMessages.RepositoriesView_RepositoryLocation) {
				@Override
				public void run() {
					new NewRepositoryLocationAction().run(this);
				}
			};
			newRepositoryLocation.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/objects/repository.gif")); //$NON-NLS-1$
			sub.add(newRepositoryLocation);
			manager.add(sub);

			//new repository
			Action newRepository = new Action(SVNUIMessages.RepositoriesView_Repository) {
				@Override
				public void run() {
					new NewRepositoryAction().run(this);
				}

				@Override
				public boolean isEnabled() {
					return NewRepositoryAction.checkEnablement();
				}
			};
			newRepository.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/repositories/new_repository.gif")); //$NON-NLS-1$
			sub.add(newRepository);
			manager.add(sub);

			manager.add(new Separator("checkoutGroup")); //$NON-NLS-1$

			sub = new MenuManager(SVNUIMessages.RepositoriesView_OpenWith, "openWithMenu"); //$NON-NLS-1$
			sub.add(new Separator("dynamicGroup")); //$NON-NLS-1$
			IStructuredSelection selection = (IStructuredSelection) provider.getSelection();
			if (selection.size() == 1) {
				Object item = selection.getFirstElement();
				if (item instanceof RepositoryFile) {
					String name = ((RepositoryFile) item).getRepositoryResource().getName();
					IEditorDescriptor[] editors = SVNTeamUIPlugin.instance()
							.getWorkbench()
							.getEditorRegistry()
							.getEditors(name);
					for (IEditorDescriptor editor : editors) {
						if (!editor.getId().equals(EditorsUI.DEFAULT_TEXT_EDITOR_ID)) {
							final OpenFileWithAction openAction = new OpenFileWithAction(editor.getId(), false);
							Action wrapper = new Action(editor.getLabel()) {
								@Override
								public void run() {
									openAction.run(this);
								}
							};
							openAction.selectionChanged(wrapper, selection);
							sub.add(wrapper);
						}
					}
				}
			}
			sub.add(new Separator("fixedGroup")); //$NON-NLS-1$
			sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			manager.add(sub);

			manager.add(new Separator("miscGroup")); //$NON-NLS-1$

			sub = new MenuManager(SVNUIMessages.RepositoriesView_Refactor, "refactorMenu"); //$NON-NLS-1$
			sub.add(new Separator("mainGroup")); //$NON-NLS-1$
			sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			manager.add(sub);

			manager.add(new Separator("locationGroup")); //$NON-NLS-1$

			manager.add(new Separator("propertiesGroup")); //$NON-NLS-1$

			manager.add(new Separator("importExportGroup")); //$NON-NLS-1$

			sub = new MenuManager(SVNUIMessages.RepositoriesView_CompareWith, "compareMenu"); //$NON-NLS-1$
			sub.add(new Separator("mainGroup")); //$NON-NLS-1$
			sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			manager.prependToGroup("importExportGroup", sub); //$NON-NLS-1$

			manager.add(new Separator("refreshGroup")); //$NON-NLS-1$

			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		});
		menuMgr.setRemoveAllWhenShown(true);
		return menuMgr;
	}

	@Override
	public void createPartControl(Composite parent) {
		repositoryTree = new RepositoryTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		repositoryTree.setContentProvider(new RepositoryContentProvider(repositoryTree));
		repositoryTree.setLabelProvider(new WorkbenchLabelProvider());
		getSite().setSelectionProvider(repositoryTree);
		repositoryTree.setInput(root = new RepositoriesRoot());
		//this.repositoryTree.setSorter(new ViewSorter())

		ddAdapter = new DrillDownAdapter(repositoryTree);

		// popup menu
		Tree tree = repositoryTree.getTree();
		MenuManager menuMgr = RepositoriesView.newMenuInstance(repositoryTree);
		tree.setMenu(menuMgr.createContextMenu(tree));
		getSite().registerContextMenu(menuMgr, repositoryTree);

		// toolbar
		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager tbm = actionBars.getToolBarManager();
		ddAdapter.addNavigationActions(tbm);
		Action tAction = null;
		tbm.add(tAction = new Action(SVNUIMessages.SVNView_Refresh_Label) {
			@Override
			public void run() {
				if (repositoryTree.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) repositoryTree.getSelection();
					RepositoriesView.this.handleRefresh(selection);
				}
			}
		});
		tAction.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/refresh.gif")); //$NON-NLS-1$
		tAction.setToolTipText(SVNUIMessages.SVNView_Refresh_ToolTip);

		tbm.add(new Separator("collapseAllGroup")); //$NON-NLS-1$

		tbm.add(tAction = new Action(SVNUIMessages.RepositoriesView_CollapseAll_Label) {
			@Override
			public void run() {
				repositoryTree.collapseAll();
			}
		});
		tAction.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/collapseall.gif")); //$NON-NLS-1$
		tAction.setToolTipText(SVNUIMessages.RepositoriesView_CollapseAll_ToolTip);

		tbm.add(new Separator("repositoryGroup")); //$NON-NLS-1$

		tbm.add(tAction = new Action(SVNUIMessages.RepositoriesView_NewLocation_Label) {
			@Override
			public void run() {
				new NewRepositoryLocationAction().run(this);
			}
		});
		tAction.setImageDescriptor(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/repositories/new_location.gif")); //$NON-NLS-1$
		tAction.setToolTipText(SVNUIMessages.RepositoriesView_NewLocation_ToolTip);

		//new repositoy action
		final Action tNewRepositoryAction = new Action(SVNUIMessages.RepositoriesView_NewRepository_Label) {
			@Override
			public void run() {
				new NewRepositoryAction().run(this);
			}
		};
		tbm.add(tNewRepositoryAction);
		tNewRepositoryAction.setImageDescriptor(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/repositories/new_repository.gif")); //$NON-NLS-1$
		tNewRepositoryAction.setToolTipText(SVNUIMessages.RepositoriesView_NewRepository_ToolTip);
		tNewRepositoryAction.setEnabled(NewRepositoryAction.checkEnablement());
		//add connector changes listener to track action enablement
		prefsPropertyListener = event -> {
			if (SVNTeamPreferences.fullCoreName(SVNTeamPreferences.CORE_SVNCONNECTOR_NAME)
					.equals(event.getProperty())) {
				tNewRepositoryAction.setEnabled(NewRepositoryAction.checkEnablement());
			}
		};
		SVNTeamUIPlugin.instance().getPreferenceStore().addPropertyChangeListener(prefsPropertyListener);

		tbm.add(showBrowserAction = new Action(SVNUIMessages.RepositoriesView_ShowBrowser_Label, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				if (isChecked()) {
					RepositoriesView.this.showRepositoryBrowser(true);
				} else {
					RepositoriesView.this.hideRepositoryBrowser();
				}
			}
		});
		showBrowserAction.setImageDescriptor(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/repositories/browser.gif")); //$NON-NLS-1$
		showBrowserAction.setToolTipText(SVNUIMessages.RepositoriesView_ShowBrowser_ToolTip);

		repositoryTree.getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (repositoryTree.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) repositoryTree.getSelection();
					if (event.keyCode == SWT.F5) {
						RepositoriesView.this.handleRefresh(selection);
					} else if (event.keyCode == SWT.DEL) {
						RepositoriesView.this.handleDeleteKey(selection);
					} else if (event.keyCode == SWT.F2) {
						RepositoriesView.this.handleEdit(selection);
					}
				}
			}
		});

		repositoryTree.addDoubleClickListener(e -> {
			ISelection selection = e.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structured = (IStructuredSelection) selection;
				if (structured.size() == 1) {
					RepositoriesView.this.handleDoubleClick(structured);
				}
			}
		});

		partListener = new IPartListener2() {
			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals(RepositoriesView.VIEW_ID)) {
					RepositoriesView.this.refreshRepositoriesImpl(false);
				}
			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
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
					RepositoriesView.this.getViewSite().getPage().removePartListener(this);
				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
			}
		};

		getViewSite().getPage().addPartListener(partListener);

		SVNRemoteStorage.instance().addRepositoriesStateChangedListener(event -> {
			if (event.getAction() == RepositoriesStateChangedEvent.ADDED) {
				RepositoriesView.refreshRepositories(false);
			}
		});

		//Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.repositoryViewContext"); //$NON-NLS-1$
	}

	@Override
	public void dispose() {
		getViewSite().getPage().removePartListener(partListener);
		if (prefsPropertyListener != null) {
			SVNTeamUIPlugin.instance().getPreferenceStore().removePropertyChangeListener(prefsPropertyListener);
		}
		super.dispose();
	}

	@Override
	public void setFocus() {
		repositoryTree.getControl().setFocus();
	}

	public static void refresh(Object where) {
		RepositoriesView.refresh(where, null);
	}

	public static void refresh(Object where, RepositoryTreeViewer.IRefreshVisitor visitor) {
		RepositoriesView instance = RepositoriesView.instance();
		if (instance != null) {
			instance.repositoryTree.refresh(where, visitor, false);
		}
	}

	public static void refreshRepositories(boolean deep) {
		RepositoriesView instance = RepositoriesView.instance();
		if (instance != null) {
			instance.refreshRepositoriesImpl(deep);
		}
	}

	public RepositoryTreeViewer getRepositoryTree() {
		return repositoryTree;
	}

	public void refreshButtonsState() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		boolean isBrowserVisible = SVNTeamPreferences.getRepositoryBoolean(store,
				SVNTeamPreferences.REPOSITORY_SHOW_BROWSER_NAME);
		showBrowserAction.setChecked(isBrowserVisible);
	}

	public static RepositoriesView instance() {
		final RepositoriesView[] view = new RepositoriesView[1];
		UIMonitorUtility.getDisplay().syncExec(() -> {
			IWorkbenchWindow window = SVNTeamUIPlugin.instance().getWorkbench().getActiveWorkbenchWindow();
			if (window != null && window.getActivePage() != null) {
				view[0] = (RepositoriesView) window.getActivePage().findView(RepositoriesView.VIEW_ID);
			}
		});
		return view[0];
	}

	protected void refreshRepositoriesImpl(boolean deep) {
		if (deep) {
			root.refresh();
		} else {
			root.softRefresh();
		}
		repositoryTree.refresh();
	}

	protected void showRepositoryBrowser(final boolean force) {
		final IWorkbenchPage page = getSite().getPage();
		UIMonitorUtility.doTaskBusyDefault(new AbstractActionOperation("Operation_ShowBrowser", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				RepositoryBrowser browser = (RepositoryBrowser) page.showView(RepositoryBrowser.VIEW_ID);
				ISelection selection = repositoryTree.getSelection();
				browser.selectionChanged(new SelectionChangedEvent(repositoryTree, selection));
			}
		});
	}

	protected void hideRepositoryBrowser() {
		IWorkbenchPage page = getSite().getPage();
		IViewPart part = page.findView(RepositoryBrowser.VIEW_ID);
		if (part != null) {
			page.hideView(part);
		}
	}

	protected void handleRefresh(IStructuredSelection selection) {
		Action tmp = new Action() {
		};
		AbstractSVNTeamAction action = null;

		if (selection.isEmpty()) {
			action = new RefreshRepositoryLocationAction();
			action.selectionChanged(tmp, selection);
			action.setActivePart(tmp, RepositoriesView.this);
			action.run(tmp);
		} else {
			action = new RefreshAction();
			action.selectionChanged(tmp, selection);
			action.setActivePart(tmp, RepositoriesView.this);
			if (tmp.isEnabled()) {
				action.run(tmp);
			}
		}
	}

	protected void handleDeleteKey(IStructuredSelection selection) {
		Action tmp = new Action() {
		};
		AbstractSVNTeamAction action = new DeleteAction();
		action.selectionChanged(tmp, selection);
		action.setActivePart(tmp, RepositoriesView.this);
		if (tmp.isEnabled()) {
			action.run(tmp);
		} else {
			action = new DiscardRevisionLinksAction();
			action.selectionChanged(tmp, selection);
			action.setActivePart(tmp, RepositoriesView.this);
			if (tmp.isEnabled()) {
				action.run(tmp);
			} else {
				action = new DiscardRepositoryLocationAction();
				action.selectionChanged(tmp, selection);
				action.setActivePart(tmp, RepositoriesView.this);
				if (tmp.isEnabled()) {
					action.run(tmp);
				}
			}
		}
	}

	protected void handleEdit(IStructuredSelection selection) {
		Action tmp = new Action() {
		};
		AbstractSVNTeamAction action = new RenameAction();
		action.selectionChanged(tmp, selection);
		action.setActivePart(tmp, this);
		if (tmp.isEnabled()) {
			action.run(tmp);
		} else {
			action = new EditRevisionLinkAction();
			action.selectionChanged(tmp, selection);
			action.setActivePart(tmp, this);
			if (tmp.isEnabled()) {
				action.run(tmp);
			}
		}
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		Action tmp = new Action() {
		};
		AbstractSVNTeamAction action = new OpenFileAction();
		action.selectionChanged(tmp, selection);
		action.setActivePart(tmp, RepositoriesView.this);
		if (tmp.isEnabled()) {
			action.run(tmp);
		}
	}

	public boolean canGoBack() {
		return ddAdapter.canGoBack();
	}

	public void goBack() {
		ddAdapter.goBack();
	}

}
