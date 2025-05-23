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
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNIgnoreOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.refactor.DeleteResourceOperation;
import org.eclipse.team.svn.core.resource.ISVNStorage;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.ResourcesParentsProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.local.AddToSVNIgnoreAction;
import org.eclipse.team.svn.ui.dialog.DiscardConfirmationDialog;
import org.eclipse.team.svn.ui.panel.participant.AddToSVNPaneParticipant;
import org.eclipse.team.svn.ui.panel.participant.BasePaneParticipant;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Add resources panel implementation
 * 
 * @author Alexander Gurov
 */
public class AddToSVNPanel extends AbstractResourceSelectionPanel {

	protected boolean actionTookEffect;

	protected IResourceStatesListener resourceStatesListener;

	public AddToSVNPanel(IResource[] resources) {
		this(resources, null);
	}

	public AddToSVNPanel(IResource[] resources, IResource[] userSelectedResources) {
		super(resources, userSelectedResources,
				new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL });
		actionTookEffect = false;
		dialogTitle = SVNUIMessages.AddToSVNPanel_Title;

		boolean isParticipantPane = paneParticipantHelper.isParticipantPane();

		dialogDescription = isParticipantPane
				? SVNUIMessages.AddToSVNPanel_Pane_Description
				: SVNUIMessages.AddToSVNPanel_Description;
		if (resources.length == 1) {
			defaultMessage = isParticipantPane
					? SVNUIMessages.AddToSVNPanel_Pane_Message_Single
					: SVNUIMessages.AddToSVNPanel_Message_Single;
		} else {
			String defaultMessage = isParticipantPane
					? SVNUIMessages.AddToSVNPanel_Pane_Message_Multi
					: SVNUIMessages.AddToSVNPanel_Message_Multi;
			this.defaultMessage = BaseMessages.format(defaultMessage,
					new String[] { String.valueOf(resources.length) });
		}
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
			if (selectedResources.length == 1) {
				manager.add(tAction = new Action(BaseMessages.format(SVNUIMessages.AddToSVNPanel_Ignore_Single,
						new String[] { selectedResources[0].getName() })) {
					@Override
					public void run() {
						CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", //$NON-NLS-1$
								SVNMessages.class);
						op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_NAME, null));
						op.add(new RefreshResourcesOperation(new ResourcesParentsProvider(selectedResources),
								IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				});
				tAction.setEnabled(FileUtility.checkForResourcesPresence(selectedResources,
						AddToSVNIgnoreAction.SF_NEW_AND_PARENT_VERSIONED, IResource.DEPTH_ZERO));
				String name = selectedResources[0].getName();
				String[] parts = name.split("\\."); //$NON-NLS-1$
				if (parts.length != 0) {
					manager.add(tAction = new Action(BaseMessages.format(SVNUIMessages.AddToSVNPanel_Ignore_Single,
							new String[] { "*." + parts[parts.length - 1] })) { //$NON-NLS-1$
						@Override
						public void run() {
							CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", //$NON-NLS-1$
									SVNMessages.class);
							op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_EXTENSION,
									null));
							op.add(new RefreshResourcesOperation(new ResourcesParentsProvider(selectedResources),
									IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));
							UIMonitorUtility.doTaskNowDefault(op, true);
						}
					});
					tAction.setEnabled(FileUtility.checkForResourcesPresence(selectedResources,
							AddToSVNIgnoreAction.SF_NEW_AND_PARENT_VERSIONED, IResource.DEPTH_ZERO));
				}
			} else {
				manager.add(tAction = new Action(SVNUIMessages.AddToSVNPanel_IgnoreByNames_Multiple) {
					@Override
					public void run() {
						CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", //$NON-NLS-1$
								SVNMessages.class);
						op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_NAME, null));
						op.add(new RefreshResourcesOperation(new ResourcesParentsProvider(selectedResources),
								IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				});
				tAction.setEnabled(tSelection.size() > 0);
				manager.add(tAction = new Action(SVNUIMessages.AddToSVNPanel_IgnoreByExtension_Multiple) {
					@Override
					public void run() {
						CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", //$NON-NLS-1$
								SVNMessages.class);
						op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_EXTENSION, null));
						op.add(new RefreshResourcesOperation(new ResourcesParentsProvider(selectedResources),
								IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));
						UIMonitorUtility.doTaskNowDefault(op, true);
					}
				});
				tAction.setEnabled(tSelection.size() > 0);
			}
			manager.add(new Separator());
			manager.add(tAction = new Action(SVNUIMessages.AddToSVNPanel_Delete_Action_Lable) {
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
			tAction.setEnabled(tSelection.size() > 0);
		});
		menuMgr.setRemoveAllWhenShown(true);
		tableViewer.getTable().setMenu(menu);
	}

	@Override
	public void postInit() {
		super.postInit();
		resourceStatesListener = event -> AddToSVNPanel.this.updateResources();
		SVNRemoteStorage.instance()
				.addResourceStatesListener(ResourceStatesChangedEvent.class, AddToSVNPanel.this.resourceStatesListener);
	}

	public boolean ifActionTookEffect() {
		return actionTookEffect;
	}

	public void updateResources() {
		HashSet<IResource> toDeleteSet = new HashSet<>(Arrays.asList(resources));
		HashSet<IResource> newResourcesSet = new HashSet<>();
		newResourcesSet.addAll(Arrays.asList(
				FileUtility.getResourcesRecursive(resources, IStateFilter.SF_UNVERSIONED, IResource.DEPTH_ZERO)));
		List<IResource> ignored = Arrays.asList(
				FileUtility.getResourcesRecursive(resources, IStateFilter.SF_IGNORED, IResource.DEPTH_ZERO));
		if (ignored.size() != 0) {
			actionTookEffect = true;
		}
		newResourcesSet.removeAll(Arrays.asList(
				FileUtility.getResourcesRecursive(resources, IStateFilter.SF_IGNORED, IResource.DEPTH_ZERO)));
		for (IResource element : resources) {
			if (!element.exists()) {
				newResourcesSet.remove(element);
				actionTookEffect = true;
			}
		}
		final IResource[] newResources = newResourcesSet.toArray(new IResource[newResourcesSet.size()]);
		toDeleteSet.removeAll(newResourcesSet);
		final IResource[] toDeleteResources = toDeleteSet.toArray(new IResource[toDeleteSet.size()]);

		if (!paneParticipantHelper.isParticipantPane()) {
			final TableViewer tableViewer = selectionComposite.getTableViewer();
			UIMonitorUtility.getDisplay().syncExec(() -> {
				AddToSVNPanel.this.selectionComposite.setResources(newResources);
				if (!tableViewer.getTable().isDisposed()) {
					tableViewer.remove(toDeleteResources);
					tableViewer.refresh();
					AddToSVNPanel.this.selectionComposite.fireSelectionChanged();
				}
			});
		}

		resources = newResources;
	}

	@Override
	public void dispose() {
		super.dispose();
		SVNRemoteStorage.instance()
				.removeResourceStatesListener(ResourceStatesChangedEvent.class, resourceStatesListener);
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.addToVCDialogContext"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.ui.panel.local.AbstractResourceSelectionPanel#createPaneParticipant()
	 */
	@Override
	protected BasePaneParticipant createPaneParticipant() {
		return new AddToSVNPaneParticipant(new ResourceScope(resources), this);
	}

}
