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

import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;

/**
 * Instantiates HistoryView's
 * 
 * @author Alexander Gurov
 */
public class SVNHistoryPageSource extends HistoryPageSource {

	public boolean canShowHistoryFor(Object object) {
		return SVNHistoryPage.isValidData(object);
	}

	public Page createPage(Object object) {
		return new SVNHistoryPage(object);
	}

}
