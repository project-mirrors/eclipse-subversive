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
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * This operation retrieve a content of the file in selected revision
 * 
 * @author Alexander Gurov
 */
public class GetFileContentOperation extends AbstractFileOperation {
	public static final int DEFAULT_BUFFER_SIZE = 2048;

	protected SVNRevision revision;

	protected SVNRevision pegRevision;

	protected int bufferSize;

	protected OutputStream target;

	public GetFileContentOperation(File file, SVNRevision revision, SVNRevision pegRevision, OutputStream target) {
		this(file, revision, pegRevision, GetFileContentOperation.DEFAULT_BUFFER_SIZE, target);
	}

	public GetFileContentOperation(File file, SVNRevision revision, SVNRevision pegRevision, int bufferSize,
			OutputStream target) {
		super("Operation_GetFileContent", SVNMessages.class, new File[] { file }); //$NON-NLS-1$
		this.revision = revision;
		this.pegRevision = pegRevision;
		this.bufferSize = bufferSize;
		this.target = target;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File file = operableData()[0];

		IRepositoryResource remote = SVNFileStorage.instance().asRepositoryResource(file, false);
		ISVNConnector proxy = remote.getRepositoryLocation().acquireSVNProxy();
		try {
			SVNRevision.Kind kind = revision.getKind();
			if (kind == Kind.BASE || kind == Kind.WORKING) {
				proxy.streamFileContent(new SVNEntryRevisionReference(file.getAbsolutePath(), null, revision),
						bufferSize, target, new SVNProgressMonitor(this, monitor, null));
			} else {
				proxy.streamFileContent(SVNUtility.getEntryRevisionReference(remote), bufferSize, target,
						new SVNProgressMonitor(this, monitor, null));
			}
		} finally {
			remote.getRepositoryLocation().releaseSVNProxy(proxy);
		}
	}

}
