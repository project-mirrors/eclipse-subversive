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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.ui.AbstractSVNView;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.operation.ScanLocksOperation;
import org.eclipse.team.svn.ui.operation.ScanLocksOperation.CreateLockResourcesHierarchyOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * SVN Lock View
 * 
 * Shows locks on files
 * 
 * @author Igor Burilo
 */
public class LocksView extends AbstractSVNView {

	public static final String VIEW_ID = LocksView.class.getName();

	protected LocksComposite locksComposite;

	protected Action linkWithEditorAction;

	protected Action linkWithEditorDropDownAction;

	public LocksView() {
		super(SVNUIMessages.LocksView_SVNLocks);
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		isLinkWithEditorEnabled = SVNTeamPreferences.getPropertiesBoolean(
				SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.LOCKS_LINK_WITH_EDITOR_NAME);

		locksComposite = new LocksComposite(parent);
		locksComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		refresh();

		createActionBars();

		//Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.locksViewContext"); //$NON-NLS-1$
	}

	protected void createActionBars() {
		//drop-down menu
		IActionBars actionBars = getViewSite().getActionBars();
		IMenuManager actionBarsMenu = actionBars.getMenuManager();

		linkWithEditorDropDownAction = new Action(SVNUIMessages.SVNView_LinkWith_Label, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				LocksView.this.linkWithEditor();
				linkWithEditorAction.setChecked(LocksView.this.isLinkWithEditorEnabled);
			}
		};
		linkWithEditorDropDownAction.setChecked(isLinkWithEditorEnabled);

		actionBarsMenu.add(linkWithEditorDropDownAction);

		IToolBarManager tbm = actionBars.getToolBarManager();
		tbm.removeAll();
		Action action = new Action(SVNUIMessages.SVNView_Refresh_Label) {
			@Override
			public void run() {
				LocksView.this.refresh();
			}
		};
		action.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/refresh.gif")); //$NON-NLS-1$
		tbm.add(action);
		tbm.add(getLinkWithEditorAction());

		tbm.update(true);

		getSite().getPage().addSelectionListener(selectionListener);
	}

	protected Action getLinkWithEditorAction() {
		linkWithEditorAction = new Action(SVNUIMessages.SVNView_LinkWith_Label, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				LocksView.this.linkWithEditor();
				linkWithEditorDropDownAction.setChecked(LocksView.this.isLinkWithEditorEnabled);
			}
		};
		linkWithEditorAction.setToolTipText(SVNUIMessages.SVNView_LinkWith_ToolTip);
		linkWithEditorAction.setDisabledImageDescriptor(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/link_with_disabled.gif")); //$NON-NLS-1$
		linkWithEditorAction
				.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/link_with.gif")); //$NON-NLS-1$

		linkWithEditorAction.setChecked(isLinkWithEditorEnabled);

		return linkWithEditorAction;
	}

	protected void linkWithEditor() {
		isLinkWithEditorEnabled = !isLinkWithEditorEnabled;
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		SVNTeamPreferences.setPropertiesBoolean(store, SVNTeamPreferences.LOCKS_LINK_WITH_EDITOR_NAME,
				isLinkWithEditorEnabled);
		if (isLinkWithEditorEnabled) {
			editorActivated(getSite().getPage().getActiveEditor());
		}
	}

	@Override
	protected void updateViewInput(IResource resource) {
		ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
		if (IStateFilter.SF_VERSIONED.accept(local)) {
			if (resource.equals(wcResource)) {
				return;
			}
			setResource(resource);
		}
	}

	public void setResourceWithoutActionExecution(IResource resource) {
		wcResource = resource;
		locksComposite.setResource(resource);
	}

	public void setResource(IResource resource) {
		setResourceWithoutActionExecution(resource);
		refresh();
	}

	public IActionOperation getUpdateViewOperation() {
		CompositeOperation op = null;
		if (wcResource != null) {
			ScanLocksOperation mainOp = new ScanLocksOperation(new IResource[] { wcResource });
			op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

			op.add(new AbstractActionOperation("", SVNUIMessages.class) { //$NON-NLS-1$
				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					//set pending
					locksComposite.setPending(true);
					LocksView.this.getSite().getShell().getDisplay().syncExec(() -> {
						LocksView.this.showResourceLabel();
						locksComposite.initializeComposite();
					});
				}
			});

			op.add(mainOp);
			/*
			 * As we don't want that scan locks operation to write in console, pass console stream as null.
			 * Scan locks operation writes only last notification in status, which is not useful info
			 * so we disable it.
			 */
			mainOp.setConsoleStream(null);

			final CreateLockResourcesHierarchyOperation createHierarchyOp = new CreateLockResourcesHierarchyOperation(
					mainOp);
			op.add(createHierarchyOp, new IActionOperation[] { mainOp });

			//update composite
			op.add(new AbstractActionOperation("", SVNUIMessages.class) { //$NON-NLS-1$
				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					locksComposite.setRootLockResource(createHierarchyOp.getLockResourceRoot());
					UIMonitorUtility.getDisplay().syncExec(() -> {
						locksComposite.setPending(false);
						locksComposite.initializeComposite();
					});
				}
			}, new IActionOperation[] { createHierarchyOp });
		}
		return op;
	}

	@Override
	public void refresh() {
		IActionOperation op = getUpdateViewOperation();
		if (op != null) {
			ProgressMonitorUtility.doTaskScheduled(op, false);
		}
	}

	@Override
	protected void disconnectView() {
		locksComposite.disconnectComposite();
		wcResource = null;
	}

	@Override
	protected boolean needsLinkWithEditorAndSelection() {
		return true;
	}

	@Override
	public void setFocus() {

	}

	public static LocksView instance() {
		final LocksView[] view = new LocksView[1];
		UIMonitorUtility.getDisplay().syncExec(() -> {
			IWorkbenchWindow window = SVNTeamUIPlugin.instance().getWorkbench().getActiveWorkbenchWindow();
			if (window != null && window.getActivePage() != null) {
				view[0] = (LocksView) window.getActivePage().findView(LocksView.VIEW_ID);
			}
		});
		return view[0];
	}
}
