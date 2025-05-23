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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Igor Burilo - Bug 245509: Improve extract log
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Extract selected resources to location (only local resources) Used from synchronize view ExtractTo outgoing action
 * 
 * @author Alexei Goncharov
 */
public class ExtractToOperationLocal extends AbstractActionOperation {
	private IResource[] outgoingResources;

	private String path;

	private boolean delitionAllowed;

	private InitExtractLogOperation logger;

	/**
	 * Operation for extracting local resources to a location
	 * 
	 * @param outgoingResources
	 *            - the resources to extract array
	 * @param path
	 *            - path to extract to
	 * @param delitionAllowed
	 *            - specifies if deletion allowed if the resource is marked for deletion
	 */
	public ExtractToOperationLocal(IResource[] outgoingResources, String path, boolean delitionAllowed,
			InitExtractLogOperation logger) {
		super(SVNMessages.Operation_ExtractTo, SVNMessages.class);
		this.outgoingResources = outgoingResources;
		this.path = path;
		this.delitionAllowed = delitionAllowed;
		this.logger = logger;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		FileUtility.reorder(outgoingResources, true);
		int processed = 0;
		IPath previousPref = null;
		for (IResource current : outgoingResources) {
			IPath currentPath = current.getFullPath();
			String toOperate = ""; //$NON-NLS-1$
			if (previousPref == null || !previousPref.isPrefixOf(currentPath)) {
				toOperate = path + "/" + current.getName(); //$NON-NLS-1$
				if (current instanceof IContainer) {
					previousPref = current.getFullPath();
				}
			} else {
				int toRemove = previousPref.removeLastSegments(1).toString().length();
				if (toRemove < 2) {
					toRemove = 0;
				}
				toOperate = path + currentPath.toString().substring(toRemove);
			}
			File operatingDirectory = new File(toOperate);
			ILocalResource localResource = SVNRemoteStorage.instance().asLocalResourceAccessible(current);
			if (!IStateFilter.SF_NOTMODIFIED.accept(localResource)) {
				logger.log(operatingDirectory.getAbsolutePath().substring(path.length() + 1),
						localResource.getStatus());
			}
			if (IStateFilter.SF_DELETED.accept(localResource)) {
				if (operatingDirectory.exists() && delitionAllowed) {
					FileUtility.deleteRecursive(operatingDirectory);
				}
			} else if (!IStateFilter.SF_NOTMODIFIED.accept(localResource)) {
				if (previousPref != null) {
					File parent = operatingDirectory.getParentFile();
					if (parent != null) {
						monitor.subTask(BaseMessages.format(SVNMessages.Operation_ExtractTo_Folders,
								new String[] { FileUtility.getWorkingCopyPath(current) }));
						parent.mkdirs();
						operatingDirectory = parent;
					}
				}
				monitor.subTask(BaseMessages.format(SVNMessages.Operation_ExtractTo_LocalFile,
						new String[] { FileUtility.getWorkingCopyPath(current) }));
				if (localResource.getResource().getType() == IResource.FILE
						|| IStateFilter.SF_ADDED.accept(localResource)) {
					FileUtility.copyAll(operatingDirectory, new File(FileUtility.getWorkingCopyPath(current)),
							FileUtility.COPY_OVERRIDE_EXISTING_FILES | FileUtility.COPY_IGNORE_EXISTING_FOLDERS, null,
							monitor);
				}
			}
			ProgressMonitorUtility.progress(monitor, processed++, outgoingResources.length);
		}
	}

	@Override
	public int getOperationWeight() {
		if (outgoingResources.length == 0) {
			return 0;
		}
		return 1;
	}
}
