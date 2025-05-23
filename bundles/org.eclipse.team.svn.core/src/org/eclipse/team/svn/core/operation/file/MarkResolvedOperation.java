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
import org.eclipse.team.svn.core.connector.SVNConflictResolution;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Mark conflicts as resolved
 * 
 * @author Alexander Gurov
 */
public class MarkResolvedOperation extends AbstractFileOperation {
	protected boolean recursive;

	public MarkResolvedOperation(File[] files, boolean recursive) {
		super("Operation_MarkResolvedFile", SVNMessages.class, files); //$NON-NLS-1$
		this.recursive = recursive;
	}

	public MarkResolvedOperation(IFileProvider provider, boolean recursive) {
		super("Operation_MarkResolvedFile", SVNMessages.class, provider); //$NON-NLS-1$
		this.recursive = recursive;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] files = operableData();

		if (recursive) {
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

			this.protectStep(monitor1 -> proxy.resolve(current.getAbsolutePath(), SVNConflictResolution.Choice.CHOOSE_MERGED,
					SVNDepth.infinityOrEmpty(recursive),
					new SVNProgressMonitor(MarkResolvedOperation.this, monitor1, null)), monitor, files.length);

			location.releaseSVNProxy(proxy);
		}
	}

}
