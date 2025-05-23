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

package org.eclipse.team.svn.ui.action;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.DefaultOperationWrapperFactory;
import org.eclipse.team.svn.ui.utility.ICancellableOperationWrapper;
import org.eclipse.team.svn.ui.utility.IOperationWrapperFactory;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.ide.IDE;

/**
 * This class implements operation running policies
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractSVNTeamAction extends TeamAction {
	// copy paste in order to fix problems with Eclipse 3.0.x->3.1.x->3.2 API changes
	private IWorkbenchWindow window;

	private Shell shell;

	private ISelectionListener selectionListener = (part, selection) -> {
		if (selection instanceof IStructuredSelection) {
			this.checkSelection((IStructuredSelection) selection);
		}
	};

	public AbstractSVNTeamAction() {
	}

	/**
	 * Save all dirty editors in the workbench that are open on files that may be affected by this operation. Opens a dialog to prompt the
	 * user if <code>confirm</code> is true. Return true if successful. Return false if the user has canceled the command. Must be called
	 * from the UI thread.
	 * 
	 * @param confirm
	 *            prompt the user if true
	 * @return boolean false if the operation was canceled.
	 */
	public final boolean saveAllEditors(boolean confirm) {
		return IDE.saveAllEditors(getOriginalSelectedResources(), confirm);
	}

	@Override
	public abstract boolean isEnabled();

	public abstract void runImpl(IAction action);

	@Override
	protected final void execute(final IAction action) throws InvocationTargetException, InterruptedException {
		ProgressMonitorUtility
				.doTaskExternal(new AbstractActionOperation("Operation_CallMenuAction", SVNUIMessages.class) { //$NON-NLS-1$
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						if (AbstractSVNTeamAction.this.isEnabled()) {
							if (AbstractSVNTeamAction.this.needsToSaveDirtyEditors() && !AbstractSVNTeamAction.this
									.saveAllEditors(AbstractSVNTeamAction.this.confirmSaveOfDirtyEditor())) {
								return;
							}
							AbstractSVNTeamAction.this.runImpl(action);
						}
					}
				}, new NullProgressMonitor());
	}

	/**
	 * Return whether dirty editor should be saved before this action is run. Default is <code>false</code>.
	 * 
	 * @return whether dirty editor should be saved before this action is run
	 */
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	/**
	 * Returns whether the user should be prompted to save dirty editors. The default is <code>true</code>.
	 * 
	 * @return whether the user should be prompted to save dirty editors
	 */
	protected boolean confirmSaveOfDirtyEditor() {
		return true;
	}

	protected ICancellableOperationWrapper runBusy(IActionOperation operation) {
		return UIMonitorUtility.doTaskBusy(operation, getOperationWrapperFactory());
	}

	protected ICancellableOperationWrapper runScheduled(IActionOperation operation) {
		return UIMonitorUtility.doTaskScheduled(getTargetPart(), operation, getOperationWrapperFactory());
	}

	protected IOperationWrapperFactory getOperationWrapperFactory() {
		return new DefaultOperationWrapperFactory();
	}

	protected void handleException(Exception ex) {
		this.handle(ex, SVNUIMessages.getErrorString("Error_ActionFailed"), SVNUIMessages.Error_ActionFailed_Message); //$NON-NLS-1$
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		try {
			IStructuredSelection structuredSelection = null;
			if (selection instanceof ITextSelection) {
				IEditorPart part = getTargetPage().getActiveEditor();
				if (part != null) {
					IResource resource = part.getEditorInput().getAdapter(IResource.class);
					if (resource != null && resource.getType() == IResource.FILE) {
						structuredSelection = new StructuredSelection(resource);
					}
				}
			} else if (selection instanceof IStructuredSelection) {
				structuredSelection = (IStructuredSelection) selection;
			}
			if (structuredSelection == null || structuredSelection.isEmpty()) {
				if (action != null) {
					action.setEnabled(false);
				}
			} else {
				checkSelection(structuredSelection);
				super.selectionChanged(action, structuredSelection);
			}
		} catch (Throwable ex) {
			LoggedOperation.reportError(SVNUIMessages.getErrorString("Error_MenuEnablement"), ex); //$NON-NLS-1$
		}
	}

	@Override
	protected Shell getShell() {
		return shell != null ? shell : super.getShell();
	}

	@Override
	public IWorkbenchWindow getWindow() {
		return window;
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
		shell = this.window.getShell();
		this.window.getSelectionService().addPostSelectionListener(selectionListener);
	}

	@Override
	public void dispose() {
		if (window != null) {
			window.getSelectionService().removePostSelectionListener(selectionListener);
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#getSelectedResources()
	 */
	@Override
	protected IResource[] getSelectedResources() {
		//filters out not valid resources
		List<IResource> res = new ArrayList<>();
		IResource[] resources = super.getSelectedResources();
		for (IResource resource : resources) {
			if (!FileUtility.isNotSupervised(resource)) {
				res.add(resource);
			}
		}
		return res.toArray(new IResource[0]);
	}

	protected IResource[] getOriginalSelectedResources() {
		return super.getSelectedResources();
	}

	protected abstract void checkSelection(IStructuredSelection selection);

	@Override
	protected abstract IStructuredSelection getSelection();

}
