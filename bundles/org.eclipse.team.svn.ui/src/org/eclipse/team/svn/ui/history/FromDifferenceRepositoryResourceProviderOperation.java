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
package org.eclipse.team.svn.ui.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNDiffStatus;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntry.Kind;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.remote.AbstractRepositoryOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.resource.IRepositoryResourceWithStatusProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * @author Igor Burilo
 */
public class FromDifferenceRepositoryResourceProviderOperation extends AbstractRepositoryOperation
		implements IRepositoryResourceWithStatusProvider {
	protected IRepositoryResource[] repositoryResources;

	protected IRepositoryResource[] repositoryResourcesToDelete;

	protected IRepositoryResource newer;

	protected IRepositoryResource older;

	protected IRepositoryLocation location;

	protected HashMap<String, String> url2status;

	public FromDifferenceRepositoryResourceProviderOperation(IRepositoryResource first, IRepositoryResource second) {
		this(new IRepositoryResourceProvider.DefaultRepositoryResourceProvider(
				new IRepositoryResource[] { first, second }));
	}

	public FromDifferenceRepositoryResourceProviderOperation(IRepositoryResourceProvider provider) {//(HashMap<SVNLogPath, Long> paths, SVNLogEntry selectedLogEntry) {
		super("Operation_GetRepositoryResource", SVNUIMessages.class, provider); //$NON-NLS-1$
		url2status = new HashMap<>();
	}

	public IRepositoryResourceProvider getDeletionsProvider() {
		return () -> repositoryResourcesToDelete;
	}

	protected IRepositoryResource createResourceFor(Kind kind, String url) {
		IRepositoryResource retVal = null;
		if (kind == SVNEntry.Kind.FILE) {
			retVal = location.asRepositoryFile(url, false);
		} else if (kind == SVNEntry.Kind.DIR) {
			retVal = location.asRepositoryContainer(url, false);
		}
		if (retVal == null) {
			throw new RuntimeException(SVNUIMessages.Error_CompareUnknownNodeKind);
		}
		return retVal;
	}

	protected IRepositoryResource getResourceForStatus(SVNDiffStatus status) {
		String url = SVNUtility.decodeURL(status.pathNext);
		return createResourceFor(SVNUtility.getNodeKind(status.pathPrev, status.nodeKind, false), url);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource[] operable = operableData();
		newer = operable[0];
		older = operable[1];
		location = newer.getRepositoryLocation();

		HashSet<IRepositoryResource> resourcesToReturn = new HashSet<>();
		HashSet<IRepositoryResource> resourcesToDelete = new HashSet<>();
		ArrayList<SVNDiffStatus> statusesList = new ArrayList<>();
		ISVNConnector proxy = location.acquireSVNProxy();
		SVNEntryRevisionReference refPrev = SVNUtility.getEntryRevisionReference(older);
		SVNEntryRevisionReference refNext = SVNUtility.getEntryRevisionReference(newer);
		ProgressMonitorUtility.setTaskInfo(monitor, this, SVNMessages.Progress_Running);
		try {
			if (SVNUtility.useSingleReferenceSignature(refPrev, refNext)) {
				SVNUtility.diffStatus(proxy, statusesList, refPrev,
						new SVNRevisionRange(refPrev.revision, refNext.revision), SVNDepth.INFINITY,
						ISVNConnector.Options.NONE, new SVNProgressMonitor(this, monitor, null, false));
			} else {
				SVNUtility.diffStatus(proxy, statusesList, refPrev, refNext, SVNDepth.INFINITY,
						ISVNConnector.Options.NONE, new SVNProgressMonitor(this, monitor, null, false));
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}

		for (SVNDiffStatus status : statusesList) {
			IRepositoryResource resourceToAdd = getResourceForStatus(status);
			resourceToAdd.setSelectedRevision(newer.getSelectedRevision());
			resourceToAdd.setPegRevision(newer.getPegRevision());
			resourcesToReturn.add(resourceToAdd);
			String strStatus = SVNRemoteStorage.getCompoundStatusString(status.propStatus, status.textStatus, true);
			url2status.put(resourceToAdd.getUrl(), strStatus);
			if (status.textStatus == SVNEntryStatus.Kind.DELETED) {
				resourcesToDelete.add(resourceToAdd);
			}
		}
		repositoryResources = resourcesToReturn.toArray(new IRepositoryResource[0]);
		repositoryResourcesToDelete = resourcesToDelete.toArray(new IRepositoryResource[0]);
	}

	@Override
	public IRepositoryResource[] getRepositoryResources() {
		return repositoryResources;
	}

	@Override
	public Map<String, String> getStatusesMap() {
		return url2status;
	}

}
