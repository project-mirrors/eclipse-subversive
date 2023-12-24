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
 * Comment filter for log entry
 * 
 * @author Alexei Goncharov
 */
public class CommentLogEntryFilter implements ILogEntryFilter {

	protected String commentToAccept;

	public CommentLogEntryFilter() {
		this(null);
	}

	public CommentLogEntryFilter(String commentToAccept) {
		this.commentToAccept = commentToAccept;
	}

	public boolean accept(SVNLogEntry logEntry) {
		if (this.commentToAccept == null) {
			return true;
		}
		StringMatcher matcher = new StringMatcher(this.commentToAccept);
		String comment = logEntry.message == null ? "" : logEntry.message; //$NON-NLS-1$
		return matcher.match(comment);
	}

	public void setCommentToAccept(String commentToAccept) {
		this.commentToAccept = commentToAccept;
	}

	public String getCommentToAccept() {
		return this.commentToAccept;
	}
}
