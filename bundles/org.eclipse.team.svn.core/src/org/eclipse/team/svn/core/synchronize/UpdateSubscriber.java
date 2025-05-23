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
 *    Andrey Loskutov - Performance improvements for UpdateSubscriber (bug 506760)
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.synchronize;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.local.IRemoteStatusOperation;
import org.eclipse.team.svn.core.operation.local.RemoteStatusOperation;
import org.eclipse.team.svn.core.operation.remote.GetLogMessagesOperation;
import org.eclipse.team.svn.core.resource.IChangeStateProvider;
import org.eclipse.team.svn.core.resource.ICommentProvider;
import org.eclipse.team.svn.core.resource.IFileChange;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Synchronize view data provider
 * 
 * @author Alexander Gurov
 */
public class UpdateSubscriber extends AbstractSVNSubscriber {
	private static UpdateSubscriber instance = null;

	protected Map<SVNRevision, String> comments;

	public static synchronized UpdateSubscriber instance() {
		if (UpdateSubscriber.instance == null) {
			UpdateSubscriber.instance = new UpdateSubscriber();
		}
		return UpdateSubscriber.instance;
	}

	@Override
	public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		comments.clear();
		super.refresh(resources, depth, monitor);
	}

	@Override
	protected IRemoteStatusOperation addStatusOperation(CompositeOperation op, IResource[] resources, int depth) {
		RemoteStatusOperation rStatus = new RemoteStatusOperation(resources);
		op.add(rStatus);
		return rStatus;
	}

	@Override
	protected SyncInfo getSVNSyncInfo(ILocalResource localStatus, IResourceChange remoteStatus) {
		return new UpdateSyncInfo(localStatus, remoteStatus, getResourceComparator());
	}

	@Override
	protected IResourceChange handleResourceChange(IRemoteStatusOperation rStatusOp, final SVNEntryStatus status) {
		final SVNChangeStatus current = (SVNChangeStatus) status;
		if (current.textStatus == SVNEntryStatus.Kind.EXTERNAL) {
			return null;
		}
		final IResource[] scope = rStatusOp.getScope();
		IChangeStateProvider provider = new IChangeStateProvider() {
			private SVNEntry.Kind kind;

			private SVNRevision rev;

			private IResource exact;

			@Override
			public long getChangeDate() {
				return current.reposLastCmtRevision == SVNRevision.INVALID_REVISION_NUMBER
						? current.lastChangedDate
						: current.reposLastCmtDate;
			}

			@Override
			public String getChangeAuthor() {
				return current.reposLastCmtRevision == SVNRevision.INVALID_REVISION_NUMBER
						? current.lastCommitAuthor
						: current.reposLastCmtAuthor;
			}

			@Override
			public SVNRevision.Number getChangeRevision() {
				if (rev == null) {
					long changeRev = current.reposLastCmtRevision == SVNRevision.INVALID_REVISION_NUMBER
							? current.lastChangedRevision
							: current.reposLastCmtRevision;
					rev = changeRev == SVNRevision.INVALID_REVISION_NUMBER
							? SVNRevision.INVALID_REVISION
							: (SVNRevision.Number) SVNRevision.fromNumber(changeRev);
				}
				return rev == SVNRevision.INVALID_REVISION ? null : (SVNRevision.Number) rev;
			}

			@Override
			public Kind getTextChangeType() {
				return current.repositoryTextStatus;
			}

			@Override
			public SVNEntryStatus.Kind getPropertiesChangeType() {
				return current.repositoryPropStatus;
			}

			@Override
			public SVNEntry.Kind getNodeKind() {
				if (kind == null) {
					kind = SVNUtility.getNodeKind(current.path, current.nodeKind, true);
					kind = kind == SVNEntry.Kind.NONE
							? SVNUtility.getNodeKind(current.path, current.reposKind, true)
							: kind;
				}
				return kind;
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
				return current.isCopied;
			}

			@Override
			public boolean isSwitched() {
				return current.isSwitched;
			}

			@Override
			public IResource getExact(IResource[] set) {
				if (exact == null) {
					exact = FileUtility.selectOneOf(scope, set);
				}
				return exact;
			}

			@Override
			public SVNConflictDescriptor getTreeConflictDescriptor() {
				return current.treeConflicts == null ? null : current.treeConflicts[0];
			}
		};
		if (provider.getNodeKind() == SVNEntry.Kind.NONE) {
			return null;
		}
		IResourceChange resourceChange = SVNRemoteStorage.instance().asResourceChange(provider, true);
		if (resourceChange == null || resourceChange
				.getRevision() == SVNRevision.INVALID_REVISION_NUMBER/* && !resourceChange.hasTreeConflict()*/) {
			return null;
		}
		IResourceChange checkForReplacement = null;
		try {
			checkForReplacement = SVNRemoteStorage.instance()
					.resourceChangeFromBytes(statusCache.getBytes(resourceChange.getResource()));
		} catch (TeamException e) {
			LoggedOperation.reportError(this.getClass().getName(), e);
		}
		if (checkForReplacement != null) {
			if (IStateFilter.SF_ADDED.accept(checkForReplacement)) {
				if (IStateFilter.SF_DELETED.accept(resourceChange)) {
					checkForReplacement.treatAsReplacement();
				}
				return checkForReplacement;
			}
			if (IStateFilter.SF_DELETED.accept(checkForReplacement) && IStateFilter.SF_ADDED.accept(resourceChange)) {
				resourceChange.treatAsReplacement();
			}
		}

		rStatusOp.setPegRevision(resourceChange);
		IRepositoryResource originator = SVNRemoteStorage.instance().asRepositoryResource(resourceChange.getResource());
		if (originator != null) {
			// for case sensitive name changes, nulls allowed for externals roots
			String url = SVNUtility.decodeURL(current.url);
			IRepositoryResource tOriginator = resourceChange instanceof IFileChange
					? (IRepositoryResource) originator.asRepositoryFile(url, true)
					: (IRepositoryResource) originator.asRepositoryContainer(url, true);
			if (tOriginator != null) {
				originator = tOriginator;
			}
			originator.setSelectedRevision(SVNRevision.fromNumber(resourceChange.getRevision()));
			originator.setPegRevision(resourceChange.getPegRevision());
			resourceChange.setOriginator(originator);
		}

		resourceChange.setCommentProvider(new ICommentProvider() {
			@Override
			public String getComment(IResource resource, SVNRevision rev, SVNRevision peg) {
				//Null is also valid value if no comment was specified for revision. So, check for key presence.
				if (!comments.containsKey(rev)) {
					cacheComments(resource, rev, peg);
				}
				return comments.get(rev);
			}

			public void cacheComments(IResource resource, SVNRevision rev, SVNRevision peg) {
				if (rev == SVNRevision.INVALID_REVISION || peg != null && peg == SVNRevision.INVALID_REVISION) {
					return;
				}
				// we optimized comment fetching by speed regarding to fact that only number revision used by this implementation of ICommentProvider
				// and select messages for project root (helpful in case of multiple-project layouts)...
				IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(resource).getRoot();
				remote.setSelectedRevision(rev);
				remote.setPegRevision(peg);
				GetLogMessagesOperation op = new GetLogMessagesOperation(remote);
				op.setLimit(20);
				op.setDiscoverPaths(false);
				ProgressMonitorUtility.doTaskExternalDefault(op, new NullProgressMonitor());
				if (op.getExecutionState() == IActionOperation.OK) {
					for (SVNLogEntry entry : op.getMessages()) {
						comments.put(SVNRevision.fromNumber(entry.revision), entry.message);
					}
				}
			}
		});
		return resourceChange;
	}

	@Override
	protected boolean isIncoming(SVNEntryStatus status) {
		SVNChangeStatus st = (SVNChangeStatus) status;
		return st.repositoryPropStatus == SVNEntryStatus.Kind.MODIFIED
				|| st.repositoryTextStatus != SVNEntryStatus.Kind.NONE || st.hasConflict;
	}

	/*
	 * Override method from super class in order to not contact the server
	 * if we're interested only in outgoing changes, see 'autoRefresh' param from SubscriberResourceMappingContext
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.Subscriber#getState(org.eclipse.core.resources.mapping.ResourceMapping, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public int getState(ResourceMapping mapping, int stateMask, IProgressMonitor monitor) throws CoreException {
		if ((stateMask & IThreeWayDiff.INCOMING) == 0) {
			// If we're only interested in outgoing changes, used the cached modified state
			ResourceTraversal[] traversals = mapping.getTraversals(new SubscriberResourceMappingContext(this, false),
					monitor);
			final int[] direction = { 0 };
			final int[] kind = { 0 };
			accept(traversals, diff -> {
				if (diff instanceof IThreeWayDiff) {
					IThreeWayDiff twd = (IThreeWayDiff) diff;
					direction[0] |= twd.getDirection();
				}
				// If the traversals contain a combination of kinds, return a CHANGE
				int diffKind = diff.getKind();
				if (kind[0] == 0) {
					kind[0] = diffKind;
				}
				if (kind[0] != diffKind) {
					kind[0] = IDiff.CHANGE;
				}
				// Only need to visit the childen of a change
				return diffKind == IDiff.CHANGE;
			});
			return (direction[0] | kind[0]) & stateMask;
		} else {
			return super.getState(mapping, stateMask, monitor);
		}
	}

	private UpdateSubscriber() {
		super(true, SVNMessages.UpdateSubscriber_Name);
		comments = new HashMap<>();
	}

}
