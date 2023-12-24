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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.ui.history.model.ILogNode;
import org.eclipse.team.ui.history.IHistoryPageSource;

/**
 * SVN HistoryPage adapter factory
 * 
 * @author Alexander Gurov
 */
public class SVNHistoryAdapterFactory implements IAdapterFactory {
	private static final Class []ADAPTED_TYPES = new Class[] {IHistoryPageSource.class, SVNLogEntry.class};
	
	private SVNHistoryPageSource pageSource = new SVNHistoryPageSource();
	
	public Class[] getAdapterList() {
		return SVNHistoryAdapterFactory.ADAPTED_TYPES;
	}

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (IHistoryPageSource.class.equals(adapterType)) {
			return this.pageSource;
		}
		if (adaptableObject instanceof ILogNode) {
			return ((ILogNode)adaptableObject).getAdapter(adapterType);
		}
		return null;
	}

}
