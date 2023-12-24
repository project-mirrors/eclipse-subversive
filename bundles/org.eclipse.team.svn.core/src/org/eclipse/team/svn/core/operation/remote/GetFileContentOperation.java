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

import java.io.FileOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.operation.AbstractGetFileContentOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * This operation retrieve a repository file content
 * 
 * @author Alexander Gurov
 */
public class GetFileContentOperation extends AbstractGetFileContentOperation {
	protected IRepositoryResource resource;

	protected IRepositoryResourceProvider provider;

	public GetFileContentOperation(IRepositoryResource resource) {
		super("Revision"); //$NON-NLS-1$
		this.resource = resource;
	}

	public GetFileContentOperation(IRepositoryResourceProvider provider) {
		super("Revision"); //$NON-NLS-1$
		this.provider = provider;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (provider != null) {
			resource = provider.getRepositoryResources()[0];
		}

		String url = resource.getUrl();
		IRepositoryLocation location = resource.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		FileOutputStream stream = null;
		try {
			url = SVNUtility.encodeURL(url);
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn cat " + url + "@" + this.resource.getPegRevision() + " -r " + selected + " --username \"" + location.getUsername() + "\"\n");
			tmpFile = createTempFile();
			stream = new FileOutputStream(tmpFile);

			proxy.streamFileContent(
					SVNUtility.getEntryRevisionReference(resource), 2048, stream,
					new SVNProgressMonitor(GetFileContentOperation.this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception ex) {
				}
			}
		}
	}

	@Override
	protected String getExtension() {
		String name = resource.getName();
		int idx = name.lastIndexOf('.');
		return idx == -1 ? "" : name.substring(idx + 1); //$NON-NLS-1$
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { resource.getUrl() });
	}

}
