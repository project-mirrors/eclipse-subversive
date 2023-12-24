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

package org.eclipse.team.svn.ui.lock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.lock.LockResource.LockStatusEnum;
import org.eclipse.team.svn.ui.utility.LockProposeUtility;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * SVN Locks composite
 * 
 * @author Igor Burilo
 */
public class LocksComposite extends Composite {

	protected boolean isProcessing;

	protected IResource resource;

	protected LockResource rootLockResource;

	protected LockResourceSelectionComposite tableViewer;

	protected TreeViewer treeViewer;

	protected Text commentText;

	protected LockResourcesTreeLabelProvider labelProvider;

	public LocksComposite(Composite parent) {
		super(parent, SWT.NONE);
		isProcessing = false;

		createControls(parent);
	}

	private void createControls(Composite parent) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = gridLayout.marginWidth = 0;
		setLayout(gridLayout);

		SashForm outerSashForm = new SashForm(this, SWT.VERTICAL);
		outerSashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		SashForm innerSashForm = new SashForm(outerSashForm, SWT.HORIZONTAL);
		innerSashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		createResourcesTree(innerSashForm);
		createResourcesTable(innerSashForm);
		innerSashForm.setWeights(new int[] { 25, 75 });

		createCommentComposite(outerSashForm);
		outerSashForm.setWeights(new int[] { 70, 30 });
	}

	protected void createCommentComposite(Composite parent) {
		commentText = new Text(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		commentText.setBackground(commentText.getBackground());
		commentText.setEditable(false);
		GridData data = new GridData(GridData.FILL_BOTH);
		commentText.setLayoutData(data);
	}

	protected void createResourcesTable(Composite parent) {
		tableViewer = new LockResourceSelectionComposite(parent, SWT.NONE, false, false);
		tableViewer.addResourcesSelectionChangedListener(event -> {
			if (event.selection != null && !event.selection.isEmpty()) {
				LockResource lockResource = (LockResource) event.selection.getFirstElement();
				if (!LockResourcesTableLabelProvider.isFakeLockResource(lockResource)) {
					commentText.setText(lockResource.getComment() == null || lockResource.getComment().length() == 0
							? SVNMessages.SVNInfo_NoComment
							: lockResource.getComment());
				} else {
					commentText.setText(""); //$NON-NLS-1$
				}
			}
		});

		tableViewer.setMenuManager(createResourcesTableMenu());
	}

	protected MenuManager createResourcesTreeMenu() {
		MenuManager menuMgr = new MenuManager();
		menuMgr.addMenuListener(manager -> {
			//ignore fake resource
			IStructuredSelection tSelection = (IStructuredSelection) treeViewer.getSelection();
			if (tSelection.size() == 1) {
				LockResource lockResource = (LockResource) tSelection.getFirstElement();
				if (LockResourcesTableLabelProvider.isFakeLockResource(lockResource)) {
					return;
				}
			}

			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

			Map<LockStatusEnum, List<LockResource>> resourcesMap = new HashMap<>();
			LockResource lockResource = (LockResource) tSelection.getFirstElement();
			LockResource[] children = lockResource.getAllChildFiles();
			for (LockResource child : children) {
				List<LockResource> resourcesList = resourcesMap.get(child.getLockStatus());
				if (resourcesList == null) {
					resourcesList = new ArrayList<>();
					resourcesMap.put(child.getLockStatus(), resourcesList);
				}
				resourcesList.add(child);
			}

			manager.add(LocksComposite.this.createLockAction(resourcesMap));
			manager.add(LocksComposite.this.createUnlockAction(resourcesMap));
			manager.add(LocksComposite.this.createBreakLockAction(resourcesMap));
		});
		menuMgr.setRemoveAllWhenShown(true);
		return menuMgr;
	}

	protected MenuManager createResourcesTableMenu() {
		MenuManager menuMgr = new MenuManager();
		menuMgr.addMenuListener(manager -> {
			//ignore fake resource
			IStructuredSelection tSelection = (IStructuredSelection) tableViewer.getTableViewer().getSelection();
			if (tSelection.size() == 1) {
				LockResource lockResource = (LockResource) tSelection.getFirstElement();
				if (LockResourcesTableLabelProvider.isFakeLockResource(lockResource)) {
					return;
				}
			}

			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

			Map<LockStatusEnum, List<LockResource>> resourcesMap = new HashMap<>();
			Iterator iter = tSelection.iterator();
			while (iter.hasNext()) {
				LockResource lockResource = (LockResource) iter.next();
				List<LockResource> resourcesList = resourcesMap.get(lockResource.getLockStatus());
				if (resourcesList == null) {
					resourcesList = new ArrayList<>();
					resourcesMap.put(lockResource.getLockStatus(), resourcesList);
				}
				resourcesList.add(lockResource);
			}

			manager.add(LocksComposite.this.createLockAction(resourcesMap));
			manager.add(LocksComposite.this.createUnlockAction(resourcesMap));
			manager.add(LocksComposite.this.createBreakLockAction(resourcesMap));
		});
		menuMgr.setRemoveAllWhenShown(true);
		return menuMgr;
	}

	protected Action createLockAction(final Map<LockStatusEnum, List<LockResource>> resourcesMap) {
		final List<LockResource> lockResources = new ArrayList<>();
		if (resourcesMap.containsKey(LockStatusEnum.BROKEN)) {
			lockResources.addAll(resourcesMap.get(LockStatusEnum.BROKEN));
		}
		if (resourcesMap.containsKey(LockStatusEnum.STOLEN)) {
			lockResources.addAll(resourcesMap.get(LockStatusEnum.STOLEN));
		}
		if (resourcesMap.containsKey(LockStatusEnum.OTHER_LOCKED)) {
			lockResources.addAll(resourcesMap.get(LockStatusEnum.OTHER_LOCKED));
		}
		if (lockResources != null && !lockResources.isEmpty()) {
			//handle if resource doesn't exist locally
			Iterator<LockResource> iter = lockResources.iterator();
			while (iter.hasNext()) {
				LockResource lockResource = iter.next();
				IResource resource = (IResource) lockResource.getAdapter(IResource.class);
				if (!IStateFilter.SF_VERSIONED.accept(SVNRemoteStorage.instance().asLocalResource(resource))) {
					iter.remove();
				}
			}
		}

		Action action = new Action(SVNUIMessages.LockAction_label) {
			@Override
			public void run() {
				CompositeOperation op = LockProposeUtility.performLockAction(lockResources.toArray(new LockResource[0]),
						true, LocksComposite.this.getShell());
				if (op != null) {
					UIMonitorUtility.doTaskScheduledDefault(LocksView.instance(), op);
				}
			}
		};
		action.setEnabled(!lockResources.isEmpty());
		action.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/lock.gif")); //$NON-NLS-1$
		return action;
	}

	protected Action createBreakLockAction(final Map<LockStatusEnum, List<LockResource>> resourcesMap) {
		Action action = new Action(SVNUIMessages.BreakLockAction_label2) {
			@Override
			public void run() {
				List<LockResource> lockResources = new ArrayList<>();
				if (resourcesMap.containsKey(LockStatusEnum.OTHER_LOCKED)) {
					lockResources.addAll(resourcesMap.get(LockStatusEnum.OTHER_LOCKED));
				}
				if (resourcesMap.containsKey(LockStatusEnum.STOLEN)) {
					lockResources.addAll(resourcesMap.get(LockStatusEnum.STOLEN));
				}
				if (lockResources != null && !lockResources.isEmpty()) {
					CompositeOperation op = LockProposeUtility.performBreakLockAction(
							lockResources.toArray(new LockResource[0]), LocksComposite.this.getShell());
					if (op != null) {
						op.add(new AbstractActionOperation("", SVNUIMessages.class) { //$NON-NLS-1$
							@Override
							protected void runImpl(IProgressMonitor monitor) throws Exception {
								LocksView.instance().refresh();
							}
						});
						UIMonitorUtility.doTaskScheduledDefault(LocksView.instance(), op);
					}
				}
			}
		};
		action.setEnabled(resourcesMap.containsKey(LockStatusEnum.OTHER_LOCKED)
				|| resourcesMap.containsKey(LockStatusEnum.STOLEN));
		return action;
	}

	protected Action createUnlockAction(final Map<LockStatusEnum, List<LockResource>> resourcesMap) {
		Action action = new Action(SVNUIMessages.UnlockAction_label) {
			@Override
			public void run() {
				List<LockResource> lockResources = resourcesMap.get(LockStatusEnum.LOCALLY_LOCKED);
				if (lockResources != null && !lockResources.isEmpty()) {
					CompositeOperation op = LockProposeUtility.performUnlockAction(
							lockResources.toArray(new LockResource[0]), LocksComposite.this.getShell());
					if (op != null) {
						UIMonitorUtility.doTaskScheduledDefault(LocksView.instance(), op);
					}
				}
			}
		};
		action.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/unlock.gif")); //$NON-NLS-1$
		action.setEnabled(resourcesMap.containsKey(LockStatusEnum.LOCALLY_LOCKED));
		return action;
	}

	protected void createResourcesTree(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer.setContentProvider(new LockResourcesTreeContentProvider());
		treeViewer.setLabelProvider(labelProvider = new LockResourcesTreeLabelProvider());
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			protected LockResource oldSelection;

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection tSelection = (IStructuredSelection) event.getSelection();
				if (tSelection.size() > 0) {
					LockResource selection = (LockResource) tSelection.getFirstElement();
					if (oldSelection != selection) {
						tableViewer.setInput(selection.getAllChildFiles());
						oldSelection = selection;
					}
				} else {
					tableViewer.setInput(null);
				}
			}
		});

		MenuManager menuManager = createResourcesTreeMenu();
		Menu menu = menuManager.createContextMenu(treeViewer.getTree());
		treeViewer.getTree().setMenu(menu);
	}

	public void initializeComposite() {
		if (isProcessing) {
			treeViewer.setInput(null);
			tableViewer.setInput(new LockResource[] { LockResourcesTableLabelProvider.FAKE_PENDING });
			tableViewer.getTableViewer().getTable().setLinesVisible(false);
			commentText.setText(""); //$NON-NLS-1$
		} else {
			((LockResourcesTreeContentProvider) treeViewer.getContentProvider()).initialize(rootLockResource);
			if (rootLockResource != null) {
				treeViewer.setInput(SVNUIMessages.LocksComposite_Root);
				treeViewer.expandAll();
				treeViewer.setSelection(new StructuredSelection(rootLockResource));
				((Tree) treeViewer.getControl()).showSelection();
				tableViewer.getTableViewer().getTable().setLinesVisible(true);
			} else {
				treeViewer.setInput(null);
				tableViewer.setInput(new LockResource[] { LockResourcesTableLabelProvider.FAKE_NO_LOCKS });
				tableViewer.getTableViewer().getTable().setLinesVisible(false);
				commentText.setText(""); //$NON-NLS-1$
			}
		}
	}

	public void setPending(boolean isProcessing) {
		this.isProcessing = isProcessing;
	}

	public boolean isPending() {
		return isProcessing;
	}

	public synchronized void setResource(IResource resource) {
		this.resource = resource;
	}

	public void setRootLockResource(LockResource rootLockResource) {
		this.rootLockResource = rootLockResource;
	}

	public synchronized void disconnectComposite() {
		resource = null;
		rootLockResource = null;
	}

}
