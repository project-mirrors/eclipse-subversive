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

package org.eclipse.team.svn.ui.compare;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Compare panel
 * 
 * @author Sergiy Logvin
 */
public class ComparePanel extends AbstractDialogPanel {

	protected CompareEditorInput compareInput;

	protected IResource resource;

	public ComparePanel(CompareEditorInput compareInput, IResource resource) {
		super(new String[] { SVNUIMessages.CompareLocalPanel_Save, IDialogConstants.CANCEL_LABEL });
		this.compareInput = compareInput;
		this.resource = resource;
		dialogTitle = SVNUIMessages.CompareLocalPanel_Title;
		dialogDescription = SVNUIMessages.CompareLocalPanel_Description;
		defaultMessage = SVNUIMessages.CompareLocalPanel_Message;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		Control control = compareInput.createContents(parent);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		Shell shell = control.getShell();
		shell.setText(compareInput.getTitle());
		shell.setImage(compareInput.getTitleImage());
	}

	@Override
	protected void cancelChangesImpl() {

	}

	@Override
	protected void saveChangesImpl() {

		RefreshResourcesOperation refreshOp = new RefreshResourcesOperation(
				new IResource[] { resource.getProject() });
		AbstractWorkingCopyOperation mainOp = new AbstractWorkingCopyOperation("Operation_SaveChanges", //$NON-NLS-1$
				SVNUIMessages.class, new IResource[] { resource.getProject() }) {
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				compareInput.saveChanges(monitor);
			}
		};
		CompositeOperation composite = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		composite.add(mainOp);
		composite.add(refreshOp);
		UIMonitorUtility.doTaskBusyWorkspaceModify(composite);
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(650, 500);
	}

}
