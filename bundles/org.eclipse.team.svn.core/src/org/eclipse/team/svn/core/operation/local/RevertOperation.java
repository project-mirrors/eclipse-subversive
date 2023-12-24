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
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Revert operation implementation
 * 
 * @author Alexander Gurov
 */
public class RevertOperation extends AbstractWorkingCopyOperation {
	protected boolean doRecursiveRevert;

	public RevertOperation(IResource[] resources, boolean doRecursiveRevert) {
		super("Operation_Revert", SVNMessages.class, resources); //$NON-NLS-1$
		this.doRecursiveRevert = doRecursiveRevert;
	}

	public RevertOperation(IResourceProvider provider, boolean doRecursiveRevert) {
		super("Operation_Revert", SVNMessages.class, provider); //$NON-NLS-1$
		this.doRecursiveRevert = doRecursiveRevert;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		if (doRecursiveRevert) {
			resources = FileUtility.shrinkChildNodesWithSwitched(resources);
		} else {
			FileUtility.reorder(resources, false);
		}

		IRemoteStorage storage = SVNRemoteStorage.instance();
		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			IRepositoryLocation location = storage.getRepositoryLocation(resources[i]);
			final String wcPath = FileUtility.getWorkingCopyPath(resources[i]);
			final ISVNConnector proxy = location.acquireSVNProxy();
			writeToConsole(IConsoleStream.LEVEL_CMD, "svn revert \"" + FileUtility.normalizePath(wcPath) + "\"" //$NON-NLS-1$//$NON-NLS-2$
					+ (doRecursiveRevert ? " -R" : "") + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			this.protectStep(monitor1 -> proxy.revert(new String[] { wcPath }, SVNDepth.infinityOrEmpty(doRecursiveRevert), null,
					ISVNConnector.Options.NONE, new SVNProgressMonitor(RevertOperation.this, monitor1, null)), monitor, resources.length);
			location.releaseSVNProxy(proxy);
		}
	}

}
