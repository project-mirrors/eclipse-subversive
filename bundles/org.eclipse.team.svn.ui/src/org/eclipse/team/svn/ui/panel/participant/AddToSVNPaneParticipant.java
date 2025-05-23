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

package org.eclipse.team.svn.ui.panel.participant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNIgnoreOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.resource.ISVNStorage;
import org.eclipse.team.svn.core.svnstorage.ResourcesParentsProvider;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.synchronize.AbstractSynchronizeActionGroup;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.svn.ui.synchronize.action.DeletePaneAction;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;

/**
 * Add to SVN pane participant
 * 
 * @author Igor Burilo
 */
public class AddToSVNPaneParticipant extends BasePaneParticipant {

	public AddToSVNPaneParticipant(ISynchronizeScope scope, IValidationManager validationManager) {
		super(scope, validationManager);
	}

	@Override
	protected Collection<AbstractSynchronizeActionGroup> getActionGroups() {
		List<AbstractSynchronizeActionGroup> actionGroups = new ArrayList<>();
		actionGroups.add(new AddToSVNPaneActionGroup(validationManager));
		return actionGroups;
	}

	/**
	 * Add to SVN pane's action set
	 *
	 * 
	 * @author Igor Burilo
	 */
	protected static class AddToSVNPaneActionGroup extends BasePaneActionGroup {

		public AddToSVNPaneActionGroup(IValidationManager validationManager) {
			super(validationManager);
		}

		/**
		 * Add to SVN ignore by name
		 *
		 * @author Igor Burilo
		 */
		protected static class AddToIgnoreByNameAction extends AbstractSynchronizeModelAction {

			public AddToIgnoreByNameAction(String text, ISynchronizePageConfiguration configuration) {
				super(text, configuration);
			}

			@Override
			protected IActionOperation getOperation(ISynchronizePageConfiguration configuration,
					IDiffElement[] elements) {
				IResource[] selectedResources = getAllSelectedResources();
				CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", SVNMessages.class); //$NON-NLS-1$
				op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_NAME, null));
				op.add(new RefreshResourcesOperation(new ResourcesParentsProvider(selectedResources),
						IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));
				return op;
			}

			@Override
			protected boolean updateSelection(IStructuredSelection selection) {
				IResource[] selectedResources = getAllSelectedResources();
				if (selectedResources.length == 1) {
					setText(BaseMessages.format(SVNUIMessages.AddToSVNPanel_Ignore_Single,
							selectedResources[0].getName()));
				} else if (selectedResources.length > 1) {
					setText(SVNUIMessages.AddToSVNPanel_IgnoreByNames_Multiple);
				}

				return super.updateSelection(selection);
			}
		}

		/**
		 * Add to SVN ignore by extension
		 *
		 * @author Igor Burilo
		 */
		protected static class AddToIgnoreByExtensionAction extends AbstractSynchronizeModelAction {

			public AddToIgnoreByExtensionAction(String text, ISynchronizePageConfiguration configuration) {
				super(text, configuration);
			}

			@Override
			protected IActionOperation getOperation(ISynchronizePageConfiguration configuration,
					IDiffElement[] elements) {
				IResource[] selectedResources = getAllSelectedResources();
				CompositeOperation op = new CompositeOperation("Operation_AddToSVNIgnore", SVNMessages.class); //$NON-NLS-1$
				op.add(new AddToSVNIgnoreOperation(selectedResources, ISVNStorage.IGNORE_EXTENSION, null));
				op.add(new RefreshResourcesOperation(new ResourcesParentsProvider(selectedResources),
						IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));
				return op;
			}

			@Override
			protected boolean updateSelection(IStructuredSelection selection) {
				IResource[] selectedResources = getAllSelectedResources();
				if (selectedResources.length == 1) {
					String[] parts = getNameParts(selectedResources);
					setText(BaseMessages.format(SVNUIMessages.AddToSVNPanel_Ignore_Single,
							"*." + parts[parts.length - 1])); //$NON-NLS-1$
				} else if (selectedResources.length > 1) {
					setText(SVNUIMessages.AddToSVNPanel_IgnoreByExtension_Multiple);
				}

				boolean isUpdate = false;
				if (super.updateSelection(selection)) {
					if (selectedResources.length == 1) {
						String[] parts = getNameParts(selectedResources);
						if (parts.length != 0) {
							isUpdate = true;
						}
					} else {
						isUpdate = true;
					}
				}
				return isUpdate;
			}

			protected String[] getNameParts(IResource[] selectedResources) {
				String name = selectedResources[0].getName();
				String[] parts = name.split("\\."); //$NON-NLS-1$
				return parts;
			}
		}

		@Override
		protected void configureActions(ISynchronizePageConfiguration configuration) {
			super.configureActions(configuration);

			//add to ignore by name
			AddToIgnoreByNameAction addToIgnoreByNameAction = new AddToIgnoreByNameAction(
					SVNUIMessages.AddToSVNPanel_IgnoreByNames_Multiple, configuration);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					addToIgnoreByNameAction);

			//add to ignore by extension
			AddToIgnoreByExtensionAction addToIgnoreByExtensionAction = new AddToIgnoreByExtensionAction(
					SVNUIMessages.AddToSVNPanel_IgnoreByExtension_Multiple, configuration);
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					addToIgnoreByExtensionAction);

			//separator
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					new Separator());

			//delete
			DeletePaneAction deleteAction = new DeletePaneAction(SVNUIMessages.CommitPanel_Delete_Action,
					configuration);
			deleteAction.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/delete.gif")); //$NON-NLS-1$
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL, deleteAction);
		}
	}

}
