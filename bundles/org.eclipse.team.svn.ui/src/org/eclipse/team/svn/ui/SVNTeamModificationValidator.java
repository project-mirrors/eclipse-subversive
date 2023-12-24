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

package org.eclipse.team.svn.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.utility.LockProposeUtility;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * SVN file modification validator
 * 
 * @author Sergiy Logvin
 */
public class SVNTeamModificationValidator extends FileModificationValidator {
	@Override
	public IStatus validateEdit(IFile[] files, final FileModificationValidationContext context) {
		if (FileUtility.isConnected(files[0])) {
			final IResource[] needsLockResources = getNeedsLockResources(files);
			if (needsLockResources.length > 0) {
				final IStatus[] retVal = new IStatus[1];
				final Shell shell = context.getShell() == null
						? UIMonitorUtility.getShell()
						: (Shell) context.getShell();
				shell.getDisplay().syncExec(() -> retVal[0] = LockProposeUtility.proposeLock(needsLockResources, shell, true));
				return retVal[0];
			}
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus validateSave(IFile file) {
		return Status.OK_STATUS;
	}

	protected IResource[] getNeedsLockResources(IResource[] files) {
		List<IResource> returnResources = new ArrayList<>();
		IResource[] needsLockResources = FileUtility.getResourcesRecursive(files, IStateFilter.SF_NEEDS_LOCK,
				IResource.DEPTH_ZERO);
		for (IResource element : needsLockResources) {
			if (!SVNRemoteStorage.instance().asLocalResource(element).isLocked()) {
				returnResources.add(element);
			}
		}
		return returnResources.toArray(new IResource[returnResources.size()]);
	}

}
