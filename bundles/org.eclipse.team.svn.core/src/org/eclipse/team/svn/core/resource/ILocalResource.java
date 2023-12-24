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

package org.eclipse.team.svn.core.resource;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;

/**
 * Checked out resource representation
 * 
 * For more details how external definitions are handled see <code>SVNRemoteStorage</code> comments
 * 
 * @author Alexander Gurov
 */
public interface ILocalResource {
	int NO_MODIFICATION = 0x00;

	/*
	 * Can be used ONLY for backward compatibility.
	 * For other cases see 'getTextStatus' and 'getPropStatus' methods
	 */
	int TEXT_MODIFIED = 0x01;

	int PROP_MODIFIED = 0x02;

	int IS_COPIED = 0x04;

	int IS_SWITCHED = 0x08;

	int IS_LOCKED = 0x10;

	int TREE_CONFLICT_UNKNOWN_NODE_KIND = 0x20;

	/**
	 * @deprecated due to mixed semantics it is replaced with IS_FORBIDDEN which is required to prevent SVN actions with resources, while
	 *             unversioned resources, produced by svn:externals, will be marked as ST_IGNORED + IS_SVN_EXTERNALS
	 */
	@Deprecated
	int IS_UNVERSIONED_EXTERNAL = 0x40;

	int IS_FORBIDDEN = 0x40;

	int IS_SVN_EXTERNALS = 0x80;

	int IS_SYMLINK = 0x100;

	IResource getResource();

	String getName();

	long getRevision();

	long getBaseRevision();

	String getTextStatus();

	String getPropStatus();

	/**
	 * Return a compound status from text and property statuses
	 * 
	 * @return
	 */
	String getStatus();

	boolean hasTreeConflict();

	SVNConflictDescriptor getTreeConflictDescriptor();

	int getChangeMask();

	boolean isCopied();

	String getAuthor();

	long getLastCommitDate();

	boolean isLocked();
}
