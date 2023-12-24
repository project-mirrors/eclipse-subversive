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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Export local resources
 * 
 * @author Alexander Gurov
 */
public class ExportOperation extends AbstractWorkingCopyOperation {
	protected SVNRevision revision;

	protected String path;

	protected long options;

	public ExportOperation(IResource[] resources, String path, SVNRevision revision, boolean ignoreExternals) {
		this(resources, path, revision, ISVNConnector.Options.FORCE
				| (ignoreExternals ? ISVNConnector.Options.IGNORE_EXTERNALS : ISVNConnector.Options.NONE));
	}

	public ExportOperation(IResourceProvider provider, String path, SVNRevision revision, boolean ignoreExternals) {
		this(provider, path, revision, ISVNConnector.Options.FORCE
				| (ignoreExternals ? ISVNConnector.Options.IGNORE_EXTERNALS : ISVNConnector.Options.NONE));
	}

	public ExportOperation(IResource[] resources, String path, SVNRevision revision, long options) {
		super("Operation_ExportRevision", SVNMessages.class, resources); //$NON-NLS-1$
		this.revision = revision;
		this.path = path;
		this.options = options & ISVNConnector.CommandMasks.EXPORT;
	}

	public ExportOperation(IResourceProvider provider, String path, SVNRevision revision, long options) {
		super("Operation_ExportRevision", SVNMessages.class, provider); //$NON-NLS-1$
		this.revision = revision;
		this.path = path;
		this.options = options & ISVNConnector.CommandMasks.EXPORT;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();
		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final IResource current = resources[i];
			final IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(current);
			final ISVNConnector proxy = location.acquireSVNProxy();

			this.protectStep(monitor1 -> {
				String wcPath = FileUtility.getWorkingCopyPath(current);
				String targetPath = path + "/" + current.getName(); //$NON-NLS-1$
				ExportOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn export \"" + wcPath + "\" -r " + revision.toString() //$NON-NLS-1$//$NON-NLS-2$
								+ ISVNConnector.Options.asCommandLine(options) + " \"" //$NON-NLS-1$
								+ FileUtility.normalizePath(targetPath) + "\" " //$NON-NLS-1$
								+ FileUtility.getUsernameParam(location.getUsername()) + "\n");
				proxy.exportTo(new SVNEntryRevisionReference(wcPath, null, revision), targetPath, null,
						SVNDepth.INFINITY, options, new SVNProgressMonitor(ExportOperation.this, monitor1, null));
			}, monitor, resources.length);

			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t),
				new Object[] { FileUtility.getNamesListAsString(operableData()) });
	}

}
