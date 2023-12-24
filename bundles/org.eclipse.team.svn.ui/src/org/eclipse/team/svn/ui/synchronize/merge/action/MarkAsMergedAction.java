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
 *    Alexander Gurov - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.merge.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNConflictResolution;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.MarkResolvedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.operation.ClearMergeStatusesOperation;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Mark as merged action
 * 
 * @author Alexander Gurov
 */
public class MarkAsMergedAction extends AbstractSynchronizeModelAction {
	public MarkAsMergedAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] {SyncInfo.CONFLICTING}) {
            public boolean select(SyncInfo info) {
                return super.select(info) && !IStateFilter.SF_OBSTRUCTED.accept(((AbstractSVNSyncInfo)info).getLocalResource()) && IStateFilter.SF_VERSIONED.accept(((AbstractSVNSyncInfo)info).getLocalResource());
            }
        };
	}

	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IResource []resources = this.syncInfoSelector.getSelectedResources();

		MarkResolvedOperation mainOp = new MarkResolvedOperation(resources, SVNConflictResolution.Choice.CHOOSE_MERGED, SVNDepth.INFINITY);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(mainOp);
		op.add(new RefreshResourcesOperation(FileUtility.getParents(resources, false)));
		op.add(new ClearMergeStatusesOperation(resources));
		return op;
	}

}
