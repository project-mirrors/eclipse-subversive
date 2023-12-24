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
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Operation organize export local folder to repository
 * 
 * @author Sergiy Logvin
 */
public class ExportOperation extends AbstractRepositoryOperation {
	protected String path;

	protected SVNDepth depth;

	protected boolean ignoreExternals;

	public ExportOperation(IRepositoryResource[] resources, String path, SVNDepth depth, boolean ignoreExternals) {
		super("Operation_ExportRevision", SVNMessages.class, resources); //$NON-NLS-1$
		this.path = path;
		this.depth = depth;
		this.ignoreExternals = ignoreExternals;
	}

	public ExportOperation(IRepositoryResourceProvider provider, String path, SVNDepth depth, boolean ignoreExternals) {
		super("Operation_ExportRevision", SVNMessages.class, provider); //$NON-NLS-1$
		this.path = path;
		this.depth = depth;
		this.ignoreExternals = ignoreExternals;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource[] resources = operableData();
		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			IRepositoryLocation location = resources[i].getRepositoryLocation();
			final ISVNConnector proxy = location.acquireSVNProxy();
			final String path = this.path + "/" + resources[i].getName(); //$NON-NLS-1$
			final SVNEntryRevisionReference entryRef = SVNUtility.getEntryRevisionReference(resources[i]);
			writeToConsole(IConsoleStream.LEVEL_CMD, "svn export \"" + resources[i].getUrl() + "@" //$NON-NLS-1$//$NON-NLS-2$
					+ resources[i].getPegRevision() + "\" -r " //$NON-NLS-1$
					+ resources[i].getSelectedRevision() + " \"" + FileUtility.normalizePath(path) + "\"" //$NON-NLS-1$//$NON-NLS-2$
					+ SVNUtility.getDepthArg(ExportOperation.this.depth, ISVNConnector.Options.NONE)
					+ ISVNConnector.Options.asCommandLine(ISVNConnector.Options.FORCE
							| (ignoreExternals ? ISVNConnector.Options.IGNORE_EXTERNALS : ISVNConnector.Options.NONE))
					+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$

			this.protectStep(monitor1 -> {
				long options = ISVNConnector.Options.FORCE;
				if (ignoreExternals) {
					options |= ISVNConnector.Options.IGNORE_EXTERNALS;
				}
				proxy.exportTo(entryRef, path, null, depth, options,
						new SVNProgressMonitor(ExportOperation.this, monitor1, null));
			}, monitor, resources.length);

			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { operableData()[0].getUrl() });
	}

}
