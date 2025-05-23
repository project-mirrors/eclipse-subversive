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

package org.eclipse.team.svn.core.connector;

/**
 * Repository or working copy entry information container
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNEntryInfo {
	/**
	 * Enumeration of operation types which could be scheduled for the working copy entries
	 */
	public enum ScheduledOperation {
		/**
		 * No operation scheduled
		 */
		NORMAL(0),
		/**
		 * Will be added to repository on commit
		 */
		ADD(1),
		/**
		 * Will be deleted from repository on commit
		 */
		DELETE(2),
		/**
		 * Will be replaced in repository on commit
		 */
		REPLACE(3);

		public final int id;

		ScheduledOperation(int id) {
			this.id = id;
		}
	}

	/**
	 * The entry local path.
	 */
	public final String path;

	/**
	 * @since 1.7 The working copy root
	 */
	public final String wcRoot;

	/**
	 * The entry URL.
	 */
	public final String url;

	/**
	 * The entry revision.
	 */
	public final long revision;

	/**
	 * The entry node kind.
	 */
	public final SVNEntry.Kind kind;

	/**
	 * The repository root URL.
	 */
	public final String reposRootUrl;

	/**
	 * The repository UUID.
	 */
	public final String reposUUID;

	/**
	 * The last change revision.
	 */
	public final long lastChangedRevision;

	/**
	 * The last change date in in microseconds.
	 */
	public final long lastChangedDate;

	/**
	 * The last change date in in nanoseconds.
	 * 
	 * @since 1.9
	 */
	//public final long lastChangedDateNano;

	/**
	 * The last change author.
	 */
	public final String lastChangedAuthor;

	/**
	 * The lock information or <code>null</code>.
	 */
	public final SVNLock lock;

	/**
	 * True if rest of fields are set (for path-based queries).
	 */
	public final boolean hasWcInfo;

	/**
	 * The operation scheduled at next commit (see {@link ScheduledOperation})
	 */
	public final ScheduledOperation schedule;

	/**
	 * The copied from URL.
	 */
	public final String copyFromUrl;

	/**
	 * The copied from revision.
	 */
	public final long copyFromRevision;

	/**
	 * The content last change time in nanoseconds.
	 */
	public final long textTime;

	/**
	 * The properties last change time in nanoseconds.
	 */
	public final long propTime;

	/**
	 * @since 1.7 The entry checksum.
	 */
	public final SVNChecksum checksum;

	/**
	 * @since 1.7 The entry's change list name
	 */
	public final String changeListName;

	/**
	 * @since 1.7 The size of the file after being translated into its local representation, or <code>-1</code> if unknown. Not applicable
	 *        for directories.
	 */
	public final long wcSize;

	/**
	 * @since 1.7 The size of the file in the repository (untranslated, e.g. without adjustment of line endings and keyword expansion). Only
	 *        applicable for file -- not directory -- URLs. For working copy paths, size will be <code>-1</code>.
	 */
	public final long reposSize;

	/**
	 * The depth of the directory or <code>null</code> if the item is a file.
	 * 
	 * @since 1.6
	 */
	public final SVNDepth depth;

	/**
	 * Info on any tree conflict of which this node is a victim
	 * 
	 * @since 1.6
	 */
	public final SVNConflictDescriptor[] treeConflicts;

	public SVNEntryInfo(String path, String wcRoot, String url, long rev, SVNEntry.Kind kind, String reposRootUrl,
			String reposUUID, long lastChangedRev, long lastChangedDate, String lastChangedAuthor, SVNLock lock,
			boolean hasWcInfo, ScheduledOperation schedule, String copyFromUrl, long copyFromRev, long textTime,
			long propTime, SVNChecksum checksum, String changeListName, long wcSize, long reposSize, SVNDepth depth,
			SVNConflictDescriptor[] treeConflicts) {
		this.path = path;
		this.wcRoot = wcRoot;
		this.url = url;
		revision = rev;
		this.kind = kind;
		this.reposRootUrl = reposRootUrl;
		this.reposUUID = reposUUID;
		lastChangedRevision = lastChangedRev;
		this.lastChangedDate = lastChangedDate;
		this.lastChangedAuthor = lastChangedAuthor;
		this.lock = lock;
		this.hasWcInfo = hasWcInfo;
		this.schedule = schedule;
		this.copyFromUrl = copyFromUrl;
		copyFromRevision = copyFromRev;
		this.textTime = textTime;
		this.propTime = propTime;
		this.checksum = checksum;
		this.changeListName = changeListName;
		this.wcSize = wcSize;
		this.reposSize = reposSize;
		this.depth = depth;
		this.treeConflicts = treeConflicts != null ? new SVNConflictDescriptor[treeConflicts.length] : null;
		if (treeConflicts != null) {
			System.arraycopy(treeConflicts, 0, this.treeConflicts, 0, treeConflicts.length);
		}
	}

}
