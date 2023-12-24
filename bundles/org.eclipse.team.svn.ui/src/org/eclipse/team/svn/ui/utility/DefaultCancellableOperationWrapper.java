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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * This wrapper allows to run operations that do not modify workspace
 * 
 * @author Alexander Gurov
 */
public class DefaultCancellableOperationWrapper implements ICancellableOperationWrapper {
	protected IProgressMonitor attachedMonitor;

	protected IActionOperation operation;

	public DefaultCancellableOperationWrapper(IActionOperation operation) {
		this.operation = operation;
		attachedMonitor = new NullProgressMonitor();
	}

	@Override
	public void setCancelled(boolean cancelled) {
		attachedMonitor.setCanceled(cancelled);
	}

	@Override
	public boolean isCancelled() {
		return attachedMonitor.isCanceled();
	}

	@Override
	public IActionOperation getOperation() {
		return operation;
	}

	@Override
	public String getOperationName() {
		return operation.getOperationName();
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.setCanceled(attachedMonitor.isCanceled());
		attachedMonitor = monitor;
		// wrap external monitor and make instance of SubProgressMonitorWithInfo
		ProgressMonitorUtility.doTaskExternal(operation, attachedMonitor, null);
	}

}
