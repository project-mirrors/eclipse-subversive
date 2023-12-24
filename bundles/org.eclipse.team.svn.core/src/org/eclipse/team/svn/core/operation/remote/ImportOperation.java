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
import org.eclipse.team.svn.core.connector.ISVNImportFilterCallback;
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
 * Operation organize import repository resources to file sistem
 * 
 * @author Sergiy Logvin
 */
public class ImportOperation extends AbstractRepositoryOperation implements IRevisionProvider {
	protected String path;

	protected String message;

	protected SVNDepth depth;

	protected RevisionPair[] revisionPair;

	protected ISVNImportFilterCallback filter;

	public ImportOperation(IRepositoryResource resource, String path, String message, SVNDepth depth) {
		this(resource, path, message, depth, null);
	}

	public ImportOperation(IRepositoryResource resource, String path, String message, SVNDepth depth,
			ISVNImportFilterCallback filter) {
		super("Operation_Import", SVNMessages.class, new IRepositoryResource[] { resource }); //$NON-NLS-1$
		this.path = path;
		this.message = message;
		this.depth = depth;
		this.filter = filter;
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
		ISVNConnector proxy = location.acquireSVNProxy();
		ISVNNotificationCallback notify = info -> {
			if (info.revision != SVNRevision.INVALID_REVISION_NUMBER) {
				String[] path = { resource.getUrl() };
				revisionPair[0] = new RevisionPair(info.revision, path, location);
				String message = BaseMessages.format(SVNMessages.Console_CommittedRevision,
						new String[] { String.valueOf(info.revision) });
				ImportOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
			}
		};
		try {
			SVNUtility.addSVNNotifyListener(proxy, notify);
			writeToConsole(IConsoleStream.LEVEL_CMD, "svn import \"" + FileUtility.normalizePath(path) //$NON-NLS-1$
					+ "\" \"" + SVNUtility.getDepthArg(depth, ISVNConnector.Options.NONE) //$NON-NLS-1$
					+ ISVNConnector.Options.asCommandLine(
							ISVNConnector.Options.INCLUDE_IGNORED | ISVNConnector.Options.IGNORE_UNKNOWN_NODE_TYPES)
					+ " -m \"" + message + "\"" + FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			proxy.importTo(path, SVNUtility.encodeURL(resource.getUrl()), message, depth,
					ISVNConnector.Options.INCLUDE_IGNORED | ISVNConnector.Options.IGNORE_UNKNOWN_NODE_TYPES, null,
					filter, new SVNProgressMonitor(this, monitor, null));
		} finally {
			SVNUtility.removeSVNNotifyListener(proxy, notify);
			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { operableData()[0].getUrl() });
	}

}
