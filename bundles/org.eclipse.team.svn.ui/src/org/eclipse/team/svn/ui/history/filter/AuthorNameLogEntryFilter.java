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
import org.eclipse.team.svn.core.utility.StringMatcher;

/**
 * Author filter for log entry
 * 
 * @author Alexei Goncharov
 */
public class AuthorNameLogEntryFilter implements ILogEntryFilter {
	
	protected String authorNameToAccept;
	
	public AuthorNameLogEntryFilter() {
		this(null);
	}
	
	public AuthorNameLogEntryFilter (String authorNameToAccept) {
		this.authorNameToAccept = authorNameToAccept;
	}

	public boolean accept(SVNLogEntry logEntry) {
		if (this.authorNameToAccept == null) {
			return true;
		}
		StringMatcher matcher = new StringMatcher(this.authorNameToAccept);
		String authorName = logEntry.author == null ? "" : logEntry.author; //$NON-NLS-1$
		return matcher.match(authorName);
	}

	public void setAuthorNameToAccept(String authorNameToAccept) {
		this.authorNameToAccept = authorNameToAccept;
	}
	
	public String getAuthorNameToAccept() {
		return this.authorNameToAccept;
	}
}
