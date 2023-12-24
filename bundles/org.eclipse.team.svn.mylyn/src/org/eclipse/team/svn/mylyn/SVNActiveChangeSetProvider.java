/*******************************************************************************
 * Copyright (c) 2008, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.mylyn;

import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.team.ui.AbstractActiveChangeSetProvider;
import org.eclipse.mylyn.team.ui.IContextChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.svn.core.SVNTeamPlugin;

/**
 * Provides access to SVN ActiveChangeSet's
 * 
 * @author Alexander Gurov
 */
public class SVNActiveChangeSetProvider extends AbstractActiveChangeSetProvider {
	public ActiveChangeSetManager getActiveChangeSetManager() {
		return SVNTeamPlugin.instance().getModelChangeSetManager();
	}

	public IContextChangeSet createChangeSet(ITask task) {
		return new SVNContextChangeSet(task, getActiveChangeSetManager());
	}

}
