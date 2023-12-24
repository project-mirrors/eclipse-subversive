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

package org.eclipse.team.svn.core.history;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNLogPath;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Resource revision based on the resource history
 * 
 * @author Alexander Gurov
 */
public class SVNRemoteResourceRevision extends FileRevision {
	protected SVNLogEntry msg;

	protected IRepositoryResource remote;

	protected boolean isDeletionRev;

	public SVNRemoteResourceRevision(IRepositoryResource remote, SVNLogEntry msg) {
		this.msg = msg;
		this.remote = SVNUtility.copyOf(remote);
		this.remote.setSelectedRevision(SVNRevision.fromNumber(msg.revision));
		if (this.msg.changedPaths != null) {
			for (int i = 0; i < this.msg.changedPaths.length && !isDeletionRev; i++) {
				if (this.msg.changedPaths[i].action == SVNLogPath.ChangeType.DELETED
						&& this.remote.getUrl().endsWith(this.msg.changedPaths[i].path)) {
					isDeletionRev = true;
				}
			}
		}
	}

	@Override
	public URI getURI() {
		try {
			return new URI(remote.getUrl());
		} catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public long getTimestamp() {
		return msg.date == 0 ? -1 : msg.date;
	}

	@Override
	public boolean exists() {
		return !isDeletionRev;
	}

	@Override
	public String getContentIdentifier() {
		return String.valueOf(msg.revision);
	}

	@Override
	public String getAuthor() {
		return msg.author;
	}

	@Override
	public String getComment() {
		return msg.message;
	}

	@Override
	public String getName() {
		return remote.getName();
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		ResourceContentStorage retVal = new ResourceContentStorage(remote);
		retVal.fetchContents(monitor);
		return retVal;
	}

	@Override
	public boolean isPropertyMissing() {
		return false;
	}

	@Override
	public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
		return this;
	}

}
