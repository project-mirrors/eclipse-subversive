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

package org.eclipse.team.svn.ui.extension.impl.synchronize;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSubscriber;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.synchronize.AbstractSynchronizeActionGroup;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Synchronize options action set
 * 
 * @author Alexander Gurov
 */
public class OptionsActionGroup extends AbstractSynchronizeActionGroup {
	public static final String GROUP_SYNCH_OPTIONS = "synchronizeViewOptions"; //$NON-NLS-1$

	protected IAction contiguousOptionAction;

	@Override
	public void configureMenuGroups(ISynchronizePageConfiguration configuration) {
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_VIEW_MENU, OptionsActionGroup.GROUP_SYNCH_OPTIONS);
	}

	@Override
	protected void configureActions(ISynchronizePageConfiguration configuration) {
		contiguousOptionAction = new Action(SVNUIMessages.OptionsActionGroup_ShowInfoContiguous, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				boolean fastReportMode = AbstractSVNSubscriber.getSynchInfoContigous();
				AbstractSVNSubscriber.setSynchInfoContigous(!fastReportMode);
				SVNTeamPlugin.instance().savePreferences();
			}
		};
		refreshOptionButtons();
		this.appendToGroup(
				ISynchronizePageConfiguration.P_VIEW_MENU, OptionsActionGroup.GROUP_SYNCH_OPTIONS,
				contiguousOptionAction);
	}

	protected void refreshOptionButtons() {
		contiguousOptionAction.setChecked(AbstractSVNSubscriber.getSynchInfoContigous());
	}
}
