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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Revert operation implementation
 * 
 * @author Alexander Gurov
 */
public class UpgradeWorkingCopyOperation extends AbstractWorkingCopyOperation {

	public UpgradeWorkingCopyOperation(IResource[] resources) {
		super("Operation_Upgrade", SVNMessages.class, resources); //$NON-NLS-1$
	}

	public UpgradeWorkingCopyOperation(IResourceProvider provider) {
		super("Operation_Upgrade", SVNMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();
		resources = FileUtility.shrinkChildNodes(resources);

		final ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
				final String wcPath = FileUtility.getWorkingCopyPath(resources[i]);
				writeToConsole(IConsoleStream.LEVEL_CMD, "svn upgrade \"" + FileUtility.normalizePath(wcPath) + "\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
				this.protectStep(monitor1 -> proxy.upgrade(wcPath, new SVNProgressMonitor(UpgradeWorkingCopyOperation.this, monitor1, null)), monitor, resources.length);
			}
		} finally {
			proxy.dispose();
		}
	}

}
