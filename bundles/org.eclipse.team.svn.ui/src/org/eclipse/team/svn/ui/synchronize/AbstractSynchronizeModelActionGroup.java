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

package org.eclipse.team.svn.ui.synchronize;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelParticipantAction;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;

/**
 * Abstract synchronize view logical model action contribution implementation
 * 
 * @author Igor Burilo
 */
public abstract class AbstractSynchronizeModelActionGroup extends ModelSynchronizeParticipantActionGroup {

	public static final String GROUP_MANAGE_LOCALS = "modelManageLocalChanges"; //$NON-NLS-1$

	public static final String GROUP_PROCESS_ALL = "modelProcessAllItems"; //$NON-NLS-1$

	public static final String GROUP_TEAM = "modelTeam"; //$NON-NLS-1$

	protected ISynchronizePageConfiguration configuration;

	protected MenuManager outgoing;

	protected MenuManager incoming;

	public AbstractSynchronizeModelActionGroup() {
	}

	@Override
	public final void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(this.configuration = configuration);
		configureActions(configuration);
	}

	@Override
	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public void dispose() {
		if (outgoing != null) {
			outgoing.removeAll();
			outgoing.dispose();
		}

		if (incoming != null) {
			incoming.removeAll();
			incoming.dispose();
		}

		super.dispose();
	}

	protected abstract void configureActions(ISynchronizePageConfiguration configuration);

	protected void addSpecificActions(final AbstractSynchronizeLogicalModelAction selectionProvider,
			final ISynchronizePageConfiguration configuration) {
		outgoing = new MenuManager(SVNUIMessages.SynchronizeActionGroup_Outgoing);
		this.appendToGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, AbstractSynchronizeModelActionGroup.GROUP_TEAM, outgoing);

		incoming = new MenuManager(SVNUIMessages.SynchronizeActionGroup_Incoming);
		this.appendToGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, AbstractSynchronizeModelActionGroup.GROUP_TEAM, incoming);

		boolean isEuropa = false;
		String version = Platform.getBundle("org.eclipse.core.runtime").getHeaders().get("Bundle-Version"); //$NON-NLS-1$ //$NON-NLS-2$
		if (version != null) {
			isEuropa = "3.4.0".compareTo(version) > 0; //$NON-NLS-1$
		}
		if (isEuropa) {
			addLocalActions(outgoing, configuration);
			addRemoteActions(incoming, configuration);
		} else {
			outgoing.setRemoveAllWhenShown(true);
			outgoing.addMenuListener(manager -> {
				AbstractSynchronizeModelActionGroup.this.addLocalActions(manager, configuration);
				AbstractSynchronizeModelActionGroup.this.updateSelection(manager,
						selectionProvider.getStructuredSelection());
			});
			incoming.setRemoveAllWhenShown(true);
			incoming.addMenuListener(manager -> {
				AbstractSynchronizeModelActionGroup.this.addRemoteActions(manager, configuration);
				AbstractSynchronizeModelActionGroup.this.updateSelection(manager,
						selectionProvider.getStructuredSelection());
			});
		}
	}

	protected void addLocalActions(IMenuManager manager, ISynchronizePageConfiguration configuration) {

	}

	protected void addRemoteActions(IMenuManager manager, ISynchronizePageConfiguration configuration) {

	}

	protected void updateSelection(IMenuManager manager, ISelection selection) {
		IContributionItem[] items = manager.getItems();
		for (IContributionItem item : items) {
			if (item instanceof ActionContributionItem) {
				IAction actionItem = ((ActionContributionItem) item).getAction();
				if (actionItem instanceof ModelParticipantAction) {
					((ModelParticipantAction) actionItem).selectionChanged(selection);
				}
			}
		}
	}
}
