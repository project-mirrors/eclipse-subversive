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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * Create patch operation implementation
 * 
 * @author Alexander Gurov
 */
public class CreatePatchOperation extends AbstractFileOperation {
	protected String fileName;

	protected boolean useRelativePath;

	protected SVNDepth depth;

	protected long options;

	protected long diffOptions;

	public CreatePatchOperation(File file, String fileName, boolean recurse, boolean ignoreDeleted,
			boolean processBinary, boolean useRelativePath) {
		this(file, fileName, SVNDepth.infinityOrFiles(recurse),
				(ignoreDeleted ? ISVNConnector.Options.SKIP_DELETED : ISVNConnector.Options.NONE)
						| (processBinary ? ISVNConnector.Options.FORCE : ISVNConnector.Options.NONE),
				useRelativePath, ISVNConnector.DiffOptions.NONE);
	}

	public CreatePatchOperation(File file, String fileName, SVNDepth depth, long options, boolean useRelativePath,
			long diffOptions) {
		super("Operation_CreatePatchFile", SVNMessages.class, new File[] { file }); //$NON-NLS-1$
		this.fileName = fileName;
		this.depth = depth;
		this.options = options | ISVNConnector.Options.IGNORE_ANCESTRY;
		this.useRelativePath = useRelativePath;
		this.diffOptions = diffOptions;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File file = operableData()[0];
		IRepositoryResource remote = SVNFileStorage.instance().asRepositoryResource(file, false);
		ISVNConnector proxy = remote.getRepositoryLocation().acquireSVNProxy();
		try {
			writeToConsole(IConsoleStream.LEVEL_CMD, "svn diff " + (depth == SVNDepth.INFINITY ? "" : " -N") //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					+ ISVNConnector.Options.asCommandLine(options) + "\n"); //$NON-NLS-1$
			String path = file.getAbsolutePath();
			proxy.diffTwo(
					new SVNEntryRevisionReference(path, null, SVNRevision.BASE),
					new SVNEntryRevisionReference(path, null, SVNRevision.WORKING), useRelativePath ? path : null,
					fileName, depth, options, null, diffOptions, new SVNProgressMonitor(this, monitor, null));
		} finally {
			remote.getRepositoryLocation().releaseSVNProxy(proxy);
		}
	}

}
