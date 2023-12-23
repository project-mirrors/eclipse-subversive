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

package org.eclipse.team.svn.core.connector;

/**
 * Repository or working copy traversal depths enumeration
 */
public enum SVNDepth {
	/**
	 * Depth undetermined or ignored.
	 */
	UNKNOWN(-2),
	/**
	 * Exclude (i.e, don't descend into) directory D.
	 */
	EXCLUDE(-1),
	/**
	 * Just the named file or folder without entries.
	 */
	EMPTY(0),
	/**
	 * The folder and child files.
	 */
	FILES(1),
	/**
	 * The folder and all direct child entries.
	 */
	IMMEDIATES(2),
	/**
	 * The folder and all descendants at any depth.
	 */
	INFINITY(3);
	
	public final int id;
	
	public static final SVNDepth infinityOrEmpty(boolean recurse) {
		return (recurse ? SVNDepth.INFINITY : SVNDepth.EMPTY);
	}

	public static final SVNDepth infinityOrFiles(boolean recurse) {
		return (recurse ? SVNDepth.INFINITY : SVNDepth.FILES);
	}

	public static final SVNDepth infinityOrImmediates(boolean recurse) {
		return (recurse ? SVNDepth.INFINITY : SVNDepth.IMMEDIATES);
	}
	
	public static final SVNDepth unknownOrFiles(boolean recurse) {
		return (recurse ? SVNDepth.UNKNOWN : SVNDepth.FILES);
	}

	private SVNDepth(int id) {
		this.id = id;
	}
}