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

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.operation.ShowHistoryViewOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Show resource history logical model action helper
 * 
 * @author Igor Burilo
 */
public class ShowHistoryActionHelper extends AbstractActionHelper {

	public ShowHistoryActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	@Override
	public IActionOperation getOperation() {
		/*
		 * If resource exists locally, then show history for local resource
		 * (even if there are incoming changes),
		 * otherwise show history for remote resource
		 */
		IResource resource = getSelectedResource();
		if (IStateFilter.SF_VERSIONED.accept(SVNRemoteStorage.instance().asLocalResource(resource))) {
			return new ShowHistoryViewOperation(resource, 0, 0);
		} else {
			AbstractSVNSyncInfo info = getSelectedSVNSyncInfo();
			if (info != null) {
				ILocalResource incoming = info.getRemoteChangeResource();
				if (incoming instanceof IResourceChange) {
					return new ShowHistoryViewOperation(((IResourceChange) incoming).getOriginator(), 0, 0);
				}
			}
		}
		return new ShowHistoryViewOperation(getSelectedResource(), 0, 0);
	}

}
