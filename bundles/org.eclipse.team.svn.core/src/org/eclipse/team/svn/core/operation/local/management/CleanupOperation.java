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

package org.eclipse.team.svn.core.operation.local.management;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Cleanup working copy after failure operation implementation
 * 
 * @author Alexander Gurov
 */
public class CleanupOperation extends AbstractWorkingCopyOperation {
	public CleanupOperation(IResource[] resources) {
		super("Operation_CleanupResources", SVNMessages.class, resources); //$NON-NLS-1$
	}

	public CleanupOperation(IResourceProvider provider) {
		super("Operation_CleanupResources", SVNMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		IRemoteStorage storage = SVNRemoteStorage.instance();

		for (int i = 0; i < resources.length; i++) {
			IRepositoryLocation location = storage.getRepositoryLocation(resources[i]);
			final String wcPath = FileUtility.getWorkingCopyPath(resources[i]);
			final ISVNConnector proxy = location.acquireSVNProxy();

			ProgressMonitorUtility.setTaskInfo(monitor, this, resources[i].getName());

			writeToConsole(IConsoleStream.LEVEL_CMD, "svn cleanup \"" + FileUtility.normalizePath(wcPath) + "\"\n"); //$NON-NLS-1$ //$NON-NLS-2$

			this.protectStep(monitor1 -> proxy.cleanup(wcPath, ISVNConnector.Options.BREAK_LOCKS | ISVNConnector.Options.INCLUDE_TIMESTAMPS
					| ISVNConnector.Options.INCLUDE_DAVCACHE | ISVNConnector.Options.INCLUDE_UNUSED_PRISTINES,
					new SVNProgressMonitor(CleanupOperation.this, monitor1, null)), monitor, resources.length);
			location.releaseSVNProxy(proxy);
			ProgressMonitorUtility.progress(monitor, i, resources.length);
		}
	}

}
