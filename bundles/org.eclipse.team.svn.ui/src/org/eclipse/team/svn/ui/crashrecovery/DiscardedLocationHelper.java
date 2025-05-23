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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.extension.crashrecovery.ErrorDescription;
import org.eclipse.team.svn.core.extension.crashrecovery.IResolutionHelper;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.remote.management.AddRepositoryLocationOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.operation.RefreshRepositoryLocationsOperation;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Provides ability to handle situation when repository location which is attached to the project is discarded.
 * 
 * @author Alexander Gurov
 */
public class DiscardedLocationHelper implements IResolutionHelper {

	@Override
	public boolean acquireResolution(ErrorDescription description) {
		if (description.code == ErrorDescription.REPOSITORY_LOCATION_IS_DISCARDED) {
			Object[] context = (Object[]) description.context;

			final IRepositoryLocation location = (IRepositoryLocation) context[1];

			// check if location is unavailable due to project is checked out with very old plug-in version
			if (location == null) {
				return false;
			}

			// check if already handled for any other project
			if (SVNRemoteStorage.instance().getRepositoryLocation(location.getId()) != null) {
				return true;
			}

			final IProject project = (IProject) context[0];

			final boolean[] solved = { false };
			UIMonitorUtility.parallelSyncExec(() -> {
				MessageDialog dlg = new MessageDialog(
						UIMonitorUtility.getShell(), SVNUIMessages.DiscardedLocationHelper_Dialog_Title, null,
						BaseMessages.format(SVNUIMessages.DiscardedLocationHelper_Dialog_Message,
								new String[] { project.getName(), location.getLabel() }),
						MessageDialog.WARNING,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
				solved[0] = dlg.open() == 0;
			});

			if (solved[0]) {
				AddRepositoryLocationOperation mainOp = new AddRepositoryLocationOperation(location);
				CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
				op.add(mainOp);
				op.add(new SaveRepositoryLocationsOperation());
				op.add(new RefreshRepositoryLocationsOperation(new IRepositoryLocation[] { location }, true));
				UIMonitorUtility.doTaskBusyDefault(op);
			}

			return solved[0];
		}
		return false;
	}

}
