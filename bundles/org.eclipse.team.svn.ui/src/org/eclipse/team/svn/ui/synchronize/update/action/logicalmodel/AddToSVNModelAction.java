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

package org.eclipse.team.svn.ui.synchronize.update.action.logicalmodel;

import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.svn.ui.synchronize.update.action.AddToSVNModelActionHelper;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Synchronize view add to version control logical model action implementation
 * 
 * @author Igor Burilo
 */
public class AddToSVNModelAction extends AbstractSynchronizeLogicalModelAction {

	protected AddToSVNModelActionHelper actionHelper;
	
	public AddToSVNModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		this.actionHelper = new AddToSVNModelActionHelper(this, configuration);
	}

	protected FastSyncInfoFilter getSyncInfoFilter() {
		return this.actionHelper.getSyncInfoFilter();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction#getOperation()
	 */
	protected IActionOperation getOperation() {
		return this.actionHelper.getOperation();
	}

}
