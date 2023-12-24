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
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;

/**
 * Save repository location changes operation
 * 
 * @author Alexander Gurov
 */
public class SaveRepositoryLocationsOperation extends AbstractActionOperation {
	public SaveRepositoryLocationsOperation() {
		super("Operation_SaveRepositoryLocations", SVNMessages.class); //$NON-NLS-1$
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		SVNRemoteStorage.instance().saveConfiguration();
		if (SVNTeamPlugin.instance().isLocationsDirty()) {
			SVNFileStorage.instance().saveConfiguration();
			SVNTeamPlugin.instance().setLocationsDirty(false);
		}
	}

}
