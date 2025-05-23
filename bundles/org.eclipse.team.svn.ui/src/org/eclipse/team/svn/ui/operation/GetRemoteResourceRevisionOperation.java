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
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Allows us to get revision in the separate thread
 * 
 * @author Alexander Gurov
 */
public class GetRemoteResourceRevisionOperation extends AbstractActionOperation {
	protected IRepositoryResource resource;

	protected long revision;

	public GetRemoteResourceRevisionOperation(IRepositoryResource resource) {
		super("Operation_GetRemoteRevision", SVNUIMessages.class); //$NON-NLS-1$
		this.resource = resource;
		revision = SVNRevision.INVALID_REVISION_NUMBER;
	}

	public long getRevision() {
		return revision;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		revision = resource.getRevision();
	}

}
