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

package org.eclipse.team.svn.ui.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.history.data.SVNLocalFileRevision;

/**
 * Provides info about history view modes
 * 
 * @author Alexander Gurov
 */
public interface ISVNHistoryViewInfo {
	int MODE_BOTH = 0x20;

	int MODE_LOCAL = 0x40;

	int MODE_REMOTE = 0x80;

	int MODE_MASK = ISVNHistoryViewInfo.MODE_BOTH | ISVNHistoryViewInfo.MODE_REMOTE | ISVNHistoryViewInfo.MODE_LOCAL;

	IResource getResource();

	IRepositoryResource getRepositoryResource();

	long getCurrentRevision();

	boolean isPending();

	boolean isRelatedPathsOnly();

	boolean isGrouped();

	int getMode();

	SVNLocalFileRevision[] getLocalHistory();

	SVNLogEntry[] getRemoteHistory();
}
