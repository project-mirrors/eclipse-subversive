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

package org.eclipse.team.svn.core.operation;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.team.svn.core.connector.ISVNProgressMonitor;
import org.eclipse.team.svn.core.connector.SVNCommitStatus;

/**
 * Empty implementation of the IProgressMonitor interface
 * 
 * @author Alexander Gurov
 */
public class SVNNullProgressMonitor implements ISVNProgressMonitor {
	protected ArrayList<SVNCommitStatus> commitStatuses = new ArrayList<>();

	public Collection<SVNCommitStatus> getCommitStatuses() {
		return commitStatuses;
	}

	public Collection<SVNCommitStatus> getPostCommitErrors() {
		ArrayList<SVNCommitStatus> retVal = new ArrayList<>(commitStatuses);
		return retVal;
	}

	@Override
	public void progress(int current, int total, ItemState state) {

	}

	@Override
	public boolean isActivityCancelled() {
		return false;
	}

	@Override
	public void commitStatus(SVNCommitStatus status) {
		commitStatuses.add(status);
	}

}
