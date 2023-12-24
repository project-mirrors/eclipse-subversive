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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.SVNConnectorCancelException;

/**
 * Logged operation allow us to safely write to log and show error messages
 * 
 * @author Alexander Gurov
 */
public class LoggedOperation implements IActionOperation {
	protected IActionOperation op;

	public LoggedOperation(IActionOperation op) {
		this.op = op;
	}

	@Override
	final public IActionOperation run(IProgressMonitor monitor) {
		IStatus status = op.run(monitor).getStatus();
		if (status.getSeverity() != IStatus.OK) {
			handleError(status);
		}
		return op;
	}

	@Override
	public int getOperationWeight() {
		return IActionOperation.DEFAULT_WEIGHT;
	}

	@Override
	public IConsoleStream getConsoleStream() {
		return op.getConsoleStream();
	}

	@Override
	public void setConsoleStream(IConsoleStream stream) {
		op.setConsoleStream(stream);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return op.getSchedulingRule();
	}

	@Override
	public String getOperationName() {
		return op.getOperationName();
	}

	@Override
	public String getId() {
		return op.getId();
	}

	@Override
	public Class<? extends NLS> getMessagesClass() {
		return op.getMessagesClass();
	}

	@Override
	public final IStatus getStatus() {
		return op.getStatus();
	}

	@Override
	public int getExecutionState() {
		return op.getExecutionState();
	}

	@Override
	public void reportStatus(int severity, String message, Throwable t) {
		op.reportStatus(severity, message, t);
	}

	public static void reportError(String where, Throwable t) {
		String errMessage = BaseMessages.format(SVNMessages.Operation_Error_LogHeader, new String[] { where });
		MultiStatus status = new MultiStatus(SVNTeamPlugin.NATURE_ID, IStatus.OK, errMessage, null);
		Status st = new Status(
				IStatus.ERROR, SVNTeamPlugin.NATURE_ID, IStatus.OK, status.getMessage() + ": " + t.getMessage(), //$NON-NLS-1$
				t);
		status.merge(st);
		LoggedOperation.logError(status);
	}

	protected void handleError(IStatus errorStatus) {
		if (!errorStatus.isMultiStatus()) {
			Throwable ex = errorStatus.getException();
			if (!(ex instanceof SVNConnectorCancelException) && !(ex instanceof ActivityCancelledException)
					&& !(ex instanceof OperationCanceledException)) {
				LoggedOperation.logError(errorStatus);
			}
			return;
		}

		IStatus[] children = errorStatus.getChildren();
		ArrayList<IStatus> statusesWithoutCancel = new ArrayList<>();
		for (IStatus child : children) {
			Throwable exception = child.getException();
			if (!(exception instanceof SVNConnectorCancelException)
					&& !(exception instanceof ActivityCancelledException)
					&& !(exception instanceof OperationCanceledException)) {
				statusesWithoutCancel.add(child);
			}
		}
		if (statusesWithoutCancel.size() > 0) {
			IStatus newStatus = new MultiStatus(errorStatus.getPlugin(), errorStatus.getCode(),
					statusesWithoutCancel.toArray(new IStatus[statusesWithoutCancel.size()]), errorStatus.getMessage(),
					errorStatus.getException());
			LoggedOperation.logError(newStatus);
		}
	}

	protected static void logError(IStatus errorStatus) {
		SVNTeamPlugin.instance().getLog().log(errorStatus);
	}

}
