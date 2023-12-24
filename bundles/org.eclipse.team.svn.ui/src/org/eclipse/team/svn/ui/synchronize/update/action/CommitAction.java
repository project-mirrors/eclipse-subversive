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
 *    Alessandro Nistico - [patch] Change Set's implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.update.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.svn.ui.synchronize.action.CommitActionHelper;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Synchronize view commit action implementation
 * 
 * @author Alexander Gurov
 */
public class CommitAction extends AbstractSynchronizeModelAction {

	protected CommitActionHelper actionHelper;

	public CommitAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		actionHelper = new CommitActionHelper(this, configuration);
	}

	public CommitAction(String text, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		actionHelper = new CommitActionHelper(this, configuration);
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
