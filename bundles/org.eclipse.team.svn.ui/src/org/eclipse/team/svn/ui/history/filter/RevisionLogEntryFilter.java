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
 * Log entry filter to remove revisions from view
 * 
 * @author Alexei Goncharov
 */
public class RevisionLogEntryFilter implements ILogEntryFilter {

	protected long from;

	protected long to;

	@Override
	public boolean accept(SVNLogEntry logEntry) {
		long current = logEntry.revision;
		return current > to || current < from;
	}

	/**
	 * Sets revisions range to exclude
	 * 
	 * @param from
	 *            - lower revision
	 * @param to
	 *            - higher revision
	 */
	public void setRevisionstoHide(long from, long to) {
		this.from = from;
		this.to = to;
	}

}
