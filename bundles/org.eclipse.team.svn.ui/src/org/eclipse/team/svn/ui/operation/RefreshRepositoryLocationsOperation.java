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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.repository.RepositoriesView;
import org.eclipse.team.svn.ui.repository.model.RepositoryLocation;

/**
 * Refresh repository location in the repository tree operation
 * 
 * @author Alexander Gurov
 */
public class RefreshRepositoryLocationsOperation extends AbstractActionOperation {
	protected IRepositoryLocation[] resources;

	protected boolean deep;

	public RefreshRepositoryLocationsOperation(boolean deep) {
		this(null, deep);
	}

	public RefreshRepositoryLocationsOperation(IRepositoryLocation[] resources, boolean deep) {
		super("Operation_RefreshLocations", SVNUIMessages.class); //$NON-NLS-1$
		this.resources = resources;
		this.deep = deep;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (resources == null) {
			RepositoriesView.refreshRepositories(deep);
			return;
		}

		for (final IRepositoryLocation current : resources) {
			this.protectStep(monitor1 -> RepositoriesView.refresh(current, data -> {
				if (data instanceof RepositoryLocation && deep) {
					((RepositoryLocation) data).refresh();
				}
			}), monitor, resources.length);
		}
	}

}
