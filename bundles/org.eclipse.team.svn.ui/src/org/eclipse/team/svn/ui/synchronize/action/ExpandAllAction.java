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
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Expand All Action
 * 
 * @author Sergiy Logvin
 */
public class ExpandAllAction extends AbstractSynchronizeModelAction {
	public ExpandAllAction(String text, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected IActionOperation getOperation(final ISynchronizePageConfiguration configuration,
			IDiffElement[] elements) {
		return new AbstractActionOperation("Operation_UExpandAll", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				UIMonitorUtility.getDisplay().syncExec(() -> {
					Viewer viewer = configuration.getPage().getViewer();
					if (viewer == null || viewer.getControl().isDisposed()
							|| !(viewer instanceof AbstractTreeViewer)) {
						return;
					}
					viewer.getControl().setRedraw(false);
					((AbstractTreeViewer) viewer).expandAll();
					viewer.getControl().setRedraw(true);
				});
			}
		};
	}

}
