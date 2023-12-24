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

package org.eclipse.team.svn.ui.synchronize.action.logicalmodel;

import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Set keywords logical model action implementation for Synchronize view
 * 
 * @author Igor Burilo
 */
public class SetKeywordsModelAction extends AbstractSynchronizeLogicalModelAction {

	public SetKeywordsModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			@Override
			public boolean select(SyncInfo info) {
				return super.select(info)
						&& IStateFilter.SF_VERSIONED_FILES.accept(((AbstractSVNSyncInfo) info).getLocalResource());
			}
		};
	}

	@Override
	protected IActionOperation getOperation() {
		org.eclipse.team.svn.ui.action.local.SetKeywordsAction.doSetKeywords(syncInfoSelector.getSelectedResources());
		return null;
	}

}
