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
import java.io.FileOutputStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.AbstractGetFileContentOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * This operation retrieve a local file content of selected revision: BASE or WORKING
 * 
 * @author Alexander Gurov
 */
public class GetLocalFileContentOperation extends AbstractGetFileContentOperation {
	protected IResource resource;

	protected SVNRevision revision;

	public GetLocalFileContentOperation(IResource resource, SVNRevision.Kind revisionKind) {
		super("Local"); //$NON-NLS-1$
		this.resource = resource;
		revision = revisionKind == Kind.BASE ? SVNRevision.BASE : SVNRevision.WORKING;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (revision.getKind() == Kind.BASE) {
			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resource);
			ISVNConnector proxy = location.acquireSVNProxy();
			FileOutputStream stream = null;
			try {
				tmpFile = createTempFile();
				stream = new FileOutputStream(tmpFile);
				proxy.streamFileContent(
						new SVNEntryRevisionReference(FileUtility.getWorkingCopyPath(resource), null, revision), 2048,
						stream, new SVNProgressMonitor(GetLocalFileContentOperation.this, monitor, null));
			} finally {
				location.releaseSVNProxy(proxy);
				if (stream != null) {
					try {
						stream.close();
					} catch (Exception ex) {
					}
				}
			}
		} else {
			tmpFile = new File(FileUtility.getWorkingCopyPath(resource));
		}
	}

	@Override
	protected String getExtension() {
		return resource.getFileExtension();
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { resource.getName() });
	}

}
