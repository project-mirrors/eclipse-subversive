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
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Revert local modifications
 * 
 * @author Alexander Gurov
 */
public class UpgradeWorkingCopyOperation extends AbstractFileOperation {
	public UpgradeWorkingCopyOperation(File[] files) {
		super("Operation_Upgrade", SVNMessages.class, files); //$NON-NLS-1$
	}

	public UpgradeWorkingCopyOperation(IFileProvider provider) {
		super("Operation_Upgrade", SVNMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] files = operableData();

		files = FileUtility.shrinkChildNodes(files, false);

		final ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
				final File current = files[i];
				writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn upgrade \"" + FileUtility.normalizePath(current.getAbsolutePath()) + "\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
				this.protectStep(monitor1 -> proxy.upgrade(current.getAbsolutePath(),
						new SVNProgressMonitor(UpgradeWorkingCopyOperation.this, monitor1, null)), monitor, files.length);
			}
		} finally {
			proxy.dispose();
		}
	}

}
