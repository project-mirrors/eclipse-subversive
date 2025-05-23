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

package org.eclipse.team.svn.ui.utility;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.LockOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.UnlockOperation;
import org.eclipse.team.svn.core.operation.remote.BreakLockOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.lock.LockResource;
import org.eclipse.team.svn.ui.lock.LockResourcesPanel;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;

/**
 * An utility to propose user to lock the file if it needs lock
 * 
 * @author Sergiy Logvin
 */
public class LockProposeUtility {
	public static IStatus proposeLock(final IResource[] resources, Shell shell, boolean fromEditor) {
		CompositeOperation op = LockProposeUtility.performLockAction(resources, false, shell, fromEditor);
		if (op != null) {
			UIMonitorUtility.doTaskBusyWorkspaceModify(op);
			return op.getStatus();
		}
		String msg = BaseMessages.format(SVNUIMessages.ErrorCancelPanel_Description_Cancelled,
				SVNUIMessages.LocksComposite_LockTitle);
		return new Status(IStatus.CANCEL, SVNTeamPlugin.NATURE_ID, IStatus.CANCEL, msg, null);
	}

	public static IResource[] asResources(LockResource[] lockResources) {
		List<IResource> res = new ArrayList<>();
		if (lockResources != null) {
			for (LockResource element : lockResources) {
				Object ob = element.getAdapter(IResource.class);
				if (ob != null) {
					res.add((IResource) ob);
				}
			}
		}
		return res.toArray(new IResource[res.size()]);
	}

	public static IRepositoryResource[] asRepositoryResources(LockResource[] lockResources) {
		List<IRepositoryResource> res = new ArrayList<>();
		if (lockResources != null) {
			for (LockResource element : lockResources) {
				Object ob = element.getAdapter(IRepositoryResource.class);
				if (ob != null) {
					res.add((IRepositoryResource) ob);
				}
			}
		}
		return res.toArray(new IRepositoryResource[res.size()]);
	}

	public static CompositeOperation performLockAction(IResource[] resourcesToProcess, boolean forceLock, Shell shell) {
		return LockProposeUtility.performLockAction(resourcesToProcess, forceLock, shell, false);
	}

	public static CompositeOperation performLockAction(IResource[] resourcesToProcess, boolean forceLock, Shell shell,
			boolean fromEditor) {
		LockResource[] lockResources = LockResource.getLockResources(resourcesToProcess, false);
		return LockProposeUtility.performLockAction(lockResources, forceLock, shell, fromEditor);
	}

	public static CompositeOperation performLockAction(LockResource[] lockResources, boolean forceLock, Shell shell) {
		return LockProposeUtility.performLockAction(lockResources, forceLock, shell, false);
	}

	public static CompositeOperation performLockAction(LockResource[] lockResources, boolean forceLock, Shell shell,
			boolean fromEditor) {
		LockResourcesPanel panel = new LockResourcesPanel(lockResources, true, forceLock,
				SVNUIMessages.LocksComposite_LockTitle, SVNUIMessages.LocksComposite_LockDescription,
				SVNUIMessages.LocksComposite_LockDefaultMessage);
		DefaultDialog dlg = new DefaultDialog(shell, panel);
		if (dlg.open() == 0) {
			IResource[] resources = asResources(panel.getSelectedResources());

			LockOperation mainOp = new LockOperation(resources, panel.getMessage(), panel.getForce());
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			op.add(mainOp);
			op.add(new RefreshResourcesOperation(resources, fromEditor));
			return op;
		}
		return null;
	}

	public static CompositeOperation performUnlockAction(IResource[] resourcesToProcess, Shell shell) {
		LockResource[] lockResources = LockResource.getLockResources(resourcesToProcess, true);
		return LockProposeUtility.performUnlockAction(lockResources, shell);
	}

	public static CompositeOperation performUnlockAction(LockResource[] lockResources, Shell shell) {
		LockResourcesPanel unlockPanel = new LockResourcesPanel(lockResources, SVNUIMessages.LocksComposite_UnlockTitle,
				SVNUIMessages.LocksComposite_UnlockDescription, SVNUIMessages.LocksComposite_UnlockDefaultMessage);
		DefaultDialog dlg = new DefaultDialog(shell, unlockPanel);
		if (dlg.open() == 0) {
			IResource[] resources = asResources(unlockPanel.getSelectedResources());
			UnlockOperation mainOp = new UnlockOperation(resources);
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			op.add(mainOp);
			op.add(new RefreshResourcesOperation(resources));
			return op;
		}
		return null;
	}

	public static CompositeOperation performBreakLockAction(LockResource[] lockResources, Shell shell) {
		LockResourcesPanel panel = new LockResourcesPanel(lockResources, SVNUIMessages.LocksComposite_BreakLockTitle,
				SVNUIMessages.LocksComposite_BreakLockDescription,
				SVNUIMessages.LocksComposite_BreakLockDefaultMessage);
		DefaultDialog dlg = new DefaultDialog(shell, panel);
		if (dlg.open() == 0) {
			IRepositoryResource[] reposResources = asRepositoryResources(panel.getSelectedResources());
			BreakLockOperation mainOp = new BreakLockOperation(reposResources);
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			op.add(mainOp);
			op.add(new RefreshRemoteResourcesOperation(reposResources));
			return op;
		}
		return null;
	}

}
