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

package org.eclipse.team.svn.ui.dialog;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Branch/Tag creation error dialog
 * 
 * @author Alexander Gurov
 */
public class OperationErrorDialog extends MessageDialog {
	public static final int ERR_NONE = -1;

	public static final int ERR_DIFFREPOSITORIES = 0;

	public static final int ERR_DIFFPROJECTS = 1;

	protected static final String[] errorMessages = { "OperationErrorDialog_Message_DifferentRepositories", //$NON-NLS-1$
			"OperationErrorDialog_Message_DifferentProjects" //$NON-NLS-1$
	};

	public OperationErrorDialog(Shell parentShell, String title, int errorCode) {
		this(parentShell, title, SVNUIMessages.getString(OperationErrorDialog.errorMessages[errorCode]));
	}

	public OperationErrorDialog(Shell parentShell, String title, String errorMessage) {
		super(parentShell, title, null, errorMessage, MessageDialog.WARNING, new String[] { IDialogConstants.OK_LABEL },
				0);
	}

	public static boolean isAcceptableAtOnce(IResource[] resources, String name, Shell shell) {
		IRepositoryResource[] remoteResources = new IRepositoryResource[resources.length];
		for (int i = 0; i < resources.length; i++) {
			remoteResources[i] = SVNRemoteStorage.instance().asRepositoryResource(resources[i]);
		}
		return OperationErrorDialog.isAcceptableAtOnce(remoteResources, name, shell);
	}

	public static boolean isAcceptableAtOnce(IRepositoryResource[] resources, String name, Shell shell) {
		IRepositoryLocation first = resources[0].getRepositoryLocation();
		String url = SVNUtility.getTrunkLocation(resources[0]).getUrl();
		for (int i = 1; i < resources.length; i++) {
			if (resources[i].getRepositoryLocation() != first) {
				new OperationErrorDialog(shell, name, OperationErrorDialog.ERR_DIFFREPOSITORIES).open();
				return false;
			}
			if (!url.equals(SVNUtility.getTrunkLocation(resources[i]).getUrl())) {
				new OperationErrorDialog(shell, name, OperationErrorDialog.ERR_DIFFPROJECTS).open();
				return false;
			}
		}
		return true;
	}

}
