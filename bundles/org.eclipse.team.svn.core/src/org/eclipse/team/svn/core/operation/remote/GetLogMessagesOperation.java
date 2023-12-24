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

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNErrorCodes;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Get repository resource LogMessage's opearation implementation
 * 
 * @author Alexander Gurov
 */
public class GetLogMessagesOperation extends AbstractRepositoryOperation {
	protected SVNLogEntry[] msg;

	protected SVNRevision startRevision;

	protected SVNRevision endRevision;

	protected long limit;

	protected boolean isRetryIfMergeInfoNotSupported;

	protected long options;

	public GetLogMessagesOperation(IRepositoryResource resource) {
		this(resource, false);
	}

	public GetLogMessagesOperation(IRepositoryResource resource, boolean stopOnCopy) {
		this(resource, ISVNConnector.Options.DISCOVER_PATHS
				| (stopOnCopy ? ISVNConnector.Options.STOP_ON_COPY : ISVNConnector.Options.NONE));
	}

	public GetLogMessagesOperation(IRepositoryResource resource, long options) {
		super("Operation_GetLogMessages", SVNMessages.class, new IRepositoryResource[] { resource }); //$NON-NLS-1$
		this.options = options & ISVNConnector.CommandMasks.LIST_HISTORY_LOG;
		limit = 0;
		endRevision = SVNRevision.fromNumber(0);
	}

	public boolean getIncludeMerged() {
		return (options & ISVNConnector.Options.INCLUDE_MERGED_REVISIONS) != 0;
	}

	public void setIncludeMerged(boolean includeMerged) {
		options &= ~ISVNConnector.Options.INCLUDE_MERGED_REVISIONS;
		options |= includeMerged ? ISVNConnector.Options.INCLUDE_MERGED_REVISIONS : ISVNConnector.Options.NONE;
	}

	public void setRetryIfMergeInfoNotSupported(boolean isRetryIfMergeInfoNotSupported) {
		this.isRetryIfMergeInfoNotSupported = isRetryIfMergeInfoNotSupported;
	}

	public boolean getStopOnCopy() {
		return (options & ISVNConnector.Options.STOP_ON_COPY) != 0;
	}

	public void setStopOnCopy(boolean stopOnCopy) {
		options &= ~ISVNConnector.Options.STOP_ON_COPY;
		options |= stopOnCopy ? ISVNConnector.Options.STOP_ON_COPY : ISVNConnector.Options.NONE;
	}

	public boolean getDiscoverPaths() {
		return (options & ISVNConnector.Options.DISCOVER_PATHS) != 0;
	}

	public void setDiscoverPaths(boolean discoverPaths) {
		options &= ~ISVNConnector.Options.DISCOVER_PATHS;
		options |= discoverPaths ? ISVNConnector.Options.DISCOVER_PATHS : ISVNConnector.Options.NONE;
	}

	public long getLimit() {
		return limit;
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}

	public void setStartRevision(SVNRevision revision) {
		startRevision = revision;
	}

	public void setEndRevision(SVNRevision revision) {
		endRevision = revision;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource resource = operableData()[0];
		if (startRevision == null) {
			startRevision = resource.getSelectedRevision();
		}
		IRepositoryLocation location = resource.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn log " + SVNUtility.encodeURL(this.resource.getUrl()) + (this.limit != 0 ? (" --limit " + this.limit) : "") + (this.stopOnCopy ? " --stop-on-copy" : "") + " -r " + this.selectedRevision + ":0 --username \"" + location.getUsername() + "\"\n");
			try {
				msg = SVNUtility.logEntries(proxy, SVNUtility.getEntryReference(resource), startRevision, endRevision,
						options, ISVNConnector.DEFAULT_LOG_ENTRY_PROPS, limit,
						new SVNProgressMonitor(this, monitor, null));
			} catch (SVNConnectorException ex) {
				/*
				 * If SVN server doesn't support merged revisions, then we re-call without this option
				 */
				if (isRetryIfMergeInfoNotSupported && ex.getErrorId() == SVNErrorCodes.unsupportedFeature
						&& (options & Options.INCLUDE_MERGED_REVISIONS) != 0) {
					options &= ~Options.INCLUDE_MERGED_REVISIONS;
					msg = SVNUtility.logEntries(proxy, SVNUtility.getEntryReference(resource), startRevision,
							endRevision, options, ISVNConnector.DEFAULT_LOG_ENTRY_PROPS, limit,
							new SVNProgressMonitor(this, monitor, null));
				} else {
					throw ex;
				}
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	public SVNLogEntry[] getMessages() {
		return msg;
	}

	public IRepositoryResource getResource() {
		return operableData()[0];
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { operableData()[0].getUrl() });
	}

}
