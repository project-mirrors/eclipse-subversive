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

package org.eclipse.team.svn.ui.synchronize;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

/**
 * Abstract synchronize view action contribution implementation
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractSynchronizeActionGroup extends SynchronizePageActionGroup {
	public static final String GROUP_MANAGE_LOCALS = "manageLocalChanges"; //$NON-NLS-1$

	public static final String GROUP_PROCESS_ALL = "processAllItems"; //$NON-NLS-1$

	public static final String GROUP_TEAM = "team"; //$NON-NLS-1$

	protected ISynchronizePageConfiguration configuration;

	protected MenuManager outgoing;

	protected MenuManager incoming;

	public AbstractSynchronizeActionGroup() {
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

	public abstract void configureMenuGroups(ISynchronizePageConfiguration configuration);

	protected abstract void configureActions(ISynchronizePageConfiguration configuration);

	protected void addSpecificActions(final AbstractSynchronizeModelAction selectionProvider,
			final ISynchronizePageConfiguration configuration) {
		outgoing = new MenuManager(SVNUIMessages.SynchronizeActionGroup_Outgoing);
		this.appendToGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, AbstractSynchronizeActionGroup.GROUP_TEAM, outgoing);

		incoming = new MenuManager(SVNUIMessages.SynchronizeActionGroup_Incoming);
		this.appendToGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, AbstractSynchronizeActionGroup.GROUP_TEAM, incoming);

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
				AbstractSynchronizeActionGroup.this.addLocalActions(manager, configuration);
				AbstractSynchronizeActionGroup.this.updateSelection(manager,
						selectionProvider.getStructuredSelection());
			});
			incoming.setRemoveAllWhenShown(true);
			incoming.addMenuListener(manager -> {
				AbstractSynchronizeActionGroup.this.addRemoteActions(manager, configuration);
				AbstractSynchronizeActionGroup.this.updateSelection(manager,
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
				if (actionItem instanceof SynchronizeModelAction) {
					((SynchronizeModelAction) actionItem).selectionChanged(selection);
				}
			}
		}
	}
}
