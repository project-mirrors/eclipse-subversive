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

package org.eclipse.team.svn.core.operation.local.refactor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Move Eclipse resource in the working copy
 * 
 * @author Alexander Gurov
 */
public class MoveResourceOperation extends AbstractActionOperation {
	protected IResource source;

	protected IResource destination;

	public MoveResourceOperation(IResource source, IResource destination) {
		super("Operation_MoveLocal", SVNMessages.class); //$NON-NLS-1$
		this.source = source;
		this.destination = destination;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return MultiRule.combine(
				source instanceof IProject ? source : source.getParent(),
				destination instanceof IProject ? destination : destination.getParent());
	}

	public boolean isAllowed() {
		IRemoteStorage storage = SVNRemoteStorage.instance();
		String locationSourceUrl = storage.getRepositoryLocation(source).getRepositoryRootUrl();
		String locationDestinationUrl = storage.getRepositoryLocation(destination).getRepositoryRootUrl();
		return locationSourceUrl.equals(locationDestinationUrl);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRemoteStorage storage = SVNRemoteStorage.instance();
		IRepositoryLocation location = storage.getRepositoryLocation(source);

		ISVNConnector proxy = location.acquireSVNProxy();

		String srcPath = FileUtility.getWorkingCopyPath(source);
		String dstPath = FileUtility.getWorkingCopyPath(destination);
		try {
			writeToConsole(IConsoleStream.LEVEL_CMD,
					"svn move \"" + FileUtility.normalizePath(srcPath) + "\" \"" + FileUtility.normalizePath(dstPath)
							+ "\"" + ISVNConnector.Options.asCommandLine(
									ISVNConnector.Options.FORCE | ISVNConnector.Options.ALLOW_MIXED_REVISIONS)
							+ " \n");
			proxy.moveLocal(new String[] { srcPath }, dstPath,
					ISVNConnector.Options.FORCE | ISVNConnector.Options.ALLOW_MIXED_REVISIONS,
					new SVNProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t),
				new Object[] { source.getName(), destination.getParent().getFullPath().toString() });
	}

}
