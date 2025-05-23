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
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.file.AbstractFileOperation;
import org.eclipse.team.svn.core.operation.file.IFileProvider;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Relocate specified folders to the new url.
 * 
 * @author Alexander Gurov
 */
public class RelocateOperation extends AbstractFileOperation {
	protected String toUrl;

	public RelocateOperation(File[] folders, String toUrl) {
		super("Operation_RelocateFile", SVNMessages.class, folders); //$NON-NLS-1$
		this.toUrl = toUrl;
	}

	public RelocateOperation(IFileProvider provider, String toUrl) {
		super("Operation_RelocateFile", SVNMessages.class, provider); //$NON-NLS-1$
		this.toUrl = toUrl;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] files = FileUtility.shrinkChildNodes(operableData(), true);

		for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
			final File current = files[i];
			final IRepositoryResource remote = SVNFileStorage.instance().asRepositoryResource(current, false);
			final IRepositoryLocation location = remote.getRepositoryLocation();
			final ISVNConnector proxy = location.acquireSVNProxy();
			this.protectStep(monitor1 -> {
				String path = current.getAbsolutePath();
				RelocateOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn switch --relocate \"" + remote.getUrl() + "\" \"" + toUrl //$NON-NLS-1$//$NON-NLS-2$
								+ "\" \"" + FileUtility.normalizePath(path) + "\"" //$NON-NLS-1$//$NON-NLS-2$
								+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
				proxy.relocate(SVNUtility.encodeURL(remote.getUrl()), SVNUtility.encodeURL(toUrl), path,
						SVNDepth.INFINITY, new SVNProgressMonitor(RelocateOperation.this, monitor1, null));
			}, monitor, files.length);
			location.releaseSVNProxy(proxy);
		}
	}

}
