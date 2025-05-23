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
 * The status information container
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNMergeStatus extends SVNEntryStatus {
	/**
	 * The repository URL of the first merged entry.
	 */
	public final String startUrl;

	/**
	 * The repository URL of the last merged entry.
	 */
	public final String endUrl;

	/**
	 * The working copy path of the entry.
	 */
	public final String path;

	/**
	 * The revision of the first merged change.
	 */
	public final long startRevision;

	/**
	 * The revision of the last merged change.
	 */
	public final long endRevision;

	/**
	 * The date of the last merged change in the merged repository resource.
	 */
	public final long date;

	/**
	 * The author of the last merged change in the merged repository resource.
	 */
	public final String author;

	/**
	 * The comment entered for the last merged change in the merged repository resource. Could be <code>null</code>.
	 */
	public final String comment;

	/**
	 * Tells if this resource is skipped during merge or not.
	 */
	public final boolean skipped;

	/**
	 * @since 1.6 is this item in a tree conflicted state
	 */
	public final boolean hasTreeConflict;

	/**
	 * @since 1.6 description of the tree conflict
	 */
	public final SVNConflictDescriptor treeConflictDescriptor;

	/**
	 * The {@link SVNMergeStatus} instance could be initialized only once because all fields are final
	 * 
	 * @param startUrl
	 *            The repository URL of the first merged entry.
	 * @param endUrl
	 *            The repository URL of the last merged entry.
	 * @param path
	 *            The working copy path of the entry.
	 * @param nodeKind
	 *            The entry kind (see {@link Kind}).
	 * @param textStatus
	 *            The entry content merge status (see {@link SVNEntryStatus.Kind}).
	 * @param propStatus
	 *            The entry properties merge status (see {@link SVNEntryStatus.Kind}).
	 * @param startRevision
	 *            The revision of the first merged change.
	 * @param endRevision
	 *            The revision of the last merged change.
	 * @param date
	 *            The date of the last merged change. Could be <code>0</code>.
	 * @param author
	 *            The author of the last merged change. Could be <code>null</code>.
	 * @param comment
	 *            The comment entered for the last merged change. Could be <code>null</code>.
	 * @param skipped
	 *            Tells if this resource is skipped during merge or not.
	 * @param hasTreeConflict
	 *            is this item in a tree conflicted state
	 * @param treeConflictDescriptor
	 *            description of the tree conflict
	 */
	public SVNMergeStatus(String startUrl, String endUrl, String path, SVNEntry.Kind nodeKind, Kind textStatus,
			Kind propStatus, long startRevision, long endRevision, long date, String author, String comment,
			boolean skipped, boolean hasTreeConflict, SVNConflictDescriptor treeConflictDescriptor) {
		super(nodeKind, textStatus, propStatus);
		this.startUrl = startUrl;
		this.endUrl = endUrl;
		this.path = path;
		this.startRevision = startRevision;
		this.endRevision = endRevision;
		this.date = date;
		this.author = author;
		this.comment = comment;
		this.skipped = skipped;
		this.hasTreeConflict = hasTreeConflict;
		this.treeConflictDescriptor = treeConflictDescriptor;
	}
}
