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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/
package org.eclipse.team.svn.core.utility;

import java.util.ArrayList;
import java.util.Stack;

import org.eclipse.team.svn.core.connector.ISVNLogEntryCallback;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;

/**
 * LogEntry call-back implementation which includes merge info
 * 
 * @author Igor Burilo
 */
public class SVNLogEntryCallbackWithMergeInfo implements ISVNLogEntryCallback {

	protected Stack<SVNLogEntry> mergeTreeBuilder = new Stack<>();

	protected ArrayList<SVNLogEntry> entries = new ArrayList<>();

	@Override
	public void next(SVNLogEntry log) {
		if (log.revision == SVNRevision.INVALID_REVISION_NUMBER) {
			if (!mergeTreeBuilder.isEmpty()) {
				log = mergeTreeBuilder.pop();
				if (mergeTreeBuilder.isEmpty()) {
					addEntry(log);
				}
			}
			return;
		}

		if (!mergeTreeBuilder.isEmpty()) {
			addChildEntry(mergeTreeBuilder.peek(), log);
		} else if (!log.hasChildren()) {
			addEntry(log);
		}
		if (log.hasChildren()) {
			mergeTreeBuilder.push(log);
		}
	}

	/*
	 * Can be overridden in sub classes
	 */
	protected void addEntry(SVNLogEntry entry) {
		entries.add(entry);
	}

	/*
	 * Can be overridden in sub classes
	 */
	protected void addChildEntry(SVNLogEntry parent, SVNLogEntry child) {
		parent.add(child);
	}

	public SVNLogEntry[] getEntries() {
		return entries.toArray(new SVNLogEntry[entries.size()]);
	}
}
