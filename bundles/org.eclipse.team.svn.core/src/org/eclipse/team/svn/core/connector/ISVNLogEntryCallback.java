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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.connector;

/**
 * LogEntry information call-back interface
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public interface ISVNLogEntryCallback {
	/**
	 * The method will be called by a connector library for every reported log entry.
	 * 
	 * @param log
	 *            the reported log entry instance, log.hasChildren() is <code>true</code> if merge-related entries will be reported after
	 *            that entry. Termination of the children sequence is a log entry which revision field is set to the
	 *            SVNRevision.INVALID_REVISION_NUMBER value. Children could be added to parent log entry if it is required. Also please note
	 *            that children could be reported recursively.
	 */
	void next(SVNLogEntry log);
}
