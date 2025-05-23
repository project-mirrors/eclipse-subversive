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
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Create diff between two repository resources. Will be usefull, for example, in order to collect differences between trunk and branch.
 * 
 * @author Alexander Gurov
 */
public class CreatePatchOperation extends AbstractRepositoryOperation {
	protected String fileName;

	protected boolean recurse;

	protected long options;

	protected long diffOptions;

	public CreatePatchOperation(IRepositoryResource first, IRepositoryResource second, String fileName, boolean recurse,
			boolean ignoreDeleted, boolean processBinary) {
		this(first, second, fileName, recurse, ignoreDeleted, processBinary, true);
	}

	public CreatePatchOperation(IRepositoryResource first, IRepositoryResource second, String fileName, boolean recurse,
			boolean ignoreDeleted, boolean processBinary, boolean ignoreAncestry) {
		this(first, second, fileName, recurse,
				(ignoreDeleted ? ISVNConnector.Options.SKIP_DELETED : ISVNConnector.Options.NONE)
						| (processBinary ? ISVNConnector.Options.FORCE : ISVNConnector.Options.NONE)
						| (ignoreAncestry ? ISVNConnector.Options.IGNORE_ANCESTRY : ISVNConnector.Options.NONE),
				ISVNConnector.DiffOptions.NONE);
	}

	public CreatePatchOperation(IRepositoryResource first, IRepositoryResource second, String fileName, boolean recurse,
			long options, long diffOptions) {
		super("Operation_CreatePatchRemote", SVNMessages.class, new IRepositoryResource[] { first, second }); //$NON-NLS-1$
		this.fileName = fileName;
		this.recurse = recurse;
		this.options = options & ISVNConnector.CommandMasks.DIFF;
		this.diffOptions = diffOptions;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource first = operableData()[0];
		IRepositoryResource second = operableData()[1];
		IRepositoryLocation location = first.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			SVNEntryRevisionReference ref1 = SVNUtility.getEntryRevisionReference(first);
			SVNEntryRevisionReference ref2 = SVNUtility.getEntryRevisionReference(second);
			if (SVNUtility.useSingleReferenceSignature(ref1, ref2)) {
				writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn diff -r " + ref1.revision + ":" + ref2.revision + " \"" + first.getUrl() + "@" //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
								+ ref1.pegRevision + "\"" + (recurse ? "" : " -N") //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
								+ ISVNConnector.Options.asCommandLine(options)
								+ ISVNConnector.DiffOptions.asCommandLine(diffOptions)
								+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
				proxy.diff(ref1, new SVNRevisionRange(ref1.revision, ref2.revision), null, fileName,
						recurse ? SVNDepth.INFINITY : SVNDepth.IMMEDIATES, options, null, diffOptions,
						new SVNProgressMonitor(this, monitor, null));
			} else {
				writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn diff \"" + first.getUrl() + "@" + first.getSelectedRevision() + "\" \"" + second.getUrl() //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
								+ "@" + second.getSelectedRevision() + "\"" + (recurse ? "" : " -N") //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
								+ ISVNConnector.Options.asCommandLine(options)
								+ ISVNConnector.DiffOptions.asCommandLine(diffOptions)
								+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
				proxy.diffTwo(ref1, ref2, null, fileName, recurse ? SVNDepth.INFINITY : SVNDepth.IMMEDIATES, options,
						null, diffOptions, new SVNProgressMonitor(this, monitor, null));
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

}
