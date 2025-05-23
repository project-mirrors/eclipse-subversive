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
 * The change status information container
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNChangeStatus extends SVNEntryStatus {
	/**
	 * The repository URL of the entry
	 */
	public final String url;

	/**
	 * The working copy path of the entry
	 */
	public final String path;

	/**
	 * The base revision of the entry
	 */
	public final long revision;

	/**
	 * The last revision the entry was changed before base
	 */
	public final long lastChangedRevision;

	/**
	 * The last date the entry was changed before base (represented in microseconds since the epoch)
	 */
	public final long lastChangedDate;

	/**
	 * The last author of the last change before base
	 */
	public final String lastCommitAuthor;

	/**
	 * <code>true/code> if the entry is locked locally by subversion (running or aborted operation)
	 */
	public final boolean isLocked;

	/**
	 * <code>true/code> if the entry is a copy of another one
	 */
	public final boolean isCopied;

	/**
	 * <code>true/code> if the entry is switch
	 */
	public final boolean isSwitched;

	/**
	 * @since 1.6 has the item is a file external
	 */
	public final boolean isFileExternal;

	/**
	 * The depth of the node as recorded in the working copy
	 * 
	 * @since 1.9
	 */
	//public final SVNDepth depth;

	/**
	 * The status of the node, Kind.NONE, unless the node has restructuring changes.
	 * 
	 * @since 1.9
	 */
	//public final Kind repositoryNodeStatus;

	/**
	 * The entry remote content status in compare to base revision (see {@link SVNEntryStatus.Kind})
	 */
	public final Kind repositoryTextStatus;

	/**
	 * The entry remote properties status in compare to base revision (see {@link SVNEntryStatus.Kind})
	 */
	public final Kind repositoryPropStatus;

	/**
	 * @since 1.7 The lock in the working copy (<code>null</code> if not locked)
	 */
	public final SVNLock wcLock;

	/**
	 * @since 1.2 The lock in the repository (<code>null</code> if not locked)
	 */
	public final SVNLock reposLock;

	/**
	 * @since 1.3 Set to the youngest committed revision, or {@link SVNRevision#INVALID_REVISION_NUMBER} if not out of date.
	 */
	public final long reposLastCmtRevision;

	/**
	 * @since 1.3 Set to the most recent commit date, or 0 if not out of date.
	 */
	public final long reposLastCmtDate;

	/**
	 * @since 1.3 Set to the node kind of the youngest commit, or {@link Kind#NONE} if not out of date.
	 */
	public final SVNEntry.Kind reposKind;

	/**
	 * @since 1.3 Set to the user name of the youngest commit, or <code>null</code> if not out of date.
	 */
	public final String reposLastCmtAuthor;

	/**
	 * @since 1.7 Is this item in a conflicted state or not. Starting from SVN 1.7 all the conflicts-related information were deleted from
	 *        the status entry.
	 */
	public final boolean hasConflict;

	/**
	 * @since 1.6 description of the tree conflict. Is ignored by SVN 1.7 API, so we leave it as non-final field in order to upload the
	 *        information later. I can hardly understand the reason why it was removed, it sure looks unreasonable.
	 */
	public SVNConflictDescriptor[] treeConflicts;

	/**
	 * @since 1.7 The entry's change list name
	 */
	public final String changeListName;

	/**
	 * @since 1.8 A path the entry was moved from
	 */
	public final String movedFromAbsPath;

	/**
	 * @since 1.8 A path the entry was moved to
	 */
	public final String movedToAbsPath;

	/**
	 * The {@link SVNChangeStatus} instance could be initialized only once because all fields are final
	 * 
	 * @param path
	 *            the file system path of item
	 * @param url
	 *            the url of the item
	 * @param nodeKind
	 *            kind of item (directory, file or unknown)
	 * @param revision
	 *            the revision number of the base
	 * @param lastChangedRevision
	 *            the last revision this item was changed
	 * @param lastChangedDate
	 *            the last date this item was changed
	 * @param lastCommitAuthor
	 *            the author of the last change
	 * @param textStatus
	 *            the file or directory status (see {@link SVNEntryStatus.Kind})
	 * @param propStatus
	 *            the property status (see {@link SVNEntryStatus.Kind})
	 * @param repositoryTextStatus
	 *            the file or directory status of the base
	 * @param repositoryPropStatus
	 *            the property status of the base
	 * @param locked
	 *            if the item is locked (running or aborted operation)
	 * @param copied
	 *            if the item is copy
	 * @param switched
	 *            flag if the node has been switched in the path
	 * @param wcLock
	 *            the lock as stored in the working copy if any
	 * @param reposLock
	 *            the lock as stored in the repository if any
	 * @param reposLastCmtRevision
	 *            the youngest revision, if out of date
	 * @param reposLastCmtDate
	 *            the last commit date, if out of date
	 * @param reposKind
	 *            the kind of the youngest revision, if out of date
	 * @param reposLastCmtAuthor
	 *            the author of the last commit, if out of date
	 * @param isFileExternal
	 *            has the item is a file external
	 * @param hasConflict
	 *            is this item in a conflicted state or not
	 * @param treeConflicts
	 *            description of the tree conflict. Is ignored by SVN 1.7 API, so we leave it as non-final field in order to upload the
	 *            information later. I can hardly understand the reason why it was removed, it sure looks unreasonable.
	 * @param changeListName
	 *            The entry's change list name
	 */
	public SVNChangeStatus(String path, String url, SVNEntry.Kind nodeKind, long revision, long lastChangedRevision,
			long lastChangedDate, String lastCommitAuthor, Kind textStatus, Kind propStatus, Kind repositoryTextStatus,
			Kind repositoryPropStatus, boolean locked, boolean copied, boolean switched, SVNLock wcLock,
			SVNLock reposLock, long reposLastCmtRevision, long reposLastCmtDate, SVNEntry.Kind reposKind,
			String reposLastCmtAuthor, boolean isFileExternal, boolean hasConflict,
			SVNConflictDescriptor[] treeConflicts, String changeListName) {
		this(path, url, nodeKind, revision, lastChangedRevision, lastChangedDate, lastCommitAuthor, textStatus,
				propStatus, repositoryTextStatus, repositoryPropStatus, locked, copied, switched, wcLock, reposLock,
				reposLastCmtRevision, reposLastCmtDate, reposKind, reposLastCmtAuthor, isFileExternal, hasConflict,
				treeConflicts, changeListName, null, null);
	}

	/**
	 * The {@link SVNChangeStatus} instance could be initialized only once because all fields are final
	 * 
	 * @param path
	 *            the file system path of item
	 * @param url
	 *            the url of the item
	 * @param nodeKind
	 *            kind of item (directory, file or unknown)
	 * @param revision
	 *            the revision number of the base
	 * @param lastChangedRevision
	 *            the last revision this item was changed
	 * @param lastChangedDate
	 *            the last date this item was changed
	 * @param lastCommitAuthor
	 *            the author of the last change
	 * @param textStatus
	 *            the file or directory status (see {@link SVNEntryStatus.Kind})
	 * @param propStatus
	 *            the property status (see {@link SVNEntryStatus.Kind})
	 * @param repositoryTextStatus
	 *            the file or directory status of the base
	 * @param repositoryPropStatus
	 *            the property status of the base
	 * @param locked
	 *            if the item is locked (running or aborted operation)
	 * @param copied
	 *            if the item is copy
	 * @param switched
	 *            flag if the node has been switched in the path
	 * @param wcLock
	 *            the lock as stored in the working copy if any
	 * @param reposLock
	 *            the lock as stored in the repository if any
	 * @param reposLastCmtRevision
	 *            the youngest revision, if out of date
	 * @param reposLastCmtDate
	 *            the last commit date, if out of date
	 * @param reposKind
	 *            the kind of the youngest revision, if out of date
	 * @param reposLastCmtAuthor
	 *            the author of the last commit, if out of date
	 * @param isFileExternal
	 *            has the item is a file external
	 * @param hasConflict
	 *            is this item in a conflicted state or not
	 * @param treeConflicts
	 *            description of the tree conflict. Is ignored by SVN 1.7 API, so we leave it as non-final field in order to upload the
	 *            information later. I can hardly understand the reason why it was removed, it sure looks unreasonable.
	 * @param changeListName
	 *            The entry's change list name
	 * @param movedFromAbsPath
	 *            A path, the entry was moved from
	 * @param movedToAbsPath
	 *            A path, the entry was moved to
	 * @since 1.8
	 */
	public SVNChangeStatus(String path, String url, SVNEntry.Kind nodeKind, long revision, long lastChangedRevision,
			long lastChangedDate, String lastCommitAuthor, Kind textStatus, Kind propStatus, Kind repositoryTextStatus,
			Kind repositoryPropStatus, boolean locked, boolean copied, boolean switched, SVNLock wcLock,
			SVNLock reposLock, long reposLastCmtRevision, long reposLastCmtDate, SVNEntry.Kind reposKind,
			String reposLastCmtAuthor, boolean isFileExternal, boolean hasConflict,
			SVNConflictDescriptor[] treeConflicts, String changeListName, String movedFromAbsPath,
			String movedToAbsPath) {
		super(nodeKind, textStatus, propStatus);
		this.path = path;
		this.url = url;
		this.revision = revision;
		this.lastChangedRevision = lastChangedRevision;
		this.lastChangedDate = lastChangedDate;
		this.lastCommitAuthor = lastCommitAuthor;
		isLocked = locked;
		isCopied = copied;
		this.repositoryTextStatus = repositoryTextStatus;
		this.repositoryPropStatus = repositoryPropStatus;
		isSwitched = switched;
		this.wcLock = wcLock;
		this.reposLock = reposLock;
		this.reposLastCmtRevision = reposLastCmtRevision;
		this.reposLastCmtDate = reposLastCmtDate;
		this.reposKind = reposKind;
		this.reposLastCmtAuthor = reposLastCmtAuthor;
		this.isFileExternal = isFileExternal;
		this.hasConflict = hasConflict;
		setTreeConflicts(treeConflicts);
		this.changeListName = changeListName;
		this.movedFromAbsPath = movedFromAbsPath;
		this.movedToAbsPath = movedToAbsPath;
	}

	public void setTreeConflicts(SVNConflictDescriptor[] treeConflicts) {
		this.treeConflicts = treeConflicts == null || treeConflicts.length == 0 ? null : treeConflicts;
	}

	/**
	 * Returns the adjusted status of the item's contents, as compatible with JavaHL 1.8 and older verions.
	 * 
	 * @return file status property enum of the "textual" component.
	 * @since 1.9
	 */
	/*public Kind getTextStatus()
	{
	    if (nodeStatus == Kind.modified || nodeStatus == Kind.conflicted)
	        return textStatus;
	    return nodeStatus;
	}*/

	/**
	 * Returns the adjusted status of the item's contents in the repository, as compatible with JavaHL 1.8 and older verions.
	 * 
	 * @return file status property enum of the "textual" component in the repository.
	 * @since 1.9
	 */
	/*public Kind getRepositoryTextStatus()
	{
	    if (repositoryNodeStatus == Kind.modified
	        || repositoryNodeStatus == Kind.conflicted)
	        return repositoryTextStatus;
	    return repositoryNodeStatus;
	}*/

}
