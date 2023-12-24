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
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;

/**
 * Action operation interface
 * 
 * @author Alexander Gurov
 */
public interface IActionOperation {
	int OK = 0;

	int ERROR = 1;

	int NOTEXECUTED = 2;

	int DEFAULT_WEIGHT = 1;

	IActionOperation run(IProgressMonitor monitor);

	IStatus getStatus();

	void reportStatus(int severity, String message, Throwable t);

	int getExecutionState();

	String getOperationName();

	int getOperationWeight();

	String getId();

	Class<? extends NLS> getMessagesClass();

	ISchedulingRule getSchedulingRule();

	void setConsoleStream(IConsoleStream stream);

	IConsoleStream getConsoleStream();
}
