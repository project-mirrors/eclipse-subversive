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

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.ResourcesTraversalOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.RevertOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.change.visitors.RemoveNonVersionedVisitor;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.action.AbstractRecursiveTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.RevertPanel;

/**
 * Team services menu revert action implementation
 * 
 * @author Alexander Gurov
 */
public class RevertAction extends AbstractRecursiveTeamAction {

	public RevertAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource[] changedResources = this.getSelectedResourcesRecursive(RevertAction.SF_REVERTABLE_OR_NEW);
		IResource[] userSelectedResources = this.getSelectedResources();
		CompositeOperation revertOp = RevertAction.getRevertOperation(getShell(), changedResources,
				userSelectedResources);
		if (revertOp != null) {
			runScheduled(revertOp);
		}
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresenceRecursive(RevertAction.SF_REVERTABLE_OR_NEW);
	}

	public static CompositeOperation getRevertOperation(Shell shell, IResource[] changedResources,
			IResource[] selectedResources) {
		RevertPanel panel = new RevertPanel(changedResources, selectedResources);
		DefaultDialog rDlg = new DefaultDialog(shell, panel);
		if (rDlg.open() == 0) {
			boolean recursive = panel.getNotSelectedResources().length == 0;
			changedResources = panel.getSelectedResources();
			IResource[] revertableResources = FileUtility.getResourcesRecursive(changedResources,
					IStateFilter.SF_REVERTABLE, recursive ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO);

			RevertOperation mainOp = new RevertOperation(revertableResources, recursive);

			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

			SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(changedResources);
			RestoreProjectMetaOperation restoreOp = new RestoreProjectMetaOperation(saveOp);

			op.add(saveOp);
			op.add(mainOp);
			if (panel.getRemoveNonVersioned()) {
				op.add(new RefreshResourcesOperation(selectedResources), new IActionOperation[] { mainOp });
				op.add(new ResourcesTraversalOperation("Operation_RemoveNonSVN", SVNMessages.class, changedResources,
						new RemoveNonVersionedVisitor(true), IResource.DEPTH_INFINITE),
						new IActionOperation[] { mainOp });
			}
			op.add(restoreOp);
			op.add(new RefreshResourcesOperation(selectedResources));
			return op;
		}

		return null;
	}

	public static IStateFilter SF_REVERTABLE_OR_NEW = new IStateFilter.AbstractStateFilter() {

		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_REVERTABLE.accept(resource, state, mask)
					|| IStateFilter.SF_NEW.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_REVERTABLE.allowsRecursion(resource, state, mask)
					|| IStateFilter.SF_NEW.allowsRecursion(resource, state, mask);
		}

	};

}
