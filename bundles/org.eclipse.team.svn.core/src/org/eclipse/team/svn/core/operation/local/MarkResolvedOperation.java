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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConflictResolution;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Performs "mark resolved" operation
 * 
 * @author Alexander Gurov
 */
public class MarkResolvedOperation extends AbstractWorkingCopyOperation {
	protected SVNConflictResolution.Choice conflictResult;

	protected SVNDepth depth;

	public MarkResolvedOperation(IResource[] resources, SVNConflictResolution.Choice conflictResult, SVNDepth depth) {
		super("Operation_MarkResolved", SVNMessages.class, resources); //$NON-NLS-1$
		this.conflictResult = conflictResult;
		this.depth = depth;
	}

	public MarkResolvedOperation(IResourceProvider provider, SVNConflictResolution.Choice conflictResult,
			SVNDepth depth) {
		super("Operation_MarkResolved", SVNMessages.class, provider); //$NON-NLS-1$
		this.conflictResult = conflictResult;
		this.depth = depth;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resources[i]);
			final String path = FileUtility.getWorkingCopyPath(resources[i]);
			final ISVNConnector proxy = location.acquireSVNProxy();

			this.protectStep(monitor1 -> proxy.resolve(path, conflictResult, depth,
					new SVNProgressMonitor(MarkResolvedOperation.this, monitor1, null)), monitor, resources.length);

			location.releaseSVNProxy(proxy);
		}
	}

}
