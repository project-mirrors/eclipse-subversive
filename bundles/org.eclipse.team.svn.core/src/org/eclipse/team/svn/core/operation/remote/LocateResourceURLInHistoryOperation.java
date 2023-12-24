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

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Locate resource URL that corresponds to revision
 * 
 * @author Alexander Gurov
 */
public class LocateResourceURLInHistoryOperation extends AbstractRepositoryOperation
		implements IRepositoryResourceProvider {
	protected IRepositoryResource[] converted;

	public LocateResourceURLInHistoryOperation(IRepositoryResource[] resources) {
		super("Operation_LocateURLInHistory", SVNMessages.class, resources); //$NON-NLS-1$
	}

	public LocateResourceURLInHistoryOperation(IRepositoryResourceProvider provider) {
		super("Operation_LocateURLInHistory", SVNMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	public IRepositoryResource[] getRepositoryResources() {
		return converted;
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		IRepositoryResource[] resources = operableData();
		converted = new IRepositoryResource[resources.length];
		System.arraycopy(resources, 0, converted, 0, resources.length);

		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final int idx = i;
			ProgressMonitorUtility.setTaskInfo(monitor, this, resources[i].getUrl());
			if (converted[i].getSelectedRevision().getKind() == Kind.NUMBER) {
				this.protectStep(monitor1 -> converted[idx] = LocateResourceURLInHistoryOperation.this.processEntry(converted[idx], monitor1), monitor, resources.length);
			}
		}
	}

	protected IRepositoryResource processEntry(IRepositoryResource current, IProgressMonitor monitor) throws Exception {
		IRepositoryLocation location = current.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			SVNEntryRevisionReference entry = SVNUtility.getEntryRevisionReference(current);
			entry = SVNUtility.convertRevisionReference(proxy, entry, new SVNProgressMonitor(this, monitor, null));
			IRepositoryResource retVal = current instanceof IRepositoryFile
					? (IRepositoryResource) location.asRepositoryFile(entry.path, false)
					: location.asRepositoryContainer(entry.path, false);
			retVal.setPegRevision(entry.pegRevision);
			retVal.setSelectedRevision(entry.revision);
			return retVal;
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

}
