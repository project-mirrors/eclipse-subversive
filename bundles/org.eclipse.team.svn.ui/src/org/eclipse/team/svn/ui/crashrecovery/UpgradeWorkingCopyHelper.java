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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.crashrecovery;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.extension.crashrecovery.ErrorDescription;
import org.eclipse.team.svn.core.extension.crashrecovery.IResolutionHelper;
import org.eclipse.team.svn.core.operation.local.UpgradeWorkingCopyOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Handle invalid meta-information problem here
 * 
 * @author Alexander Gurov
 */
public class UpgradeWorkingCopyHelper implements IResolutionHelper {

	@Override
	public boolean acquireResolution(ErrorDescription description) {
		if (description.code == ErrorDescription.WORKING_COPY_REQUIRES_UPGRADE) {
			final IProject project = (IProject) description.context;
			final boolean[] solved = { false };
			UIMonitorUtility.parallelSyncExec(() -> {
				String title = BaseMessages.format(SVNUIMessages.UpgradeWorkingCopyDialog_Title,
						new String[] { project.getName() });
				MessageDialog dlg = new MessageDialog(UIMonitorUtility.getShell(), title, null,
						SVNUIMessages.UpgradeWorkingCopyDialog_Message, MessageDialog.QUESTION,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
				if (dlg.open() == 0) {
					UIMonitorUtility.doTaskNowDefault(UIMonitorUtility.getShell(),
							new UpgradeWorkingCopyOperation(new IResource[] { project }), false);
					solved[0] = true;
				}
			});
			return solved[0];
		}
		return false;
	}

}
