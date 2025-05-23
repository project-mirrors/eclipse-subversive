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

package org.eclipse.team.svn.ui.utility;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * UI Monitor Utility class
 * 
 * @author Alexander Gurov
 */
public final class UIMonitorUtility {
	public static final IOperationWrapperFactory DEFAULT_FACTORY = new DefaultOperationWrapperFactory();

	public static final IOperationWrapperFactory WORKSPACE_MODIFY_FACTORY = new WorkspaceModifyOperationWrapperFactory();

	public static void parallelSyncExec(Runnable runnable) {
		// requires additional investigation: is it possible to deadlock on UI synch mutex here ?
		UIMonitorUtility.getDisplay().syncExec(runnable);
	}

	public static Display getDisplay() {
		Display retVal = Display.getCurrent();
		if (retVal == null || retVal.isDisposed()) {
			IWorkbench workbench = PlatformUI.getWorkbench();
			if (workbench != null) {
				retVal = workbench.getDisplay();
			}
			if (retVal == null || retVal.isDisposed()) {
				retVal = Display.getDefault();
			}
		}
		return retVal;
	}

	public static Shell getShell() {
		return UIMonitorUtility.getShell(null);
	}

	public static Shell getShell(IWorkbenchSite site) {
		if (site != null) {
			Shell shell = site.getShell();
			if (!shell.isDisposed()) {
				return shell;
			}
		}
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench != null) {
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window != null) {
				return window.getShell();
			}
		}
		Display display = UIMonitorUtility.getDisplay();
		return new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	public static ICancellableOperationWrapper doTaskScheduledWorkspaceModify(IWorkbenchPart part,
			IActionOperation op) {
		return UIMonitorUtility.doTaskScheduled(part, op, UIMonitorUtility.WORKSPACE_MODIFY_FACTORY);
	}

	public static ICancellableOperationWrapper doTaskScheduledDefault(IWorkbenchPart part, IActionOperation op) {
		return UIMonitorUtility.doTaskScheduled(part, op, UIMonitorUtility.DEFAULT_FACTORY);
	}

	public static ICancellableOperationWrapper doTaskScheduled(IWorkbenchPart part, IActionOperation op,
			IOperationWrapperFactory factory) {
		ICancellableOperationWrapper runnable = factory.getCancellable(factory.getLogged(op));

		try {
			new SVNTeamOperationWrapper(part, runnable).run();
		} catch (InterruptedException e) {
			runnable.setCancelled(true);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		return runnable;
	}

	public static ICancellableOperationWrapper doTaskScheduledWorkspaceModify(IActionOperation op) {
		return UIMonitorUtility.doTaskScheduled(op, UIMonitorUtility.WORKSPACE_MODIFY_FACTORY);
	}

	public static ICancellableOperationWrapper doTaskScheduledDefault(IActionOperation op) {
		return UIMonitorUtility.doTaskScheduled(op, UIMonitorUtility.DEFAULT_FACTORY);
	}

	public static ICancellableOperationWrapper doTaskScheduledActive(IActionOperation op) {
		IWorkbenchPart activePart = UIMonitorUtility.getActivePart();
		if (activePart != null) {
			return UIMonitorUtility.doTaskScheduled(activePart, op, UIMonitorUtility.DEFAULT_FACTORY);
		}
		return UIMonitorUtility.doTaskScheduledDefault(op);
	}

	public static IWorkbenchPart getActivePart() {
		IWorkbenchPage activePage = UIMonitorUtility.getActivePage();
		return activePage == null ? null : activePage.getActivePart();
	}

	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window = SVNTeamUIPlugin.instance().getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] ws = SVNTeamUIPlugin.instance().getWorkbench().getWorkbenchWindows();
			window = ws != null && ws.length > 0 ? ws[0] : null;
		}
		return window == null ? null : window.getActivePage();
	}

	public static ICancellableOperationWrapper doTaskScheduled(IActionOperation op, IOperationWrapperFactory factory) {
		ICancellableOperationWrapper runnable = factory.getCancellable(factory.getLogged(op));

		new ScheduledOperationWrapper(runnable).schedule();

		return runnable;
	}

	public static ICancellableOperationWrapper doTaskNowWorkspaceModify(IActionOperation op, boolean cancellable) {
		return UIMonitorUtility.doTaskNowWorkspaceModify(UIMonitorUtility.getShell(), op, cancellable);
	}

	public static ICancellableOperationWrapper doTaskNowWorkspaceModify(Shell shell, IActionOperation op,
			boolean cancellable) {
		return UIMonitorUtility.doTaskNow(shell, op, cancellable, UIMonitorUtility.WORKSPACE_MODIFY_FACTORY);
	}

	public static ICancellableOperationWrapper doTaskNowDefault(IActionOperation op, boolean cancellable) {
		return UIMonitorUtility.doTaskNowDefault(UIMonitorUtility.getShell(), op, cancellable);
	}

	public static ICancellableOperationWrapper doTaskNowDefault(Shell shell, IActionOperation op, boolean cancellable) {
		return UIMonitorUtility.doTaskNow(shell, op, cancellable, UIMonitorUtility.DEFAULT_FACTORY);
	}

	public static ICancellableOperationWrapper doTaskNow(Shell shell, IActionOperation op, boolean cancellable,
			IOperationWrapperFactory factory) {
		ICancellableOperationWrapper runnable = factory.getCancellable(factory.getLogged(op));
		try {
			new ProgressMonitorDialog(shell).run(true, cancellable, runnable);
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return runnable;
	}

	public static ICancellableOperationWrapper doTaskBusyDefault(IActionOperation op) {
		return UIMonitorUtility.doTaskBusy(op, UIMonitorUtility.DEFAULT_FACTORY);
	}

	public static ICancellableOperationWrapper doTaskBusyWorkspaceModify(IActionOperation op) {
		return UIMonitorUtility.doTaskBusy(op, UIMonitorUtility.WORKSPACE_MODIFY_FACTORY);
	}

	public static ICancellableOperationWrapper doTaskBusy(IActionOperation op, IOperationWrapperFactory factory) {
		final ICancellableOperationWrapper runnable = factory.getCancellable(factory.getLogged(op));
		BusyIndicator.showWhile(null, () -> {
			try {
				runnable.run(new NullProgressMonitor());
			} catch (InterruptedException e) {
				runnable.setCancelled(true);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		});
		return runnable;
	}

	private UIMonitorUtility() {
	}

}
