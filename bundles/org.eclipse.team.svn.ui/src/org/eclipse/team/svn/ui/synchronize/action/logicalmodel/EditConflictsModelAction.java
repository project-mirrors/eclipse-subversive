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

package org.eclipse.team.svn.ui.synchronize.action.logicalmodel;

import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.svn.ui.synchronize.action.EditConflictsActionHelper;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Edit conflicts logical model action implementation for Synchronize view
 * 
 * @author Igor Burilo
 */
public class EditConflictsModelAction extends AbstractSynchronizeLogicalModelAction {

	protected EditConflictsActionHelper actionHelper;

	public EditConflictsModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		actionHelper = new EditConflictsActionHelper(this, configuration);
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return actionHelper.getSyncInfoFilter();
	}

	@Override
	protected IActionOperation getOperation() {
		return actionHelper.getOperation();
	}

}
