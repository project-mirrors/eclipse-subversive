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

package org.eclipse.team.svn.ui.action;

import org.eclipse.team.svn.ui.utility.IOperationWrapperFactory;
import org.eclipse.team.svn.ui.utility.WorkspaceModifyOperationWrapperFactory;

/**
 * Abstract UI repository action that is not indifferent to workspace modifications
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractRepositoryModifyWorkspaceAction extends AbstractRepositoryTeamAction {

	public AbstractRepositoryModifyWorkspaceAction() {
	}

	@Override
	protected IOperationWrapperFactory getOperationWrapperFactory() {
		return new WorkspaceModifyOperationWrapperFactory();
	}

}
