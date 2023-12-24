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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.repository.model.IResourceTreeNode;
import org.eclipse.team.svn.ui.repository.model.RepositoryLocation;

/**
 * Abstract UI repository action
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractRepositoryTeamAction extends AbstractSVNTeamAction {
	private IStructuredSelection selection;

	public AbstractRepositoryTeamAction() {
	}

	@Override
	protected IStructuredSelection getSelection() {
		if (selection == null) {
			selection = StructuredSelection.EMPTY;
		}
		return selection;
	}

	@Override
	protected void checkSelection(IStructuredSelection selection) {
		this.selection = selection;
	}

	protected IRepositoryLocation[] getSelectedRepositoryLocations() {
		Object[] locationWrappers = this.getAdaptedSelection(RepositoryLocation.class);
		IRepositoryLocation[] locations = new IRepositoryLocation[locationWrappers.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = ((RepositoryLocation) locationWrappers[i]).getRepositoryLocation();
		}
		return locations;
	}

	protected IRepositoryResource[] getSelectedRepositoryResources() {
		Object[] wrappers = this.getAdaptedSelection(IResourceTreeNode.class);
		IRepositoryResource[] resources = new IRepositoryResource[wrappers.length];
		for (int i = 0; i < resources.length; i++) {
			resources[i] = ((IResourceTreeNode) wrappers[i]).getRepositoryResource();
		}
		return resources;
	}

}
