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
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Add to version control operation
 * 
 * @author Alexander Gurov
 */
public class AddToSVNOperation extends AbstractFileOperation {
	protected boolean isRecursive;

	public AddToSVNOperation(File[] files, boolean isRecursive) {
		super("Operation_AddToSVNFile", SVNMessages.class, files); //$NON-NLS-1$
		this.isRecursive = isRecursive;
	}

	public AddToSVNOperation(IFileProvider provider, boolean isRecursive) {
		super("Operation_AddToSVNFile", SVNMessages.class, provider); //$NON-NLS-1$
		this.isRecursive = isRecursive;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] files = operableData();
		if (isRecursive) {
			files = FileUtility.shrinkChildNodes(files, false);
		} else {
			FileUtility.reorder(files, true);
		}

		for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
			final File current = files[i];
			IRepositoryLocation location = SVNFileStorage.instance()
					.asRepositoryResource(current, false)
					.getRepositoryLocation();
			final ISVNConnector proxy = location.acquireSVNProxy();
			this.protectStep(monitor1 -> {
				AddToSVNOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn add \"" + FileUtility.normalizePath(current.getAbsolutePath()) + "\"" //$NON-NLS-1$//$NON-NLS-2$
								+ (isRecursive ? "" : " -N") //$NON-NLS-1$//$NON-NLS-2$
								+ ISVNConnector.Options.asCommandLine(
										ISVNConnector.Options.FORCE | ISVNConnector.Options.INCLUDE_PARENTS)
								+ "\n"); //$NON-NLS-1$

				File parent = current.getParentFile();
				if (parent != null) {
					org.eclipse.team.svn.core.operation.local.AddToSVNOperation.removeFromParentIgnore(proxy,
							parent.getAbsolutePath(), current.getName());
				}

				proxy.add(current.getAbsolutePath(), SVNDepth.infinityOrEmpty(isRecursive),
						ISVNConnector.Options.FORCE | ISVNConnector.Options.INCLUDE_PARENTS,
						new SVNProgressMonitor(AddToSVNOperation.this, monitor1, null));
			}, monitor, files.length);
			location.releaseSVNProxy(proxy);
		}
	}

}
