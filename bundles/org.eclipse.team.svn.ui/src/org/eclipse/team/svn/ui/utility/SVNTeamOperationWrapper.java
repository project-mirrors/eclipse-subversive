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

package org.eclipse.team.svn.ui.utility;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This wrapper provide ability to run ICancellableOperationWrapper scheduled
 * 
 * @author Alexander Gurov
 */
public class SVNTeamOperationWrapper extends TeamOperation {

	protected ICancellableOperationWrapper operationWrapper;

	public SVNTeamOperationWrapper(IWorkbenchPart part, ICancellableOperationWrapper operationWrapper) {
		super(part);
		this.operationWrapper = operationWrapper;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		operationWrapper.run(monitor);
	}

	@Override
	protected boolean canRunAsJob() {
		return true;
	}

	@Override
	protected String getJobName() {
		return operationWrapper.getOperationName();
	}

}
