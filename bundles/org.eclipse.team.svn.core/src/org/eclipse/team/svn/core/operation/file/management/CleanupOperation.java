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

package org.eclipse.team.svn.core.operation.file.management;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.file.AbstractFileOperation;
import org.eclipse.team.svn.core.operation.file.IFileProvider;
import org.eclipse.team.svn.core.operation.file.IFileStorage;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Cleanup working copy after failure operation implementation
 * 
 * @author Alexander Gurov
 */
public class CleanupOperation extends AbstractFileOperation {
	public CleanupOperation(File[] files) {
		super("Operation_CleanupFile", SVNMessages.class, files); //$NON-NLS-1$
	}

	public CleanupOperation(IFileProvider provider) {
		super("Operation_CleanupFile", SVNMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] files = operableData();

		IFileStorage storage = SVNFileStorage.instance();

		for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
			IRepositoryResource remote = storage.asRepositoryResource(files[i], false);

			IRepositoryLocation location = remote.getRepositoryLocation();
			final ISVNConnector proxy = location.acquireSVNProxy();
			final String current = files[i].getAbsolutePath();

			ProgressMonitorUtility.setTaskInfo(monitor, this, current);

			writeToConsole(IConsoleStream.LEVEL_CMD, "svn cleanup \"" + FileUtility.normalizePath(current) + "\"\n"); //$NON-NLS-1$ //$NON-NLS-2$

			this.protectStep(monitor1 -> proxy.cleanup(current, ISVNConnector.Options.BREAK_LOCKS | ISVNConnector.Options.INCLUDE_TIMESTAMPS
					| ISVNConnector.Options.INCLUDE_DAVCACHE | ISVNConnector.Options.INCLUDE_UNUSED_PRISTINES,
					new SVNProgressMonitor(CleanupOperation.this, monitor1, null)), monitor, files.length);
			location.releaseSVNProxy(proxy);
		}
	}

}
