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

package org.eclipse.team.svn.core.synchronize.variant;

import java.io.ByteArrayInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.GetFileContentOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Remote file revision variant
 * 
 * @author Alexander Gurov
 */
public class RemoteFileVariant extends RemoteResourceVariant {

	public RemoteFileVariant(ILocalResource local) {
		super(local);
	}

	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException {
		if (!local.isCopied() && local.getRevision() == SVNRevision.INVALID_REVISION_NUMBER
				|| (IStateFilter.SF_DELETED.accept(local) || IStateFilter.SF_TREE_CONFLICTING.accept(local)
						&& !IStateFilter.SF_TREE_CONFLICTING_REPOSITORY_EXIST.accept(local))
						&& !IStateFilter.SF_REPLACED.accept(local)) {
			setContents(new ByteArrayInputStream(new byte[0]), monitor);
			return;
		}
		IRepositoryResource remote = null;
		if (local.isCopied()) {
			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(local.getResource());
			SVNEntryInfo[] st = SVNUtility
					.info(new SVNEntryRevisionReference(FileUtility.getWorkingCopyPath(local.getResource())));
			remote = location.asRepositoryFile(st[0].copyFromUrl, false);
			remote.setSelectedRevision(SVNRevision.fromNumber(st[0].copyFromRevision));
			remote.setPegRevision(SVNRevision.fromNumber(st[0].copyFromRevision));
		} else {
			remote = SVNRemoteStorage.instance().asRepositoryResource(local.getResource());
			remote.setSelectedRevision(SVNRevision.fromNumber(local.getRevision()));
			remote.setPegRevision(((IResourceChange) local).getPegRevision());
			if (local instanceof IResourceChange) {
				IRepositoryResource originator = ((IResourceChange) local).getOriginator();
				if (originator != null) {
					remote = originator;
				}
			}
		}

		GetFileContentOperation op = new GetFileContentOperation(remote);
		ProgressMonitorUtility.doTaskExternal(op, monitor);
		if (op.getExecutionState() == IActionOperation.OK) {
			setContents(op.getContent(), monitor);
		}
	}

	@Override
	public boolean isContainer() {
		return false;
	}

}
