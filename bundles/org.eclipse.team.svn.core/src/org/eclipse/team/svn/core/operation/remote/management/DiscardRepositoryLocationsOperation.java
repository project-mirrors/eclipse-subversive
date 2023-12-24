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

package org.eclipse.team.svn.core.operation.remote.management;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;

/**
 * Discard location operation
 * 
 * @author Alexander Gurov
 */
public class DiscardRepositoryLocationsOperation extends AbstractActionOperation {
	protected IRepositoryLocation[] locations;

	public DiscardRepositoryLocationsOperation(IRepositoryLocation[] locations) {
		super("Operation_DiscardRepositoryLocation", SVNMessages.class); //$NON-NLS-1$
		this.locations = locations;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		for (final IRepositoryLocation current : locations) {
			this.protectStep(monitor1 -> SVNRemoteStorage.instance().removeRepositoryLocation(current), monitor, locations.length);
		}
	}

}
