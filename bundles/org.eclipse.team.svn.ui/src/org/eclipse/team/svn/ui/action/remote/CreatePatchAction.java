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

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.CreatePatchOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.action.AbstractRepositoryModifyWorkspaceAction;
import org.eclipse.team.svn.ui.operation.FileToClipboardOperation;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;
import org.eclipse.team.svn.ui.wizard.CreatePatchRemoteWizard;
import org.eclipse.team.svn.ui.wizard.CreatePatchWizard;

/**
 * Create patch bases on difference between two repository resources
 * 
 * @author Alexander Gurov
 */
public class CreatePatchAction extends AbstractRepositoryModifyWorkspaceAction {
	public CreatePatchAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		CreatePatchWizard wizard = null;
		if (resources.length == 1) {
			wizard = new CreatePatchRemoteWizard(resources[0], true);
		} else {
			wizard = new CreatePatchWizard(resources[0].getName(), null, true);
		}

		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		if (dialog.open() == 0) {
			IRepositoryResource second = resources.length == 1
					? ((CreatePatchRemoteWizard) wizard).getSelectedResource()
					: resources[1];
			try {
				if (resources[0].getRevision() > second.getRevision()) {
					IRepositoryResource tmp = second;
					second = resources[0];
					resources[0] = tmp;
				}
				runScheduled(CreatePatchAction.getCreatePatchOperation(resources[0], second, wizard));
			} catch (SVNConnectorException ex) {
				UILoggedOperation.reportError(SVNMessages.Operation_CreatePatchRemote, ex);
			}
		}
	}

	public static IActionOperation getCreatePatchOperation(IRepositoryResource first, IRepositoryResource second,
			CreatePatchWizard wizard) {
		CreatePatchOperation mainOp = new CreatePatchOperation(first, second, wizard.getFileName(),
				wizard.isRecursive(), wizard.getDiffOptions(), wizard.getDiffOutputOptions());
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(mainOp);
		switch (wizard.getWriteMode()) {
			case CreatePatchWizard.WRITE_TO_WORKSPACE_FILE: {
				op.add(new RefreshResourcesOperation(new IResource[] { wizard.getTargetFolder() }, IResource.DEPTH_ONE,
						RefreshResourcesOperation.REFRESH_CHANGES), new IActionOperation[] { mainOp });
				break;
			}
			case CreatePatchWizard.WRITE_TO_CLIPBOARD: {
				op.add(new FileToClipboardOperation(wizard.getFileName(), wizard.getCharset(), true),
						new IActionOperation[] { mainOp });
				break;
			}
		}
		return op;

	}

	@Override
	public boolean isEnabled() {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		return resources.length == 1 || resources.length == 2;
	}

}
