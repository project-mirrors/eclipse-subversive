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

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Synchronize view revert action implementation
 * 
 * @author Alexander Gurov
 */
public class RevertAction extends AbstractSynchronizeModelAction {

	protected RevertActionHelper actionHelper;

	public RevertAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		actionHelper = new RevertActionHelper(this, configuration);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return actionHelper.getSyncInfoFilter();
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return actionHelper.getOperation();
	}

}
