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

import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.operation.ShowConflictEditorOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Edit conflicts action helper implementation for Synchronize view
 * 
 * @author Igor Burilo
 */
public class EditConflictsActionHelper extends AbstractActionHelper {

	public EditConflictsActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	@Override
	public FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] { SyncInfo.CONFLICTING }) {
			@Override
			public boolean select(SyncInfo info) {
				return super.select(info)
						&& IStateFilter.SF_DATA_CONFLICTING.accept(((AbstractSVNSyncInfo) info).getLocalResource());
			}
		};
	}

	@Override
	public IActionOperation getOperation() {
		return new ShowConflictEditorOperation(
				getSyncInfoSelector().getSelectedResources(IStateFilter.SF_DATA_CONFLICTING), false);
	}

}
