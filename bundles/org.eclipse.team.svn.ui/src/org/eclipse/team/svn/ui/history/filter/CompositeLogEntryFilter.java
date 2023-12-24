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

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.team.svn.core.connector.SVNLogEntry;

/**
 * Composite log entries filter.
 * 
 * @author Alexei Goncharov
 */
public class CompositeLogEntryFilter implements ILogEntryFilter {

	protected HashSet<ILogEntryFilter> filtersSet;

	public CompositeLogEntryFilter() {
		filtersSet = new HashSet<>();
	}

	public CompositeLogEntryFilter(ILogEntryFilter[] filters) {
		filtersSet = new HashSet<>(Arrays.asList(filters));
	}

	@Override
	public boolean accept(SVNLogEntry logEntry) {
		for (ILogEntryFilter current : filtersSet) {
			if (!current.accept(logEntry)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds a filter to filters set
	 */
	public void addFilter(ILogEntryFilter filter) {
		filtersSet.add(filter);
	}

	/**
	 * Removes a filter from filters set
	 */
	public void removeFilter(ILogEntryFilter filter) {
		filtersSet.remove(filter);
	}
}
