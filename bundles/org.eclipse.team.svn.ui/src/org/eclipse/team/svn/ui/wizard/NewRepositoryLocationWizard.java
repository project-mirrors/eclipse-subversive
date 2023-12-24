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

package org.eclipse.team.svn.ui.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.wizard.shareproject.AddRepositoryLocationPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Repository location registration wizard
 * 
 * @author Alexander Gurov
 */
public class NewRepositoryLocationWizard extends AbstractSVNWizard implements INewWizard {
	protected AddRepositoryLocationPage locationPage;

	protected IRepositoryLocation editable;

	protected boolean performAction;

	protected IRepositoryLocation backup;

	public NewRepositoryLocationWizard() {
		this(null, true);
	}

	public NewRepositoryLocationWizard(IRepositoryLocation editable, boolean performAction) {
		this.performAction = performAction;
		this.editable = editable;
		if (this.editable != null) {
			setWindowTitle(SVNUIMessages.NewRepositoryLocationWizard_Title_Edit);
			backup = SVNRemoteStorage.instance().newRepositoryLocation();
			SVNRemoteStorage.instance().copyRepositoryLocation(backup, editable);
		} else {
			setWindowTitle(SVNUIMessages.NewRepositoryLocationWizard_Title_New);
		}
	}

	@Override
	public void addPages() {
		addPage(locationPage = new AddRepositoryLocationPage(editable));
	}

	public IActionOperation getOperationToPerform() {
		return locationPage.getOperationToPeform();
	}

	@Override
	public boolean performCancel() {
		if (editable != null) {
			SVNRemoteStorage.instance().copyRepositoryLocation(editable, backup);
		}
		return super.performCancel();
	}

	@Override
	public boolean performFinish() {
		if (locationPage.performFinish()) {
			if (performAction) {
				IActionOperation op = locationPage.getOperationToPeform();
				if (op != null) {
					UIMonitorUtility.doTaskBusyDefault(op);
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {

	}

}
