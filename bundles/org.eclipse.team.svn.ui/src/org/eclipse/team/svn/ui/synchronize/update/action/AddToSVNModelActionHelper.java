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

package org.eclipse.team.svn.ui.synchronize.update.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNWithPropertiesOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.synchronize.UpdateSyncInfo;
import org.eclipse.team.svn.ui.action.QueryResourceAddition;
import org.eclipse.team.svn.ui.synchronize.action.AbstractActionHelper;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Synchronize view add to version control action helper
 * 
 * @author Igor Burilo
 */
public class AddToSVNModelActionHelper extends AbstractActionHelper {

	public AddToSVNModelActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	@Override
	public FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] { SyncInfo.OUTGOING, SyncInfo.CONFLICTING }) {
			@Override
			public boolean select(SyncInfo info) {
				UpdateSyncInfo sync = (UpdateSyncInfo) info;
				return super.select(info) && (IStateFilter.SF_NEW.accept(sync.getLocalResource())
						|| IStateFilter.SF_IGNORED.accept(sync.getLocalResource()));
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.ui.synchronize.action.AbstractActionHelper#getOperation()
	 */
	@Override
	public IActionOperation getOperation() {
		QueryResourceAddition query = new QueryResourceAddition(getSyncInfoSelector(),
				configuration.getSite().getShell());
		IResource[] resources = query.queryAddition();
		if (resources == null) {
			return null;
		}
		AddToSVNWithPropertiesOperation mainOp = new AddToSVNWithPropertiesOperation(resources, false);

		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

		op.add(mainOp);
		op.add(new RefreshResourcesOperation(
				resources/*, IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL*/));

		return op;
	}

}
