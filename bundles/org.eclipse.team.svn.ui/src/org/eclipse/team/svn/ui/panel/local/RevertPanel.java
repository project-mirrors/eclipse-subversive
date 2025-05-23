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

package org.eclipse.team.svn.ui.panel.local;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.CreatePatchOperation;
import org.eclipse.team.svn.core.operation.local.ExportOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.management.CleanupOperation;
import org.eclipse.team.svn.core.operation.local.refactor.DeleteResourceOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.local.BranchTagAction;
import org.eclipse.team.svn.ui.action.local.CompareWithWorkingCopyAction;
import org.eclipse.team.svn.ui.action.local.ReplaceWithLatestRevisionAction;
import org.eclipse.team.svn.ui.action.local.ReplaceWithRevisionAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.dialog.DiscardConfirmationDialog;
import org.eclipse.team.svn.ui.operation.CompareResourcesOperation;
import org.eclipse.team.svn.ui.panel.participant.BasePaneParticipant;
import org.eclipse.team.svn.ui.panel.participant.RevertPaneParticipant;
import org.eclipse.team.svn.ui.panel.remote.ComparePanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.LockProposeUtility;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Revert resources state panel implementation
 * 
 * @author Alexander Gurov
 */
public class RevertPanel extends AbstractResourceSelectionPanel {

	protected boolean removeNonVersioned;

	protected boolean disableRemoveNonVersionedChange;

	protected IResourceStatesListener resourceStatesListener;

	public RevertPanel(IResource[] resources) {
		this(resources, null);
	}

	public RevertPanel(IResource[] resources, IResource[] userSelectedResources) {
		super(resources, userSelectedResources,
				new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL });
		dialogTitle = SVNUIMessages.RevertPanel_Title;

		boolean isParticipantPane = paneParticipantHelper.isParticipantPane();
		dialogDescription = isParticipantPane
				? SVNUIMessages.RevertPanel_Pane_Description
				: SVNUIMessages.RevertPanel_Description;
		defaultMessage = isParticipantPane ? SVNUIMessages.RevertPanel_Pane_Message : SVNUIMessages.RevertPanel_Message;
		IResource[] nonVersionedResources = FileUtility.getResourcesRecursive(resources, IStateFilter.SF_NEW,
				IResource.DEPTH_ZERO);
		disableRemoveNonVersionedChange = nonVersionedResources.length == resources.length;
		removeNonVersioned = disableRemoveNonVersionedChange;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		super.createControlsImpl(composite);

		createVerticalStrut(composite, 4);

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		separator.setLayoutData(data);

		createVerticalStrut(composite, 7);

		final Button removeNonVersionedButton = new Button(composite, SWT.CHECK);
		data = new GridData();
		removeNonVersionedButton.setLayoutData(data);
		removeNonVersionedButton.setText(SVNUIMessages.RevertPanel_Button_RemoveNonVersioned);
		removeNonVersionedButton.setSelection(removeNonVersioned);
		removeNonVersionedButton.setEnabled(!disableRemoveNonVersionedChange);
		removeNonVersionedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeNonVersioned = removeNonVersionedButton.getSelection();
			}
		});

		if (!paneParticipantHelper.isParticipantPane()) {
			addContextMenu();
		}
	}

	@Override
	public void postInit() {
		super.postInit();

		resourceStatesListener = RevertPanel.this::updateResources;
		SVNRemoteStorage.instance()
				.addResourceStatesListener(ResourceStatesChangedEvent.class, RevertPanel.this.resourceStatesListener);
	}

	@Override
	public void dispose() {
		super.dispose();

		SVNRemoteStorage.instance()
				.removeResourceStatesListener(ResourceStatesChangedEvent.class, resourceStatesListener);
	}

	@Override
	protected void addContextMenu() {
		final TableViewer tableViewer = selectionComposite.getTableViewer();
		MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(tableViewer.getTable());
		menuMgr.addMenuListener(manager -> {
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			final IStructuredSelection tSelection = (IStructuredSelection) tableViewer.getSelection();
			final IResource[] selectedResources = (IResource[]) tSelection.toList()
					.toArray(new IResource[tSelection.size()]);
			Action tAction = null;

			//Create Patch File action
			manager.add(tAction = new Action(SVNUIMessages.CreatePatchCommand_label) {
				@Override
				public void run() {
					FileDialog dlg = new FileDialog(UIMonitorUtility.getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
					dlg.setText(SVNUIMessages.SelectPatchFilePage_SavePatchAs);
					dlg.setFileName(selectedResources[0].getName() + ".patch"); //$NON-NLS-1$
					dlg.setFilterExtensions(new String[] { "patch", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
					String file = dlg.open();
					if (file != null) {
						CreatePatchOperation mainOp = new CreatePatchOperation(
								new IResource[] { selectedResources[0] }, file, true, true, true, true);
						UIMonitorUtility.doTaskNowDefault(mainOp, false);
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_VERSIONED, IResource.DEPTH_ZERO));

			//Create Branch action
			manager.add(tAction = new Action(SVNUIMessages.BranchAction_label) {
				@Override
				public void run() {
					IResource[] resources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_EXCLUDE_DELETED, IResource.DEPTH_INFINITE);
					IActionOperation op = BranchTagAction.getBranchTagOperation(UIMonitorUtility.getShell(),
							BranchTagAction.BRANCH_ACTION, resources);
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				}
			});
			tAction.setEnabled(tSelection.size() > 0 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_EXCLUDE_DELETED, IResource.DEPTH_ZERO));
			tAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/branch.gif")); //$NON-NLS-1$
			manager.add(new Separator());

			//Lock action
			manager.add(tAction = new Action(SVNUIMessages.LockAction_label) {
				@Override
				public void run() {
					IResource[] filteredResources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_READY_TO_LOCK, IResource.DEPTH_INFINITE);
					IActionOperation op = LockProposeUtility.performLockAction(filteredResources, false,
							UIMonitorUtility.getShell());
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, false);
					}
				}
			});
			tAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/lock.gif")); //$NON-NLS-1$
			tAction.setEnabled(FileUtility.checkForResourcesPresenceRecursive(selectedResources,
					IStateFilter.SF_READY_TO_LOCK));

			//Unlock action
			manager.add(tAction = new Action(SVNUIMessages.UnlockAction_label) {
				@Override
				public void run() {
					IResource[] filteredResources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_LOCKED, IResource.DEPTH_INFINITE);
					IActionOperation op = LockProposeUtility.performUnlockAction(filteredResources,
							UIMonitorUtility.getShell());
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, false);
					}
				}
			});
			tAction.setImageDescriptor(
					SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/actions/unlock.gif")); //$NON-NLS-1$
			tAction.setEnabled(
					FileUtility.checkForResourcesPresenceRecursive(selectedResources, IStateFilter.SF_LOCKED));
			manager.add(new Separator());

			//Compare With group
			MenuManager subMenu = new MenuManager(SVNUIMessages.CommitPanel_CompareWith_Group);
			subMenu.add(tAction = new Action(SVNUIMessages.CompareWithWorkingCopyAction_label) {
				@Override
				public void run() {
					IResource resource = selectedResources[0];
					ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
					if (!IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
						IRepositoryResource remote = local.isCopied()
								? SVNUtility.getCopiedFrom(resource)
								: SVNRemoteStorage.instance().asRepositoryResource(resource);
						remote.setSelectedRevision(SVNRevision.BASE);
						UIMonitorUtility
								.doTaskScheduledDefault(new CompareResourcesOperation(local, remote, false, true));
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1 && FileUtility.checkForResourcesPresence(selectedResources,
					CompareWithWorkingCopyAction.COMPARE_FILTER, IResource.DEPTH_ZERO));
			subMenu.add(tAction = new Action(SVNUIMessages.CompareWithLatestRevisionAction_label) {
				@Override
				public void run() {
					IResource resource = selectedResources[0];
					ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
					if (!IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
						IRepositoryResource remote = local.isCopied()
								? SVNUtility.getCopiedFrom(resource)
								: SVNRemoteStorage.instance().asRepositoryResource(resource);
						remote.setSelectedRevision(SVNRevision.HEAD);
						UIMonitorUtility
								.doTaskScheduledDefault(new CompareResourcesOperation(local, remote, false, true));
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1
					&& (CoreExtensionsManager.instance()
							.getSVNConnectorFactory()
							.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x
							|| selectedResources[0].getType() == IResource.FILE)
					&& FileUtility.checkForResourcesPresenceRecursive(selectedResources,
							CompareWithWorkingCopyAction.COMPARE_FILTER));
			subMenu.add(tAction = new Action(SVNUIMessages.CompareWithRevisionAction_label) {
				@Override
				public void run() {
					IResource resource = selectedResources[0];
					ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
					if (!IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
						IRepositoryResource remote = local.isCopied()
								? SVNUtility.getCopiedFrom(resource)
								: SVNRemoteStorage.instance().asRepositoryResource(resource);
						ComparePanel panel = new ComparePanel(remote, local.getRevision());
						DefaultDialog dlg = new DefaultDialog(UIMonitorUtility.getShell(), panel);
						if (dlg.open() == 0) {
							remote = panel.getSelectedResource();
							UIMonitorUtility.doTaskScheduledDefault(
									new CompareResourcesOperation(local, remote, false, true));
						}
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1
					&& (CoreExtensionsManager.instance()
							.getSVNConnectorFactory()
							.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x
							|| selectedResources[0].getType() == IResource.FILE)
					&& FileUtility.checkForResourcesPresenceRecursive(selectedResources,
							CompareWithWorkingCopyAction.COMPARE_FILTER));
			manager.add(subMenu);

			//Replace with group
			subMenu = new MenuManager(SVNUIMessages.CommitPanel_ReplaceWith_Group);
			subMenu.add(tAction = new Action(SVNUIMessages.ReplaceWithLatestRevisionAction_label) {
				@Override
				public void run() {
					IResource[] resources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_ONREPOSITORY, IResource.DEPTH_ZERO);
					IActionOperation op = ReplaceWithLatestRevisionAction.getReplaceOperation(resources,
							UIMonitorUtility.getShell());
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				}
			});
			tAction.setEnabled(FileUtility.checkForResourcesPresenceRecursive(selectedResources,
					IStateFilter.SF_ONREPOSITORY));
			subMenu.add(tAction = new Action(SVNUIMessages.ReplaceWithRevisionAction_label) {
				@Override
				public void run() {
					IActionOperation op = ReplaceWithRevisionAction.getReplaceOperation(selectedResources,
							UIMonitorUtility.getShell());
					if (op != null) {
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				}
			});
			tAction.setEnabled(tSelection.size() == 1 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_ONREPOSITORY, IResource.DEPTH_ZERO));
			manager.add(subMenu);
			manager.add(new Separator());

			//Export action
			manager.add(tAction = new Action(SVNUIMessages.ExportCommand_label) {
				@Override
				public void run() {
					DirectoryDialog fileDialog = new DirectoryDialog(UIMonitorUtility.getShell());
					fileDialog.setText(SVNUIMessages.ExportAction_Select_Title);
					fileDialog.setMessage(SVNUIMessages.ExportAction_Select_Description);
					String path = fileDialog.open();
					if (path != null) {
						boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
								SVNTeamUIPlugin.instance().getPreferenceStore(),
								SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
						UIMonitorUtility.doTaskScheduledDefault(new ExportOperation(
								FileUtility.getResourcesRecursive(selectedResources,
										IStateFilter.SF_EXCLUDE_DELETED, IResource.DEPTH_ZERO),
								path, SVNRevision.WORKING, ignoreExternals));
					}
				}
			});
			tAction.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/export.gif")); //$NON-NLS-1$
			tAction.setEnabled(tSelection.size() > 0 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_EXCLUDE_DELETED, IResource.DEPTH_ZERO));

			//Clean-up action
			manager.add(tAction = new Action(SVNUIMessages.CleanupCommand_label) {
				@Override
				public void run() {
					IResource[] resources = FileUtility.getResourcesRecursive(selectedResources,
							IStateFilter.SF_VERSIONED_FOLDERS, IResource.DEPTH_ZERO);
					CleanupOperation mainOp = new CleanupOperation(resources);
					CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
					op.add(mainOp);
					op.add(new RefreshResourcesOperation(resources));
					UIMonitorUtility.doTaskNowDefault(op, false);
				}
			});
			tAction.setEnabled(tSelection.size() > 0 && FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_VERSIONED_FOLDERS, IResource.DEPTH_ZERO));
			manager.add(new Separator());

			//Delete action
			manager.add(tAction = new Action(SVNUIMessages.CommitPanel_Delete_Action) {
				@Override
				public void run() {
					DiscardConfirmationDialog dialog = new DiscardConfirmationDialog(UIMonitorUtility.getShell(),
							selectedResources.length == 1, DiscardConfirmationDialog.MSG_RESOURCE);
					if (dialog.open() == 0) {
						DeleteResourceOperation deleteOperation = new DeleteResourceOperation(selectedResources);
						CompositeOperation op = new CompositeOperation(deleteOperation.getId(),
								deleteOperation.getMessagesClass());
						SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(selectedResources);
						RestoreProjectMetaOperation restoreOp = new RestoreProjectMetaOperation(saveOp);
						op.add(saveOp);
						op.add(deleteOperation);
						op.add(restoreOp);
						op.add(new RefreshResourcesOperation(selectedResources));
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				}
			});
			tAction.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/delete.gif")); //$NON-NLS-1$
			tAction.setEnabled(tSelection.size() > 0 && !FileUtility.checkForResourcesPresence(selectedResources,
					IStateFilter.SF_DELETED, IResource.DEPTH_ZERO));
		});
		menuMgr.setRemoveAllWhenShown(true);
		tableViewer.getTable().setMenu(menu);
	}

	protected void updateResources(ResourceStatesChangedEvent event) {
		HashSet<IResource> allResources = new HashSet<>(Arrays.asList(resources));

		HashSet<IResource> toDeleteSet = new HashSet<>(Arrays.asList(
				FileUtility.getResourcesRecursive(event.resources, IStateFilter.SF_NOTMODIFIED, IResource.DEPTH_ZERO)));
		toDeleteSet.addAll(Arrays.asList(
				FileUtility.getResourcesRecursive(event.resources, IStateFilter.SF_NOTEXISTS, IResource.DEPTH_ZERO)));
		toDeleteSet.addAll(Arrays.asList(
				FileUtility.getResourcesRecursive(event.resources, IStateFilter.SF_IGNORED, IResource.DEPTH_ZERO)));

		allResources.removeAll(toDeleteSet);

		final IResource[] newResources = allResources.toArray(new IResource[allResources.size()]);

		if (!paneParticipantHelper.isParticipantPane()) {
			UIMonitorUtility.getDisplay().syncExec(() -> {
				//FIXME isDisposed() test is necessary as dispose() method is not called from FastTrack Commit Dialog
				if (!RevertPanel.this.selectionComposite.isDisposed()) {
					RevertPanel.this.selectionComposite.setResources(newResources);
					RevertPanel.this.selectionComposite.fireSelectionChanged();
				}
			});
		}

		resources = newResources;
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.revertDialogContext"; //$NON-NLS-1$
	}

	public boolean getRemoveNonVersioned() {
		return removeNonVersioned;
	}

	protected void createVerticalStrut(Composite parent, int height) {
		Label strut = new Label(parent, SWT.NONE);
		GridData data = new GridData();
		data.heightHint = height;
		strut.setLayoutData(data);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.ui.panel.local.AbstractResourceSelectionPanel#createPaneParticipant()
	 */
	@Override
	protected BasePaneParticipant createPaneParticipant() {
		return new RevertPaneParticipant(new ResourceScope(resources), this);
	}

}
