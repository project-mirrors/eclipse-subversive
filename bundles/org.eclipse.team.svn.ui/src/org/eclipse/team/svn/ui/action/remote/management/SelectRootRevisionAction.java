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

package org.eclipse.team.svn.ui.action.remote.management;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.repository.model.RepositoryRevisions;

/**
 * Select revision for repository location root
 * 
 * @author Alexander Gurov
 */
public class SelectRootRevisionAction extends SelectResourceRevisionAction {
	public SelectRootRevisionAction() {
		super();
	}

	public void runImpl(IAction action) {
		RepositoryRevisions root = ((RepositoryRevisions [])this.getAdaptedSelection(RepositoryRevisions.class))[0];
		this.runImpl(new IRepositoryResource[] {root.getRepositoryLocation().getRoot()});
	}
	
	public boolean isEnabled() {
		return true;
	}
	
}
