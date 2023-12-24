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

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.IRevisionProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Create new remote file by importing it from local file system
 * 
 * @author Sergiy Logvin
 */
public class CreateFileOperation extends AbstractRepositoryOperation implements IRevisionProvider {
	protected String path;

	protected String message;

	protected String[] fileNames;

	protected RevisionPair[] revisionPair;

	public CreateFileOperation(IRepositoryResource resource, String path, String message, String[] fileNames) {
		super("Operation_CreateFile", SVNMessages.class, new IRepositoryResource[] { resource }); //$NON-NLS-1$
		this.path = path;
		this.message = message;
		this.fileNames = fileNames;
	}

	@Override
	public RevisionPair[] getRevisions() {
		return revisionPair;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		final IRepositoryResource resource = operableData()[0];
		final IRepositoryLocation location = resource.getRepositoryLocation();
		revisionPair = new RevisionPair[1];
		final ISVNConnector proxy = location.acquireSVNProxy();
		ISVNNotificationCallback notify = info -> {
			if (info.revision != SVNRevision.INVALID_REVISION_NUMBER) {
				String[] path = { resource.getUrl() };
				revisionPair[0] = new RevisionPair(info.revision, path, location);
				String message = BaseMessages.format(SVNMessages.Console_CommittedRevision,
						new String[] { String.valueOf(info.revision) });
				CreateFileOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
			}
		};
		try {
			SVNUtility.addSVNNotifyListener(proxy, notify);
			for (String fileName : fileNames) {
				final String[] currentFile = { fileName };
				this.protectStep(monitor1 -> {
					String path = FileUtility.normalizePath(CreateFileOperation.this.path + "/" + currentFile[0]); //$NON-NLS-1$
					String url = resource.getUrl() + "/" + currentFile[0]; //$NON-NLS-1$
					CreateFileOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
							"svn import \"" + path + "\" \"" + url + "\" -m \"" + message //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
									+ "\"" //$NON-NLS-1$
									+ ISVNConnector.Options.asCommandLine(ISVNConnector.Options.INCLUDE_IGNORED
											| ISVNConnector.Options.IGNORE_UNKNOWN_NODE_TYPES)
									+ FileUtility.getUsernameParam(location.getUsername()) + " -N\n"); //$NON-NLS-1$
					proxy.importTo(path, SVNUtility.encodeURL(url), message, SVNDepth.FILES,
							ISVNConnector.Options.INCLUDE_IGNORED | ISVNConnector.Options.IGNORE_UNKNOWN_NODE_TYPES,
							null, null, new SVNProgressMonitor(CreateFileOperation.this, monitor1, null));
				}, monitor, fileNames.length);
			}
		} finally {
			SVNUtility.removeSVNNotifyListener(proxy, notify);
			location.releaseSVNProxy(proxy);
		}
	}

}