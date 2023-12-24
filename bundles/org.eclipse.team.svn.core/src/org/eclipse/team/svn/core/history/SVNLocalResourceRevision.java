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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractGetFileContentOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.GetLocalFileContentOperation;
import org.eclipse.team.svn.core.operation.remote.GetLogMessagesOperation;
import org.eclipse.team.svn.core.resource.ILocalFolder;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Resource revision based on the local info
 * 
 * @author Alexander Gurov
 */
public class SVNLocalResourceRevision extends FileRevision {
	protected ILocalResource local;

	protected SVNRevision rev;

	public SVNLocalResourceRevision(ILocalResource local, SVNRevision rev) {
		this.local = local;
		this.rev = rev;
	}

	@Override
	public URI getURI() {
		return local.getResource().getLocationURI();
	}

	@Override
	public long getTimestamp() {
		return !IStateFilter.SF_ONREPOSITORY.accept(local) ? -1 : local.getLastCommitDate();
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public String getContentIdentifier() {
		if (IStateFilter.SF_UNVERSIONED.accept(local)) {
			return SVNMessages.ResourceVariant_unversioned;
		}
		long revision = local.getRevision();
		if (IStateFilter.SF_DELETED.accept(local) && revision == SVNRevision.INVALID_REVISION_NUMBER) {
			return SVNMessages.ResourceVariant_deleted;
		}
		return String.valueOf(revision);
	}

	@Override
	public String getAuthor() {
		return local.getAuthor();
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public String getName() {
		return local.getName();
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		if (local instanceof ILocalFolder) {
			return null;
		}
		if (IStateFilter.SF_UNVERSIONED.accept(local) && !IStateFilter.SF_PREREPLACED.accept(local)) {
			return (IStorage) local.getResource();
		}
		return new LocalStorage();
	}

	@Override
	public boolean isPropertyMissing() {
		return IStateFilter.SF_ONREPOSITORY.accept(local);
	}

	@Override
	public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
		if (!IStateFilter.SF_ONREPOSITORY.accept(local)) {
			return this;
		}
		IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(local.getResource());
		remote.setSelectedRevision(SVNRevision.fromNumber(local.getRevision()));
		GetLogMessagesOperation log = new GetLogMessagesOperation(remote);
		log.setLimit(1);
		ProgressMonitorUtility.doTaskExternal(log, monitor);
		if (log.getExecutionState() == IActionOperation.OK && log.getMessages().length > 0) {
			return new SVNRemoteResourceRevision(remote, log.getMessages()[0]);
		}
		return null;
	}

	protected class LocalStorage extends ResourceContentStorage {
		public LocalStorage() {
			super(SVNRemoteStorage.instance().asRepositoryResource(local.getResource()));
		}

		@Override
		public IPath getFullPath() {
			return local.getResource().getLocation();
		}

		@Override
		protected AbstractGetFileContentOperation getLoadContentOperation() {
			return new GetLocalFileContentOperation(local.getResource(), rev.getKind());
		}

	}
}
