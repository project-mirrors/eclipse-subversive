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

import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * IResource-based resource history model object
 * 
 * @author Alexander Gurov
 */
public class SVNResourceHistory extends FileHistory {
	protected static IFileRevision[] EMPTY = {};

	protected ILocalResource local;

	protected IRepositoryResource remote;

	protected SVNLogEntry[] msgs;

	protected boolean full;

	public SVNResourceHistory(ILocalResource local, IRepositoryResource remote, SVNLogEntry[] msgs, boolean full) {
		this.local = local;
		this.remote = remote;
		this.msgs = msgs;
		this.full = full;
	}

	@Override
	public IFileRevision[] getContributors(IFileRevision revision) {
		int idx = this.getRevisionIdx(revision.getContentIdentifier());
		if (idx != -1 && idx < msgs.length - 1) {
			return new IFileRevision[] { new SVNRemoteResourceRevision(remote, msgs[idx + 1]) };
		}
		return SVNResourceHistory.EMPTY;
	}

	@Override
	public IFileRevision getFileRevision(String id) {
		if (SVNRevision.WORKING.toString().equals(id)) {
			return new SVNLocalResourceRevision(local, SVNRevision.WORKING);
		} else if (SVNRevision.BASE.toString().equals(id)) {
			return new SVNLocalResourceRevision(local, SVNRevision.BASE);
		} else if (SVNRevision.HEAD.toString().equals(id)) {
			return full ? new SVNRemoteResourceRevision(remote, msgs[0]) : null;
		} else if (SVNRevision.START.toString().equals(id)) {
			return full ? new SVNRemoteResourceRevision(remote, msgs[msgs.length - 1]) : null;
		} else if (SVNRevision.PREVIOUS.toString().equals(id)) {
			int idx = this.getRevisionIdx(local.getRevision());
			return idx != -1 && idx < msgs.length - 1 ? new SVNRemoteResourceRevision(remote, msgs[idx + 1]) : null;
		} else if (SVNRevision.COMMITTED.toString().equals(id)) {
			int idx = this.getRevisionIdx(local.getRevision());
			return idx != -1 ? new SVNRemoteResourceRevision(remote, msgs[idx]) : null;
		}

		int idx = this.getRevisionIdx(id);
		if (idx != -1) {
			return new SVNRemoteResourceRevision(remote, msgs[idx]);
		}
		return null;
	}

	@Override
	public IFileRevision[] getFileRevisions() {
		IFileRevision[] revs = new IFileRevision[msgs.length];
		for (int i = 0; i < msgs.length; i++) {
			revs[i] = new SVNRemoteResourceRevision(remote, msgs[i]);
		}
		return revs;
	}

	@Override
	public IFileRevision[] getTargets(IFileRevision revision) {
		int idx = this.getRevisionIdx(revision.getContentIdentifier());
		if (idx > 0) {
			return new IFileRevision[] { new SVNRemoteResourceRevision(remote, msgs[idx - 1]) };
		}
		return SVNResourceHistory.EMPTY;
	}

	protected int getRevisionIdx(String id) {
		long revision;
		try {
			revision = Long.parseLong(id);
		} catch (Exception ex) {
			return -1;
		}
		return this.getRevisionIdx(revision);
	}

	protected int getRevisionIdx(long revision) {
		for (int i = 0; i < msgs.length; i++) {
			if (msgs[i].revision == revision) {
				return i;
			}
		}
		return -1;
	}

}
