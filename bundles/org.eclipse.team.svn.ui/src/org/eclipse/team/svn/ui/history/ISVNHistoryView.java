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
import org.eclipse.team.ui.history.HistoryPage;

/**
 * Allow to manage history view
 * 
 * @author Alexander Gurov
 */
public interface ISVNHistoryView extends ISVNHistoryViewInfo {
	int REFRESH_VIEW = 0;

	int REFRESH_LOCAL = 1;

	int REFRESH_REMOTE = 2;

	int REFRESH_ALL = 3;

	int PAGING_ENABLED = 0x01;

	int COMPARE_MODE = 0x02;

	int HIDE_UNRELATED = 0x04;

	int STOP_ON_COPY = 0x08;

	int GROUP_BY_DATE = 0x10;
	/* 0x20, 0x40 and 0x80 are reserved for LogMessagesComposite */

	HistoryPage getHistoryPage();

	IResource getCompareWith();

	SVNLogEntry[] getFullRemoteHistory();

	boolean isAllRemoteHistoryFetched();

	void clearFilter();

	void setFilter();

	void refresh(int refreshType);

	boolean isFilterEnabled();

	int getOptions();
}