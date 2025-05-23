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

package org.eclipse.team.svn.core.synchronize;

import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind;
import org.eclipse.team.svn.core.connector.SVNMergeStatus;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.local.AbstractMergeSet;
import org.eclipse.team.svn.core.operation.local.IRemoteStatusOperation;
import org.eclipse.team.svn.core.operation.local.MergeSet1URL;
import org.eclipse.team.svn.core.operation.local.MergeSet2URL;
import org.eclipse.team.svn.core.operation.local.MergeSetReintegrate;
import org.eclipse.team.svn.core.operation.local.MergeStatusOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.resource.IChangeStateProvider;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Merge view data provider
 * 
 * @author Alexander Gurov
 */
public class MergeSubscriber extends AbstractSVNSubscriber {
	private static MergeSubscriber instance = null;

	protected MergeScopeHelper mergeScopeHelper;

	protected MergeStatusOperation mergeStatusOp;

	protected IRemoteStatusCache baseStatusCache;

	public static synchronized MergeSubscriber instance() {
		if (MergeSubscriber.instance == null) {
			MergeSubscriber.instance = new MergeSubscriber();
		}
		return MergeSubscriber.instance;
	}

	public MergeScopeHelper getMergeScopeHelper() {
		return mergeScopeHelper;
	}

	public void setMergeScopeHelper(MergeScopeHelper scope) {
		mergeScopeHelper = scope;
	}

	@Override
	protected SyncInfo getSVNSyncInfo(ILocalResource localStatus, IResourceChange remoteStatus) throws TeamException {
		IResourceChange baseStatus = SVNRemoteStorage.instance()
				.resourceChangeFromBytes(baseStatusCache.getBytes(localStatus.getResource()));
		AbstractSVNSyncInfo syncInfo;
		if (remoteStatus == null && baseStatus == null) {
			syncInfo = new MergeSyncInfo(localStatus, null, null, getResourceComparator());
		} else {
			SVNMergeStatus mergeStatus = getStatusFor(localStatus.getResource());
			if (mergeStatus != null) {
				if (mergeStatus.skipped) {
					syncInfo = new MergeSyncInfo(localStatus, baseStatus, remoteStatus, getResourceComparator());
				} else {
					syncInfo = new UpdateSyncInfoForMerge(localStatus, baseStatus, remoteStatus,
							getResourceComparator());
				}
			} else {
				//should never happen
				syncInfo = new UpdateSyncInfo(localStatus, null, getResourceComparator());
			}
		}
		return syncInfo;
	}

	protected SVNMergeStatus getStatusFor(IResource resource) {
		SVNMergeStatus[] statuses = mergeScopeHelper.getMergeSet().getStatuses();
		IPath target = FileUtility.getResourcePath(resource);
		for (SVNMergeStatus status : statuses) {
			if (target.equals(new Path(status.path))) {
				return status;
			}
		}
		return null;
	}

	@Override
	protected IRemoteStatusOperation addStatusOperation(CompositeOperation op, IResource[] resources, int depth) {
		MergeStatusOperation mergeOp = mergeStatusOp = mergeScopeHelper == null
				? null
				: new MergeStatusOperation(mergeScopeHelper.getMergeSet(), resources);
		if (mergeOp == null) {
			return null;
		}
		SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
		op.add(saveOp);
		op.add(mergeOp);
		op.add(new RestoreProjectMetaOperation(saveOp));
		op.add(new RefreshResourcesOperation(resources, depth, RefreshResourcesOperation.REFRESH_CHANGES));
		return mergeOp;
	}

	@Override
	protected HashSet<IResource> clearRemoteStatusesImpl(IResource[] resources) throws TeamException {
		this.clearRemoteStatusesImpl(baseStatusCache, resources);
		return super.clearRemoteStatusesImpl(resources);
	}

	@Override
	public void refresh(final IResource[] resources, final int depth, IProgressMonitor monitor) throws TeamException {
		if (mergeScopeHelper != null) {
			baseStatusCache.clearAll();
			mergeScopeHelper.getMergeSet().setStatuses(new SVNMergeStatus[0]);
		}
		super.refresh(resources, depth, monitor);
	}

	@Override
	protected IResourceChange handleResourceChange(IRemoteStatusOperation rStatusOp, SVNEntryStatus status) {
		final SVNMergeStatus current = (SVNMergeStatus) status;
		IChangeStateProvider endProvider = new IChangeStateProvider() {
			@Override
			public long getChangeDate() {
				return current.date;
			}

			@Override
			public String getChangeAuthor() {
				return current.author;
			}

			@Override
			public SVNRevision.Number getChangeRevision() {
				return current.endRevision == SVNRevision.INVALID_REVISION_NUMBER
						? null
						: SVNRevision.fromNumber(current.endRevision);
			}

			@Override
			public Kind getTextChangeType() {
				return current.textStatus;
			}

			@Override
			public SVNEntryStatus.Kind getPropertiesChangeType() {
				return current.propStatus;
			}

			@Override
			public SVNEntry.Kind getNodeKind() {
				SVNEntry.Kind kind = SVNUtility.getNodeKind(current.path, current.nodeKind, true);
				// if not exists on repository try to check it with WC kind...
				return kind == SVNEntry.Kind.NONE && !current.hasTreeConflict
						? SVNUtility.getNodeKind(current.path, current.nodeKind, false)
						: kind;
			}

			@Override
			public String getLocalPath() {
				return current.path;
			}

			@Override
			public String getComment() {
				return current.comment;
			}

			@Override
			public boolean isCopied() {
				return false;
			}

			@Override
			public boolean isSwitched() {
				return false;
			}

			@Override
			public IResource getExact(IResource[] set) {
				return FileUtility.selectOneOf(mergeScopeHelper.getRoots(), set);
			}

			@Override
			public SVNConflictDescriptor getTreeConflictDescriptor() {
				return current.treeConflictDescriptor;
			}
		};
		if (endProvider.getNodeKind() == SVNEntry.Kind.NONE && !current.hasTreeConflict) {
			return null;
		}
		IResourceChange endResourceChange = SVNRemoteStorage.instance().asResourceChange(endProvider, false);
		if (endResourceChange == null) {
			return null;
		}
		if (endResourceChange.getRevision() != SVNRevision.INVALID_REVISION_NUMBER) {
			IRepositoryResource originator = getEndOriginator();
			String decodedUrl = SVNUtility.decodeURL(current.endUrl);
			originator = endProvider.getNodeKind() == SVNEntry.Kind.DIR
					? (IRepositoryResource) originator.asRepositoryContainer(decodedUrl, false)
					: originator.asRepositoryFile(decodedUrl, false);
			originator.setSelectedRevision(SVNRevision.fromNumber(
					current.textStatus == SVNEntryStatus.Kind.DELETED ? current.endRevision - 1 : current.endRevision));
			endResourceChange.setOriginator(originator);
		}

		IChangeStateProvider startProvider = new IChangeStateProvider() {
			@Override
			public long getChangeDate() {
				return current.date;
			}

			@Override
			public String getChangeAuthor() {
				return null;
			}

			@Override
			public SVNRevision.Number getChangeRevision() {
				return current.startRevision == SVNRevision.INVALID_REVISION_NUMBER
						? null
						: SVNRevision.fromNumber(current.startRevision);
			}

			@Override
			public Kind getTextChangeType() {
				return current.startRevision == SVNRevision.INVALID_REVISION_NUMBER
						? SVNEntryStatus.Kind.NONE
						: SVNEntryStatus.Kind.NORMAL;
			}

			@Override
			public SVNEntryStatus.Kind getPropertiesChangeType() {
				return SVNEntryStatus.Kind.NONE;
			}

			@Override
			public SVNEntry.Kind getNodeKind() {
				SVNEntry.Kind kind = SVNUtility.getNodeKind(current.path, current.nodeKind, true);
				// if not exists on repository try to check it with WC kind...
				return kind == SVNEntry.Kind.NONE && !current.hasTreeConflict
						? SVNUtility.getNodeKind(current.path, current.nodeKind, false)
						: kind;
			}

			@Override
			public String getLocalPath() {
				return current.path;
			}

			@Override
			public String getComment() {
				return null;
			}

			@Override
			public boolean isCopied() {
				return false;
			}

			@Override
			public boolean isSwitched() {
				return false;
			}

			@Override
			public IResource getExact(IResource[] set) {
				return FileUtility.selectOneOf(mergeScopeHelper.getRoots(), set);
			}

			@Override
			public SVNConflictDescriptor getTreeConflictDescriptor() {
				return current.treeConflictDescriptor;
			}
		};
		IResourceChange startResourceChange = SVNRemoteStorage.instance().asResourceChange(startProvider, false);
		if (startResourceChange.getRevision() != SVNRevision.INVALID_REVISION_NUMBER) {
			String decodedUrl = SVNUtility.decodeURL(current.startUrl);
			IRepositoryResource originator = getStartOriginator();
			originator = startProvider.getNodeKind() == SVNEntry.Kind.DIR
					? (IRepositoryResource) originator.asRepositoryContainer(decodedUrl, false)
					: originator.asRepositoryFile(decodedUrl, false);
			originator.setSelectedRevision(SVNRevision.fromNumber(current.startRevision));
			startResourceChange.setOriginator(originator);
		}
		try {
			baseStatusCache.setBytes(startResourceChange.getResource(),
					SVNRemoteStorage.instance().resourceChangeAsBytes(startResourceChange));
		} catch (TeamException e) {
			LoggedOperation.reportError(this.getClass().getName(), e);
		}

		return endResourceChange;
	}

	protected IRepositoryResource getEndOriginator() {
		AbstractMergeSet mergeSet = mergeScopeHelper.getMergeSet();
		if (mergeSet instanceof MergeSet1URL) {
			return ((MergeSet1URL) mergeSet).from[0];
		} else if (mergeSet instanceof MergeSet2URL) {
			return ((MergeSet2URL) mergeSet).fromEnd[0];
		} else {
			return ((MergeSetReintegrate) mergeSet).from[0];
		}
	}

	protected IRepositoryResource getStartOriginator() {
		AbstractMergeSet mergeSet = mergeScopeHelper.getMergeSet();
		if (mergeSet instanceof MergeSet1URL) {
			return ((MergeSet1URL) mergeSet).from[0];
		} else if (mergeSet instanceof MergeSet2URL) {
			return ((MergeSet2URL) mergeSet).fromStart[0];
		} else {
			return ((MergeSetReintegrate) mergeSet).from[0];
		}
	}

	@Override
	protected boolean isIncoming(SVNEntryStatus status) {
		return true;
	}

	private MergeSubscriber() {
		super(false, SVNMessages.MergeSubscriber_Name);
		baseStatusCache = new RemoteStatusCache();
	}

}
