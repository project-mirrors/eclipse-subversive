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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNEntryStatusCallback;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * Abstract status reporting operation
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractStatusOperation extends AbstractFileOperation {
	protected SVNChangeStatus[] statuses;

	protected boolean recursive;

	public AbstractStatusOperation(String operationName, Class<? extends NLS> messagesClass, File[] files,
			boolean recursive) {
		super(operationName, messagesClass, files);
		this.recursive = recursive;
	}

	public AbstractStatusOperation(String operationName, Class<? extends NLS> messagesClass, IFileProvider provider,
			boolean recursive) {
		super(operationName, messagesClass, provider);
		this.recursive = recursive;
	}

	public SVNChangeStatus[] getStatuses() {
		return statuses;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] files = operableData();

		final List<SVNChangeStatus> result = new ArrayList<>();
		final List<SVNChangeStatus> conflicts = new ArrayList<>();

		ISVNEntryStatusCallback cb = status -> {
			result.add(status);
			if (status.hasConflict && status.treeConflicts == null) {
				conflicts.add(status);
			}
		};
		for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
			IRepositoryResource remote = SVNFileStorage.instance().asRepositoryResource(files[i], false);

			IRepositoryLocation location = remote.getRepositoryLocation();
			final ISVNConnector proxy = location.acquireSVNProxy();

			conflicts.clear();

			reportStatuses(proxy, cb, files[i], monitor, files.length);

			for (Iterator<SVNChangeStatus> it = conflicts.iterator(); it.hasNext() && !monitor.isCanceled();) {
				final SVNChangeStatus svnChangeStatus = it.next();
				this.protectStep(monitor1 -> proxy.getInfo(new SVNEntryRevisionReference(svnChangeStatus.path), SVNDepth.EMPTY,
						ISVNConnector.Options.FETCH_ACTUAL_ONLY, null, info -> svnChangeStatus.setTreeConflicts(info.treeConflicts), new SVNProgressMonitor(AbstractStatusOperation.this, monitor1, null, false)), monitor, files.length);
			}

			location.releaseSVNProxy(proxy);
		}
		statuses = result.toArray(new SVNChangeStatus[result.size()]);
	}

	protected void reportStatuses(final ISVNConnector proxy, final ISVNEntryStatusCallback cb, final File current,
			IProgressMonitor monitor, int tasks) {
		this.protectStep(monitor1 -> proxy.status(
				current.getAbsolutePath(), SVNDepth.infinityOrImmediates(recursive),
				AbstractStatusOperation.this.isRemote()
						? ISVNConnector.Options.SERVER_SIDE
						: ISVNConnector.Options.LOCAL_SIDE,
				null, cb, new SVNProgressMonitor(AbstractStatusOperation.this, monitor1, null, false)), monitor, tasks);
	}

	protected abstract boolean isRemote();

}
