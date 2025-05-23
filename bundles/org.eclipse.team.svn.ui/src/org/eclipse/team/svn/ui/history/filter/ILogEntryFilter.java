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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history.filter;

import org.eclipse.team.svn.core.connector.SVNLogEntry;

/**
 * Interface of an ILogMessage filter. Filters to be added to {@link CompositeLogEntryFilter} must implement this interface.
 * 
 * @author Alexei Goncharov
 */
public interface ILogEntryFilter {

	/**
	 * Returns if the filter accepts the given log node.
	 * 
	 * @param logEntry
	 *            - log entry to check
	 * @return - true if accepted, otherwise false
	 */
	boolean accept(SVNLogEntry logEntry);
}