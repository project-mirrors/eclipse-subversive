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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

/**
 * Synchronize model operation wrapper to provide a probability of running the operations by our code.
 * 
 * @author Igor Burilo
 */
public class FilteredSynchronizeModelOperation extends SynchronizeModelOperation {

	protected IActionOperation executable;

	public FilteredSynchronizeModelOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements,
			IActionOperation executable) {
		super(configuration, elements);
		this.executable = executable;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (executable != null) {
			ProgressMonitorUtility.doTaskExternal(executable, monitor);
		}
	}

	@Override
	protected boolean canRunAsJob() {
		return true;
	}

	@Override
	protected String getJobName() {
		return executable == null ? super.getJobName() : executable.getOperationName();
	}

}
