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

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.ui.action.AbstractRecursiveTeamAction;
import org.eclipse.team.svn.ui.operation.ShowConflictEditorOperation;

/**
 * Edit conflicting files action implementation
 * 
 * @author Alexander Gurov
 */
public class EditConflictsAction extends AbstractRecursiveTeamAction {
	public EditConflictsAction() {
		super();
	}

	public void runImpl(IAction action) {
		this.runScheduled(new ShowConflictEditorOperation(
				this.getSelectedResourcesRecursive(IStateFilter.SF_DATA_CONFLICTING), false));
	}

	public boolean isEnabled() {
		return this.checkForResourcesPresenceRecursive(IStateFilter.SF_DATA_CONFLICTING);
	}

	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

}
