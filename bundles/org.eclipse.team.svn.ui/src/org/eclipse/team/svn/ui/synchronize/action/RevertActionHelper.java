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
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Synchronize view revert action helper
 * 
 * @author Igor Burilo
 */
public class RevertActionHelper extends AbstractActionHelper {
	
	public RevertActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	public FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			public boolean select(SyncInfo info) {
				return org.eclipse.team.svn.ui.action.local.RevertAction.SF_REVERTABLE_OR_NEW.accept(((AbstractSVNSyncInfo)info).getLocalResource());
			}
		};
	}
	
	public IActionOperation getOperation() {
		IResource []changedResources = this.getSyncInfoSelector().getSelectedResources();
		return org.eclipse.team.svn.ui.action.local.RevertAction.getRevertOperation(this.configuration.getSite().getShell(), changedResources, changedResources);
	}

}
