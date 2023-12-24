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
import org.eclipse.team.svn.core.connector.SVNLogPath;
import org.eclipse.team.svn.core.utility.StringMatcher;

/**
 * Changed path log entries filter
 * 
 * @author Alexei Goncharov
 */
public class ChangeNameLogEntryFilter implements ILogEntryFilter {

	protected String changedPathToAccept;

	public ChangeNameLogEntryFilter() {
		this(null);
	}

	public ChangeNameLogEntryFilter(String changedPathToAccept) {
		this.changedPathToAccept = changedPathToAccept;
	}

	@Override
	public boolean accept(SVNLogEntry logEntry) {
		if (changedPathToAccept == null) {
			return true;
		}
		StringMatcher matcher = new StringMatcher(changedPathToAccept);
		SVNLogPath[] paths = logEntry.changedPaths == null ? new SVNLogPath[0] : logEntry.changedPaths;
		for (SVNLogPath path : paths) {
			if (matcher.match(path.path)) {
				return true;
			}
		}
		return false;
	}

	public void setGangedPathToAccept(String changedPathToAccept) {
		this.changedPathToAccept = changedPathToAccept;
	}

	public String getGangedPathToAccept() {
		return changedPathToAccept;
	}

}