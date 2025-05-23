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

package org.eclipse.team.svn.ui.synchronize.update.action.logicalmodel;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.synchronize.action.logicalmodel.AbstractModelToolbarAction;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Expand All logical model action
 * 
 * @author Igor Burilo
 */
public class ExpandAllModelAction extends AbstractModelToolbarAction {

	public ExpandAllModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	@Override
	protected IActionOperation getOperation() {
		return new AbstractActionOperation("Operation_UExpandAll", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				UIMonitorUtility.getDisplay().syncExec(() -> {
					Viewer viewer = ExpandAllModelAction.this.getConfiguration().getPage().getViewer();
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
