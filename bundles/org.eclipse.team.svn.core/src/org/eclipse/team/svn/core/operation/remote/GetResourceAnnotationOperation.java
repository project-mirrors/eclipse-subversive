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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNAnnotationCallback;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.connector.SVNAnnotationData;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNErrorCodes;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Resource annotation operation implementation
 * 
 * @author Alexander Gurov
 */
public class GetResourceAnnotationOperation extends AbstractRepositoryOperation {
	protected SVNAnnotationData[] annotatedLines;

	protected byte[] content;

	protected long options;

	protected SVNRevisionRange revisions;

	protected boolean isRetryIfMergeInfoNotSupported;

	public GetResourceAnnotationOperation(IRepositoryResource resource, SVNRevisionRange revisions) {
		this(resource, revisions, ISVNConnector.Options.IGNORE_MIME_TYPE);
	}

	public GetResourceAnnotationOperation(IRepositoryResource resource, SVNRevisionRange revisions, long options) {
		super("Operation_GetAnnotation", SVNMessages.class, new IRepositoryResource[] { resource }); //$NON-NLS-1$
		this.revisions = revisions;
		this.options = options & ISVNConnector.CommandMasks.ANNOTATE;
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

	public IRepositoryResource getRepositoryResource() {
		return operableData()[0];
	}

	public SVNAnnotationData[] getAnnotatedLines() {
		return annotatedLines;
	}

	public byte[] getContent() {
		return content;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();
		final ArrayList<SVNAnnotationData> lines = new ArrayList<>();
		IRepositoryResource resource = operableData()[0];
		IRepositoryLocation location = resource.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn blame " + url + "@" + resource.getPegRevision() + " -r 0:" + resource.getSelectedRevision() + " --username \"" + location.getUsername() + "\"\n");

			ISVNAnnotationCallback callback = (line, data) -> {
				lines.add(data);
				try {
					stream.write((line + "\n").getBytes()); //$NON-NLS-1$
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			};

			try {
				proxy.annotate(
						SVNUtility.getEntryReference(resource), new SVNRevisionRange(revisions.from, revisions.to),
						options, ISVNConnector.DiffOptions.NONE, callback, new SVNProgressMonitor(this, monitor, null));
			} catch (SVNConnectorException ex) {
				/*
				 * If SVN server doesn't support merged revisions, then we re-call without this option
				 */
				if (isRetryIfMergeInfoNotSupported && ex.getErrorId() == SVNErrorCodes.unsupportedFeature
						&& (options & Options.INCLUDE_MERGED_REVISIONS) != 0) {
					options &= ~Options.INCLUDE_MERGED_REVISIONS;
					proxy.annotate(
							SVNUtility.getEntryReference(resource), new SVNRevisionRange(revisions.from, revisions.to),
							options, ISVNConnector.DiffOptions.NONE, callback,
							new SVNProgressMonitor(this, monitor, null));
				} else {
					throw ex;
				}
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}
		annotatedLines = lines.toArray(new SVNAnnotationData[lines.size()]);
		content = stream.toByteArray();
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		if (t instanceof SVNConnectorException
				&& ((SVNConnectorException) t).getErrorId() == SVNErrorCodes.clientIsBinaryFile) {
			return getOperationResource("Error_IsBinary"); //$NON-NLS-1$
		}
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { operableData()[0].getName() });
	}

}
