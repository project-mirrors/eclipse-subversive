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

package org.eclipse.team.svn.ui.properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.core.operation.remote.GetRemotePropertiesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.AbstractSVNView;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.PropertiesComposite;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;

/**
 * Properties view
 *
 * @author Alexander Gurov
 */
public class PropertiesView extends AbstractSVNView {

	public static final String VIEW_ID = PropertiesView.class.getName();

	protected PropertiesComposite propertiesComposite;

	protected IResourcePropertyProvider propertyProvider;

	protected IAdaptable adaptable;

	protected Action linkWithEditorAction;

	protected Action linkWithEditorDropDownAction;

	protected boolean backgroundExecution;

	public PropertiesView() {
		super(SVNUIMessages.PropertiesView_Description);
	}

	@Override
	public void setFocus() {
	}

	public void setResource(IAdaptable resource, IResourcePropertyProvider propertyProvider,
			boolean backgroundExecution) {
		if (resource instanceof IRepositoryResource) {
			repositoryResource = (IRepositoryResource) resource;
			wcResource = null;
		} else if (resource instanceof IResource) {
			wcResource = (IResource) resource;
			repositoryResource = null;
		}
		adaptable = resource;
		this.propertyProvider = propertyProvider;
		this.backgroundExecution = backgroundExecution;

		propertiesComposite.setResource(resource, propertyProvider);
		refresh();
	}

	@Override
	public void refresh() {
		boolean operationToFollow = propertyProvider != null && propertyProvider.getExecutionState() != IStatus.OK;
		propertiesComposite.setPending(operationToFollow);
		getSite().getShell().getDisplay().syncExec(() -> {
			PropertiesView.this.showResourceLabel();
			propertiesComposite.initializeComposite();
		});
		CompositeOperation composite = new CompositeOperation("Operation_ShowProperties", SVNUIMessages.class); //$NON-NLS-1$
		if (propertyProvider != null && propertyProvider.getExecutionState() != IStatus.OK) {
			composite.add(propertyProvider);
			composite.add(propertiesComposite.getRefreshViewOperation(), new IActionOperation[] { propertyProvider });
		} else {
			composite.add(propertiesComposite.getRefreshViewOperation());
		}

		if (backgroundExecution) {
			UIMonitorUtility.doTaskScheduledDefault(composite);
		} else {
			UIMonitorUtility.doTaskScheduledDefault(this, composite);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		isLinkWithEditorEnabled = SVNTeamPreferences.getPropertiesBoolean(store,
				SVNTeamPreferences.PROPERTY_LINK_WITH_EDITOR_NAME);
		propertiesComposite = new PropertiesComposite(parent);
		propertiesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		propertiesComposite.setPropertiesView(this);
		refresh();

		//drop-down menu
		IActionBars actionBars = getViewSite().getActionBars();
		IMenuManager actionBarsMenu = actionBars.getMenuManager();

		linkWithEditorDropDownAction = new Action(SVNUIMessages.SVNView_LinkWith_Label, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				PropertiesView.this.linkWithEditor();
				linkWithEditorAction.setChecked(PropertiesView.this.isLinkWithEditorEnabled);
			}
		};
		linkWithEditorDropDownAction.setChecked(isLinkWithEditorEnabled);

		actionBarsMenu.add(linkWithEditorDropDownAction);

		IToolBarManager tbm = actionBars.getToolBarManager();
		tbm.removeAll();
		Action action = new Action(SVNUIMessages.SVNView_Refresh_Label) {
			@Override
			public void run() {
				PropertiesView.this.refreshAction();
			}
		};
		action.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/refresh.gif")); //$NON-NLS-1$
		tbm.add(action);
		tbm.add(getLinkWithEditorAction());

		tbm.update(true);

		getSite().getPage().addSelectionListener(selectionListener);

		//Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.propertiesViewContext"); //$NON-NLS-1$
	}

	@Override
	protected void disconnectView() {
		propertiesComposite.disconnectComposite();
		wcResource = null;
	}

	protected void refreshAction() {
		if (PropertiesView.this.repositoryResource != null) {
			PropertiesView.this.propertyProvider = new GetRemotePropertiesOperation(
					PropertiesView.this.repositoryResource);
			PropertiesView.this.propertiesComposite.setResource(PropertiesView.this.adaptable,
					PropertiesView.this.propertyProvider);
		}
		PropertiesView.this.refresh();
	}

	protected Action getLinkWithEditorAction() {
		linkWithEditorAction = new Action(SVNUIMessages.SVNView_LinkWith_Label, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				PropertiesView.this.linkWithEditor();
				linkWithEditorDropDownAction.setChecked(PropertiesView.this.isLinkWithEditorEnabled);
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
		SVNTeamPreferences.setPropertiesBoolean(store, SVNTeamPreferences.PROPERTY_LINK_WITH_EDITOR_NAME,
				isLinkWithEditorEnabled);
		if (isLinkWithEditorEnabled) {
			editorActivated(getSite().getPage().getActiveEditor());
		}
	}

	@Override
	protected void updateViewInput(IRepositoryResource resource) {
		if (repositoryResource != null && repositoryResource.equals(resource)) {
			return;
		}
		if (resource != null) {
			setResource(resource, new GetRemotePropertiesOperation(resource), true);
		}
	}

	@Override
	protected void updateViewInput(IResource resource) {
		ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
		if (IStateFilter.SF_EXCLUDE_PREREPLACED_AND_DELETED.accept(local)) {
			if (local.getResource().equals(wcResource)) {
				return;
			}
			setResource(resource, new GetPropertiesOperation(resource), true);
		}
	}

	@Override
	protected boolean needsLinkWithEditorAndSelection() {
		return true;
	}

}
