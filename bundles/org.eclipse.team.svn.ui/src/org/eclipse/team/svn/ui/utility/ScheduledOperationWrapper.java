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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Scheduled
 * 
 * @author Alexander Gurov
 */
public class ScheduledOperationWrapper extends Job {

	protected ICancellableOperationWrapper operationWrapper;

	public ScheduledOperationWrapper(ICancellableOperationWrapper operationWrapper) {
		super(operationWrapper.getOperationName());
		this.operationWrapper = operationWrapper;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			operationWrapper.run(monitor);
		} catch (InterruptedException e) {
			operationWrapper.setCancelled(true);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return Status.OK_STATUS;//this.operationWrapper.getOperation().getStatus()
	}

}
