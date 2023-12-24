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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.SVNConnectorCancelException;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Abstract IActionOperation implementation provides default implementation of the status processing and console support
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractActionOperation implements IActionOperation {
	private final MultiStatus status = new MultiStatus(SVNTeamPlugin.NATURE_ID, IStatus.OK, "", null); //$NON-NLS-1$

	protected String nameId;

	protected String name;

	protected boolean isExecuted;

	protected IConsoleStream consoleStream;

	protected Class<? extends NLS> messagesClass;

	public AbstractActionOperation(String operationName, Class<? extends NLS> messagesClass) {
		isExecuted = false;
		this.messagesClass = messagesClass;
		setOperationName(operationName);
		updateStatusMessage();
	}

	@Override
	public int getOperationWeight() {
		return IActionOperation.DEFAULT_WEIGHT;
	}

	@Override
	public IConsoleStream getConsoleStream() {
		return consoleStream;
	}

	@Override
	public void setConsoleStream(IConsoleStream stream) {
		consoleStream = stream;
	}

	@Override
	public IStatus getStatus() {
		return status;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		// do not lock anything by default
		return null;
	}

	@Override
	public int getExecutionState() {
		if (status.isOK()) {
			return isExecuted ? IActionOperation.OK : IActionOperation.NOTEXECUTED;
		}
		return IActionOperation.ERROR;
	}

	@Override
	public final IActionOperation run(IProgressMonitor monitor) {
		try {
			updateStatusMessage();
			isExecuted = true;
			if (consoleStream != null) {
				consoleStream.markStart(name);
			}
			runImpl(monitor);
			if (monitor.isCanceled()) {
				boolean hasCanceledException = false;
				// We have a multi status, so we iterate the children
				IStatus[] children = status.getChildren();
				for (IStatus child : children) {
					Throwable exception = child.getException();
					if (exception instanceof SVNConnectorCancelException
							|| exception instanceof ActivityCancelledException
							|| exception instanceof OperationCanceledException) {
						hasCanceledException = true;
						break;
					}
				}
				if (!hasCanceledException) {
					throw new ActivityCancelledException();
				}
			}
		} catch (Throwable t) {
			this.reportStatus(IStatus.ERROR, null, t);
		} finally {
			if (consoleStream != null) {
				consoleStream.markEnd();
			}
		}

		return this;
	}

	public void setOperationName(String name) {
		nameId = name;
		this.name = getNationalizedString(name);
	}

	@Override
	public String getOperationName() {
		return name;
	}

	@Override
	public String getId() {
		return nameId;
	}

	@Override
	public Class<? extends NLS> getMessagesClass() {
		return messagesClass;
	}

	protected abstract void runImpl(IProgressMonitor monitor) throws Exception;

	protected void writeCancelledToConsole() {
		if (consoleStream != null) {
			consoleStream.markCancelled();
		}
	}

	protected void writeToConsole(int severity, String data) {
		if (consoleStream != null) {
			consoleStream.write(severity, data);
		}
	}

	protected void complexWriteToConsole(Runnable runnable) {
		if (consoleStream != null) {
			consoleStream.doComplexWrite(runnable);
		}
	}

	protected void protectStep(IUnprotectedOperation step, IProgressMonitor monitor, int subTaskCnt) {
		this.protectStep(step, monitor, IActionOperation.DEFAULT_WEIGHT * subTaskCnt, IActionOperation.DEFAULT_WEIGHT);
	}

	protected void protectStep(IUnprotectedOperation step, IProgressMonitor monitor, int totalWeight,
			int currentWeight) {
		try {
			ProgressMonitorUtility.doSubTask(this, step, monitor, totalWeight, currentWeight);
		} catch (Throwable t) {
			this.reportStatus(IStatus.ERROR, null, t);
		}
	}

	protected void reportWarning(String message, Throwable t) {
		this.reportStatus(IStatus.WARNING, message, t);
	}

	protected void reportError(Throwable t) {
		this.reportStatus(IStatus.ERROR, null, t);
	}

	@Override
	public void reportStatus(int severity, String message, Throwable t) {
		String msg = message != null ? message : getShortErrorMessage(t);
		if (severity == IStatus.ERROR) {
			if (t instanceof SVNConnectorCancelException || t instanceof ActivityCancelledException
					|| t instanceof OperationCanceledException) {
				writeCancelledToConsole();
			} else {
				writeToConsole(IConsoleStream.LEVEL_ERROR, msg + "\n"); //$NON-NLS-1$
			}
		} else if (severity == IStatus.WARNING) {
			writeToConsole(IConsoleStream.LEVEL_WARNING, message + "\n");
		}
		this.reportStatus(new Status(severity, SVNTeamPlugin.NATURE_ID, IStatus.OK, msg, t));
	}

	protected String getShortErrorMessage(Throwable t) {
		String key = nameId + "_Error"; //$NON-NLS-1$
		String retVal = getNationalizedString(key);
		if (retVal.equals(key)) {
			return status.getMessage() + ": " + (t == null ? "<null>" : t.getMessage()); //$NON-NLS-1$ $NON-NLS-2$
		}
		return retVal;
	}

	protected void reportStatus(IStatus st) {
		if (st.getSeverity() != IStatus.OK) {
			status.merge(st);
		}
	}

	protected String getOperationResource(String key) {
		return getNationalizedString(nameId + "_" + key); //$NON-NLS-1$
	}

	protected final String getNationalizedString(String key) {
		String retVal = BaseMessages.getErrorString(key, messagesClass);
		if (retVal.equals(key)) {
			return CoreExtensionsManager.instance().getOptionProvider().getResource(key);
		}
		return retVal;
	}

	private void updateStatusMessage() {
		String key = nameId + "_Id"; //$NON-NLS-1$
		String prefix = getNationalizedString(key);
		prefix = prefix.equals(key) ? "" : prefix + ": "; //$NON-NLS-1$ //$NON-NLS-2$
		String errMessage = BaseMessages.format(SVNMessages.Operation_Error_LogHeader, new String[] { prefix + name });
		status.setMessage(errMessage);
	}

	/**
	 * Give us an accessible version of MultiStatus to be able to update the message.
	 * 
	 * @author Alessandro Nistico
	 */
	private static class MultiStatus extends org.eclipse.core.runtime.MultiStatus {
		public MultiStatus(String pluginId, int code, IStatus[] newChildren, String message, Throwable exception) {
			super(pluginId, code, newChildren, message, exception);
		}

		public MultiStatus(String pluginId, int code, String message, Throwable exception) {
			super(pluginId, code, message, exception);
		}

		@Override
		public void setMessage(String message) {
			super.setMessage(message);
		}

		// The severity is set automatically by org.eclipse.core.runtime.MultiStatus.add() method which is called every time child status is added. So we don't need to override the severity field manually.
	}

}
