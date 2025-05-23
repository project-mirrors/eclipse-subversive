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

package org.eclipse.team.svn.core.operation;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.connector.SVNNotification.NodeStatus;
import org.eclipse.team.svn.core.connector.SVNNotification.PerformedAction;

/**
 * SVN conflict detection progress monitor
 * 
 * @author Igor Burilo
 */
public abstract class SVNConflictDetectionProgressMonitor extends SVNProgressMonitor {

	public SVNConflictDetectionProgressMonitor(IActionOperation parent, IProgressMonitor monitor, IPath root) {
		super(parent, monitor, root);
	}

	public SVNConflictDetectionProgressMonitor(IActionOperation parent, IProgressMonitor monitor, IPath root,
			boolean enableConsoleOutput) {
		super(parent, monitor, root, enableConsoleOutput);
	}

	@Override
	public void progress(int current, int total, ItemState state) {
		super.progress(current, total, state);
		if (state.contentState == NodeStatus.CONFLICTED.id || state.propState == NodeStatus.CONFLICTED.id
				|| state.action == PerformedAction.TREE_CONFLICT.id) {
			processConflict(state);
		}
	}

	protected abstract void processConflict(ItemState state);
}
