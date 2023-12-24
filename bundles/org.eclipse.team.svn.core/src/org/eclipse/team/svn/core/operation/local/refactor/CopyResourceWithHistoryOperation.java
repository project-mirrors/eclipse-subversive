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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local.refactor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Copies versioned resource with the history preserved
 * 
 * @author Sergiy Logvin
 */
public class CopyResourceWithHistoryOperation extends AbstractActionOperation {
	protected IResource source;

	protected IResource destination;

	public CopyResourceWithHistoryOperation(IResource source, IResource destination) {
		super("Operation_CopyLocalH", SVNMessages.class); //$NON-NLS-1$
		this.source = source;
		this.destination = destination;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return destination instanceof IProject ? destination : destination.getParent();
	}

	public boolean isAllowed() {
		IRemoteStorage storage = SVNRemoteStorage.instance();
		IRepositoryLocation locationSource = storage.getRepositoryLocation(source);
		IRepositoryLocation locationDestination = storage.getRepositoryLocation(destination);
		ILocalResource localSource = storage.asLocalResource(source);

		return IStateFilter.SF_ONREPOSITORY.accept(localSource) && locationSource.equals(locationDestination);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRemoteStorage storage = SVNRemoteStorage.instance();
		IRepositoryLocation location = storage.getRepositoryLocation(source);
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			writeToConsole(IConsoleStream.LEVEL_CMD,
					"svn copy \"" + FileUtility.normalizePath(FileUtility.getWorkingCopyPath(source)) + "\" \"" //$NON-NLS-1$//$NON-NLS-2$
							+ FileUtility.getWorkingCopyPath(destination) + "\"\n"); //$NON-NLS-1$
			proxy.copyLocal(
					new SVNEntryRevisionReference[] { new SVNEntryRevisionReference(
							FileUtility.getWorkingCopyPath(source), null, SVNRevision.WORKING) },
					FileUtility.getWorkingCopyPath(destination), ISVNConnector.Options.NONE,
					ISVNConnector.NO_EXTERNALS_TO_PIN, new SVNProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t),
				new Object[] { source.getName(), destination.toString() });
	}

}
