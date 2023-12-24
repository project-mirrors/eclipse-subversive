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
 *    Alessandro Nistico - [patch] Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.utility.DateFormatter;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Change Set collector implementation.
 * 
 * @author Alessandro Nistico
 */
public class SVNChangeSetCollector extends SyncInfoSetChangeSetCollector {
	public SVNChangeSetCollector(ISynchronizePageConfiguration configuration) {
		super(configuration);
	}

	@Override
	public void dispose() {
		SVNChangeSetCapability.isEnabled = false;
		super.dispose();
	}

	@Override
	protected void add(SyncInfo[] infos) {
		if (infos == null || infos.length == 0) {
			return;
		}
		Map<Long, SVNCheckedInChangeSet> sets = new HashMap<>();
		final Set<SVNCheckedInChangeSet> added = new HashSet<>();
		for (ChangeSet set : getSets()) {
			SVNCheckedInChangeSet svnSet = (SVNCheckedInChangeSet) set;
			sets.put(svnSet.getRevision(), svnSet);
		}
		// change set name format is: revisionNum (date) [author] ...comment...
		String svnAuthor = SVNMessages.SVNInfo_Author;
		String svnDate = SVNMessages.SVNInfo_Date;
		String svnNoAuthor = SVNMessages.SVNInfo_NoAuthor;
		String svnNoDate = SVNMessages.SVNInfo_NoDate;
		for (SyncInfo info : infos) {
			if ((info.getKind() & SyncInfo.INCOMING) == 0) {
				continue;
			}
			ILocalResource resource = ((AbstractSVNSyncInfo) info).getRemoteChangeResource();
			long revision = resource.getRevision();
			SVNCheckedInChangeSet set = sets.get(revision);
			boolean updateName = false;
			if (set == null) {
				set = new SVNCheckedInChangeSet();
				set.author = resource.getAuthor();
				set.date = new Date(resource.getLastCommitDate());
				set.revision = revision;
				if (resource instanceof IResourceChange) {
					set.comment = ((IResourceChange) resource).getComment();
				}
				updateName = true;
				sets.put(revision, set);
				added.add(set);
			} else if (set.date.getTime() == 0) {
				updateName = true;
				set.date = new Date(resource.getLastCommitDate());
			} else if (set.author == null) {
				updateName = true;
				set.author = resource.getAuthor();
			}
			if (updateName) {
				// rebuild name
				String name = String.valueOf(revision) + " " + //$NON-NLS-1$
						(resource.getLastCommitDate() == 0
								? svnNoDate
								: BaseMessages.format(svnDate, new Object[] { DateFormatter.formatDate(set.date) }))
						+ " " + //$NON-NLS-1$
						(resource.getAuthor() == null
								? svnNoAuthor
								: BaseMessages.format(svnAuthor, new Object[] { resource.getAuthor() }));
				if (set.comment != null) {
					String comment = set.comment;
					if (FileUtility.isWindows()) {
						comment = comment.replaceAll("\r\n|\r|\n", " "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					name += " " + comment; //$NON-NLS-1$
				}
				set.setName(name);
			}

			set.add(info);
		}

		// lesser UI blinking and thread safe...
		performUpdate(monitor -> {
			for (SVNCheckedInChangeSet set : added) {
				SVNChangeSetCollector.this.add(set);
			}
		}, true, new NullProgressMonitor());
	}

	@Override
	protected void initializeSets() {
	}

	public static class SVNCheckedInChangeSet extends CheckedInChangeSet {
		private String author;

		private String comment;

		private Date date;

		private long revision;

		public long getRevision() {
			return revision;
		}

		@Override
		public String getAuthor() {
			return author;
		}

		@Override
		public Date getDate() {
			return date;
		}

		@Override
		public String getComment() {
			return comment;
		}

		@Override
		public void setName(String name) {
			super.setName(name);
		}
	}

}
