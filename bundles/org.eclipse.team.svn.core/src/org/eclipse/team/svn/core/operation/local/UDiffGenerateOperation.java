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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Generate file in unified diff format operation
 * 
 * @author Igor Burilo
 */
public class UDiffGenerateOperation extends AbstractActionOperation {

	protected ILocalResource local;

	protected IRepositoryResource remote;

	protected String diffFile;

	public UDiffGenerateOperation(ILocalResource local, IRepositoryResource remote, String diffFile) {
		super("Operation_UDiffGenerate", SVNMessages.class); //$NON-NLS-1$
		this.local = local;
		this.remote = remote;
		this.diffFile = diffFile;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(local.getResource());
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			String wcPath = FileUtility.getWorkingCopyPath(local.getResource());
			SVNEntryRevisionReference refPrev = new SVNEntryRevisionReference(wcPath, null, SVNRevision.WORKING);
			SVNEntryRevisionReference refNext = SVNUtility.getEntryRevisionReference(remote);

			String projectPath = FileUtility.getWorkingCopyPath(local.getResource().getProject());
			String relativeToDir = projectPath;

			SVNDepth depth = SVNDepth.INFINITY;
			long options = ISVNConnector.Options.NONE;
			//ISVNConnector.Options.IGNORE_ANCESTRY;
			String[] changelistNames = {};

			writeToConsole(
					IConsoleStream.LEVEL_CMD, "svn diff -r " //$NON-NLS-1$
							+ refNext.revision + " \"" + wcPath + "\"" //$NON-NLS-1$ //$NON-NLS-2$
							//+ "@" + refNext.pegRevision + "\""
							+ ISVNConnector.Options.asCommandLine(options)
							+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$

			proxy.diffTwo(refPrev, refNext, relativeToDir, diffFile, depth, options, changelistNames,
					ISVNConnector.Options.NONE, new SVNProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

}
