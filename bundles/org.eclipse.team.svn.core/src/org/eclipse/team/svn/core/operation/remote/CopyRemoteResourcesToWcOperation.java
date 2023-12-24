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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryReference;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Copy remote resource to working copy operation
 * 
 * @author Igor Burilo
 */
public class CopyRemoteResourcesToWcOperation extends AbstractActionOperation {
	protected SVNEntryReference entry;

	protected IResource resource;

	public CopyRemoteResourcesToWcOperation(SVNEntryReference entry, IResource resource) {
		super("Operation_CopyRemoteToWC", SVNMessages.class); //$NON-NLS-1$
		this.entry = entry;
		this.resource = resource;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource repositoryResource = SVNUtility.asRepositoryResource(entry.path, true);
		IRepositoryLocation location = repositoryResource.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			writeToConsole(IConsoleStream.LEVEL_CMD, "svn copy \"" + entry.path + "@" + entry.pegRevision //$NON-NLS-1$//$NON-NLS-2$
					+ "\" \"" + FileUtility.getWorkingCopyPath(resource) + "\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
			proxy.copyLocal(
					new SVNEntryRevisionReference[] { new SVNEntryRevisionReference(entry, entry.pegRevision) },
					FileUtility.getWorkingCopyPath(resource), ISVNConnector.Options.NONE,
					ISVNConnector.NO_EXTERNALS_TO_PIN, new SVNProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return resource instanceof IProject ? resource : resource.getParent();
	}
}
