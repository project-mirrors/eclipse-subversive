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

package org.eclipse.team.svn.core.operation.file;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Revert local modifications
 * 
 * @author Alexander Gurov
 */
public class RevertOperation extends AbstractFileOperation {
	protected boolean recursive;

	public RevertOperation(File[] files, boolean recursive) {
		super("Operation_RevertFile", SVNMessages.class, files); //$NON-NLS-1$
		this.recursive = recursive;
	}

	public RevertOperation(IFileProvider provider, boolean recursive) {
		super("Operation_RevertFile", SVNMessages.class, provider); //$NON-NLS-1$
		this.recursive = recursive;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] files = operableData();

		if (recursive) {
			files = FileUtility.shrinkChildNodes(files, false);
		} else {
			FileUtility.reorder(files, false);
		}

		for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
			final File current = files[i];
			IRepositoryResource remote = SVNFileStorage.instance().asRepositoryResource(current, false);
			IRepositoryLocation location = remote.getRepositoryLocation();
			final ISVNConnector proxy = location.acquireSVNProxy();
			writeToConsole(IConsoleStream.LEVEL_CMD,
					"svn revert \"" + FileUtility.normalizePath(current.getAbsolutePath()) + "\"" //$NON-NLS-1$//$NON-NLS-2$
							+ (recursive ? " -R" : "") + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			this.protectStep(monitor1 -> proxy.revert(new String[] { current.getAbsolutePath() }, SVNDepth.infinityOrEmpty(recursive), null,
					ISVNConnector.Options.NONE, new SVNProgressMonitor(RevertOperation.this, monitor1, null)), monitor, files.length);
			location.releaseSVNProxy(proxy);
		}
	}

}
