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

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.RemoteShowAnnotationOperation;

/**
 * Show annotation for repository file
 * 
 * @author Alexander Gurov
 */
public class ShowAnnotationAction extends AbstractRepositoryTeamAction {

	public ShowAnnotationAction() {
		super();
	}

	public void runImpl(IAction action) {
		this.runScheduled(new RemoteShowAnnotationOperation(this.getSelectedRepositoryResources()[0]));
	}

	public boolean isEnabled() {
		return this.getSelectedRepositoryResources().length == 1;
	}

}
