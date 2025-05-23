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

package org.eclipse.team.svn.core.operation.local;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Get remote resource contents without working copy modification
 * 
 * @author Alexander Gurov
 */
public class GetRemoteContentsOperation extends AbstractWorkingCopyOperation {
	protected IRepositoryResourceProvider provider;

	protected HashMap<String, String> remotePath2localPath;

	protected long options;

	public GetRemoteContentsOperation(IResource[] resources, final IRepositoryResource[] remoteResources,
			HashMap<String, String> remotePath2localPath, boolean ignoreExternals) {
		this(resources, (IRepositoryResourceProvider) () -> remoteResources, remotePath2localPath, ignoreExternals);
	}

	public GetRemoteContentsOperation(IResource[] resources, IRepositoryResourceProvider provider,
			HashMap<String, String> remotePath2localPath, boolean ignoreExternals) {
		this(resources, provider, remotePath2localPath, ISVNConnector.Options.FORCE
				| (ignoreExternals ? ISVNConnector.Options.IGNORE_EXTERNALS : ISVNConnector.Options.NONE));
	}

	public GetRemoteContentsOperation(IResource[] resources, IRepositoryResourceProvider provider,
			HashMap<String, String> remotePath2localPath, long options) {
		super("Operation_GetContent", SVNMessages.class, resources); //$NON-NLS-1$
		this.provider = provider;
		this.remotePath2localPath = remotePath2localPath;
		this.options = options & ISVNConnector.CommandMasks.EXPORT;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource[] remoteResources = provider.getRepositoryResources();
		for (int i = 0; i < remoteResources.length && !monitor.isCanceled(); i++) {
			final IRepositoryResource remote = remoteResources[i];
			this.protectStep(monitor1 -> GetRemoteContentsOperation.this.doGet(remote, monitor1), monitor, remoteResources.length);
		}
	}

	protected void doGet(IRepositoryResource remote, IProgressMonitor monitor) throws Exception {
		IRepositoryLocation location = remote.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			String url = SVNUtility.encodeURL(remote.getUrl());
			String wcPath = remotePath2localPath.get(url);
			if (remote instanceof IRepositoryFile) {
				File parent = new File(wcPath.substring(0, wcPath.lastIndexOf("/"))); //$NON-NLS-1$
				if (!parent.exists()) {
					parent.mkdirs();
				}
				File file = new File(wcPath);
				if (!file.exists()) {
					file.createNewFile();
				}
				FileOutputStream stream = null;
				try {
					writeToConsole(IConsoleStream.LEVEL_CMD,
							"svn cat " + url + "@" + remote.getPegRevision() + " -r " + remote.getSelectedRevision() //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
									+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
					stream = new FileOutputStream(wcPath);
					proxy.streamFileContent(SVNUtility.getEntryRevisionReference(remote), 2048, stream,
							new SVNProgressMonitor(this, monitor, null));
				} catch (FileNotFoundException e) {
					//skip read-only files
				} finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (Exception ex) {
						}
					}
				}
			} else {
				File directory = new File(wcPath);
				if (!directory.exists()) {
					directory.mkdirs();
				}
				writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn export " + url + "@" + remote.getPegRevision() + " -r " + remote.getSelectedRevision() //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
								+ ISVNConnector.Options.asCommandLine(options) + " \"" + wcPath + "\" " //$NON-NLS-1$//$NON-NLS-2$
								+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
				proxy.exportTo(SVNUtility.getEntryRevisionReference(remote), wcPath, null, SVNDepth.INFINITY, options,
						new SVNProgressMonitor(this, monitor, null));
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t),
				new Object[] { FileUtility.getNamesListAsString(provider.getRepositoryResources()) });
	}

}
