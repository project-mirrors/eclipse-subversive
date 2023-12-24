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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Composite operation provide way to combine different operations
 * 
 * @author Alexander Gurov
 */
public class CompositeOperation extends AbstractActionOperation implements IConsoleStream {
	protected List<Pair> operations;

	protected boolean checkWarnings;

	protected int totalWeight;

	public CompositeOperation(String operationName, Class<? extends NLS> messagesClass) {
		this(operationName, messagesClass, false);
	}

	public CompositeOperation(String operationName, Class<? extends NLS> messagesClass, boolean checkWarnings) {
		super(operationName, messagesClass);
		operations = new ArrayList<>();
		this.checkWarnings = checkWarnings;
		totalWeight = 0;
	}

	public boolean isEmpty() {
		return operations.size() == 0;
	}

	public void add(IActionOperation operation) {
		this.add(operation, null);
	}

	public void add(IActionOperation operation, IActionOperation[] dependsOnOperation) {
		operation.setConsoleStream(this);
		operations.add(new Pair(operation, dependsOnOperation));
		totalWeight += operation.getOperationWeight();
	}

	public void remove(IActionOperation operation) {
		for (Iterator<Pair> it = operations.iterator(); it.hasNext();) {
			Pair pair = it.next();
			if (pair.operation == operation) {
				if (operation.getConsoleStream() == this) {
					operation.setConsoleStream(null);
				}
				it.remove();
				totalWeight -= operation.getOperationWeight();
				break;
			}
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		ISchedulingRule retVal = null;
		for (Pair pair : operations) {
			retVal = MultiRule.combine(retVal, pair.operation.getSchedulingRule());
		}
		return retVal;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		for (Iterator<Pair> it = operations.iterator(); it.hasNext() && !monitor.isCanceled();) {
			Pair pair = it.next();

			boolean errorFound = false;
			if (pair.dependsOnOperation != null) {
				for (IActionOperation element : pair.dependsOnOperation) {
					if (element.getStatus().getSeverity() == IStatus.ERROR
							|| checkWarnings && element.getStatus().getSeverity() == IStatus.WARNING
							|| element.getExecutionState() == IActionOperation.NOTEXECUTED) {
						errorFound = true;
						break;
					}
				}
			}
			if (!errorFound) {
				ProgressMonitorUtility.doTask(pair.operation, monitor, totalWeight,
						pair.operation.getOperationWeight());
				this.reportStatus(pair.operation.getStatus());
			}
		}
	}

	protected class Pair {
		public IActionOperation operation;

		public IActionOperation[] dependsOnOperation;

		public Pair(IActionOperation operation, IActionOperation[] dependsOnOperation) {
			this.operation = operation;
			this.dependsOnOperation = dependsOnOperation;
		}
	}

	@Override
	public void markEnd() {
		// do nothing
	}

	@Override
	public void markStart(String data) {
		// do nothing
	}

	@Override
	public void write(int severity, String data) {
		writeToConsole(severity, data);
	}

	@Override
	public void doComplexWrite(Runnable runnable) {
		complexWriteToConsole(runnable);
	}

	@Override
	public void markCancelled() {
		writeCancelledToConsole();
	}

}
