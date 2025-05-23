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

import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.synchronize.action.AbstractActionHelper;
import org.eclipse.team.svn.ui.utility.LockProposeUtility;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Unlock action helper implementation for Synchronize View
 * 
 * @author Igor Burilo
 */
public class UnlockActionHelper extends AbstractActionHelper {

	public UnlockActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	@Override
	public FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			@Override
			public boolean select(SyncInfo info) {
				return super.select(info)
						&& IStateFilter.SF_LOCKED.accept(((AbstractSVNSyncInfo) info).getLocalResource());
			}
		};
	}

	@Override
	public IActionOperation getOperation() {
		return LockProposeUtility.performUnlockAction(getSyncInfoSelector().getSelectedResources(),
				configuration.getSite().getShell());
	}

}
