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
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;

/**
 * Add repository location implementation
 * 
 * @author Alexander Gurov
 */
public class AddRepositoryLocationOperation extends AbstractActionOperation {
	protected IRepositoryLocation location;

	public AddRepositoryLocationOperation(IRepositoryLocation location) {
		super("Operation_AddRepositoryLocation", SVNMessages.class); //$NON-NLS-1$
		this.location = location;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		SVNRemoteStorage.instance().addRepositoryLocation(location);
		//try to fetch repository root URL and UUID
		location.getRepositoryRootUrl();
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { location.getUrl() });
	}

}
