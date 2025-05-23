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

import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.synchronize.AbstractSynchronizeActionGroup;
import org.eclipse.team.svn.ui.synchronize.action.ExpandAllAction;
import org.eclipse.team.svn.ui.synchronize.update.UpdateParticipant;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Base pane participant class Used by ParticipantPagePane to show resources like in Synchronize view
 * 
 * Note: in order to add actions you need to override getActionGroups method
 * 
 * @author Igor Burilo
 */
public class BasePaneParticipant extends UpdateParticipant {

	protected IValidationManager validationManager;

	public BasePaneParticipant(ISynchronizeScope scope, IValidationManager validationManager) {
		super(scope);
		this.validationManager = validationManager;
	}

	/**
	 * Base class for panel action group
	 *
	 * @author Igor Burilo
	 */
	public static class BasePaneActionGroup extends AbstractSynchronizeActionGroup {

		protected static final String GROUP_SYNC_NORMAL = "syncNormal"; //$NON-NLS-1$

		protected IValidationManager validationManager;

		public BasePaneActionGroup(IValidationManager validationManager) {
			this.validationManager = validationManager;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#modelChanged(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
		 */
		@Override
		public void modelChanged(ISynchronizeModelElement root) {
			super.modelChanged(root);

			Display.getDefault().asyncExec(() -> {
				if (validationManager != null) {
					validationManager.validateContent();
				}
			});
		}

		@Override
		public void configureMenuGroups(ISynchronizePageConfiguration configuration) {
			configuration.addMenuGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL);
		}

		@Override
		protected void configureActions(ISynchronizePageConfiguration configuration) {
			this.appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, BasePaneActionGroup.GROUP_SYNC_NORMAL,
					new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

			//expand all
			ExpandAllAction expandAllAction = new ExpandAllAction(SVNUIMessages.SynchronizeActionGroup_ExpandAll,
					configuration, getVisibleRootsSelectionProvider());
			expandAllAction
					.setImageDescriptor(SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/expandall.gif")); //$NON-NLS-1$
			this.appendToGroup(
					ISynchronizePageConfiguration.P_TOOLBAR_MENU, ISynchronizePageConfiguration.NAVIGATE_GROUP,
					expandAllAction);
		}
	}

	@Override
	protected int getSupportedModes() {
		return ISynchronizePageConfiguration.OUTGOING_MODE;
	}

	@Override
	protected int getDefaultMode() {
		return ISynchronizePageConfiguration.OUTGOING_MODE;
	}

	@Override
	public ChangeSetCapability getChangeSetCapability() {
		return null; // we don't want that button
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#doesSupportSynchronize()
	 */
	@Override
	public boolean doesSupportSynchronize() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.ui.synchronize.AbstractSVNParticipant#isSetModes()
	 */
	@Override
	protected boolean isSetModes() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.ui.synchronize.update.UpdateParticipant#getActionGroups()
	 */
	@Override
	protected Collection<AbstractSynchronizeActionGroup> getActionGroups() {
		Collection<AbstractSynchronizeActionGroup> actionGroups = new ArrayList<>();
		return actionGroups;
	}

}
