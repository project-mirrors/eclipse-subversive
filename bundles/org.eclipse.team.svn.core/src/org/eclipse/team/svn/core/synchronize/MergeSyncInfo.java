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
 *    Rene Link - [patch] NPE in Interactive Merge UI
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.synchronize;

import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceChange;

/**
 * Merge sync info: ignores outgoing changes
 * 
 * It's used to present sync info for skipped by merge resources, e.g. skipped resources can be considered as tree conflicts (which appeared
 * in SVN 1.6) but in previous SVN versions. As tree conflicts were not correctly treated in previous to SVN 1.6 version, we should somehow
 * show them to user.
 * 
 * @author Alexander Gurov
 */
public class MergeSyncInfo extends AbstractSVNSyncInfo implements IMergeSyncInfo {

	protected IResourceChange baseStatus;

	public MergeSyncInfo(ILocalResource local, IResourceChange base, IResourceChange remote,
			IResourceVariantComparator comparator) {
		super(local,
				base == null
						? AbstractSVNSyncInfo.makeBaseVariant(local)
						: AbstractSVNSyncInfo.makeRemoteVariant(local, base),
				AbstractSVNSyncInfo.makeRemoteVariant(local, remote), comparator, remote);
		baseStatus = base;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.SyncInfo#calculateKind()
	 */
	@Override
	protected int calculateKind() throws TeamException {
		String localKind = local == null ? IStateFilter.ST_NOTEXISTS : local.getStatus();
		int localMask = local == null ? 0 : local.getChangeMask();
		String remoteKind = remoteStatus == null
				? this.isNonVersioned(localKind, localMask) ? IStateFilter.ST_NOTEXISTS : IStateFilter.ST_NORMAL
				: remoteStatus.getStatus();
		int remoteMask = remoteStatus == null ? 0 : remoteStatus.getChangeMask();

		if (isLinked(localKind, localMask)) {
			// Corresponding resource at remote site can produce a change
			if (this.isAdded(remoteKind, remoteMask)) {
				this.localKind = SyncInfo.OUTGOING | SyncInfo.ADDITION;
				this.remoteKind = SyncInfo.INCOMING | SyncInfo.ADDITION;
				return SyncInfo.CONFLICTING | SyncInfo.ADDITION;
			}
			if (isModified(remoteKind, remoteMask) || this.isReplaced(remoteKind, remoteMask)) {
				this.localKind = SyncInfo.OUTGOING | SyncInfo.ADDITION;
				this.remoteKind = SyncInfo.INCOMING | SyncInfo.CHANGE;
				return SyncInfo.CONFLICTING | SyncInfo.CHANGE;
			}
			if (this.isDeleted(remoteKind, remoteMask)) {
				this.localKind = SyncInfo.OUTGOING | SyncInfo.ADDITION;
				this.remoteKind = SyncInfo.INCOMING | SyncInfo.DELETION;
				return SyncInfo.CONFLICTING | SyncInfo.DELETION;
			}
			return SyncInfo.IN_SYNC;
		}

		if (isTreeConflicted(localKind, localMask) || isTreeConflicted(remoteKind, remoteMask)) {
			this.localKind = SyncInfo.OUTGOING | SyncInfo.CHANGE;
			this.remoteKind = SyncInfo.INCOMING | SyncInfo.CHANGE;
			return SyncInfo.CONFLICTING | SyncInfo.CHANGE;
		}

		if (this.isAdded(remoteKind, remoteMask)) {
			this.remoteKind = SyncInfo.INCOMING | SyncInfo.ADDITION;
			if (this.isNotExists(localKind, localMask) || this.isDeleted(localKind, localMask)) {
				return SyncInfo.INCOMING | SyncInfo.ADDITION;
			}
			this.localKind = SyncInfo.OUTGOING | SyncInfo.ADDITION;
			return SyncInfo.CONFLICTING | SyncInfo.ADDITION;
		}
		if (isConflicted(remoteKind, remoteMask)) {
			this.localKind = SyncInfo.OUTGOING | SyncInfo.CHANGE;
			this.remoteKind = SyncInfo.INCOMING | SyncInfo.CHANGE;
			return SyncInfo.CONFLICTING | SyncInfo.CHANGE;
		}
		if (isModified(remoteKind, remoteMask)) {
			this.remoteKind = SyncInfo.INCOMING | SyncInfo.CHANGE;
			if (this.isNotExists(localKind, localMask)) {
				this.remoteKind = SyncInfo.INCOMING | SyncInfo.ADDITION;
				return SyncInfo.INCOMING | SyncInfo.ADDITION;
			}
			if (this.isDeleted(localKind, localMask)) {
				this.localKind = SyncInfo.OUTGOING | SyncInfo.DELETION;
				return SyncInfo.CONFLICTING | SyncInfo.DELETION;
			}
			return SyncInfo.INCOMING | SyncInfo.CHANGE;
		}
		if (this.isDeleted(remoteKind, remoteMask)) {
			this.remoteKind = SyncInfo.INCOMING | SyncInfo.DELETION;
			if (this.isNotExists(localKind, localMask) || this.isDeleted(localKind, localMask)) {
				this.remoteKind = SyncInfo.IN_SYNC;
				return SyncInfo.IN_SYNC;
			}
			if (isNotModified(localKind, localMask)) {
				return SyncInfo.INCOMING | SyncInfo.DELETION;
			}
			this.localKind = SyncInfo.OUTGOING | SyncInfo.CHANGE;
			return SyncInfo.CONFLICTING | SyncInfo.DELETION;
		}

		// if (this.isNotModified(remoteKind)) {...
		return SyncInfo.IN_SYNC;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo#getBaseChangeResource()
	 */
	@Override
	public ILocalResource getBaseChangeResource() {
		return baseStatus == null ? local : baseStatus;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.core.synchronize.IMergeSyncInfo#getBaseResource()
	 */
	@Override
	public IResourceChange getBaseResource() {
		return baseStatus;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.core.synchronize.IMergeSyncInfo#getRemoteResource()
	 */
	@Override
	public IResourceChange getRemoteResource() {
		return remoteStatus;
	}

}
