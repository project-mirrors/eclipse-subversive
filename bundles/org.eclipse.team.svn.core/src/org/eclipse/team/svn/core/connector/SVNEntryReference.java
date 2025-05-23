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

import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Copy source information container
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNEntryReference {
	/**
	 * The copy source path or URL.
	 */
	public final String path;

	/**
	 * The copy source peg revision. If null could be treated as {@link SVNRevision#HEAD} or ignored depending on calling context.
	 */
	public final SVNRevision pegRevision;

	/**
	 * The {@link SVNEntryRevisionReference} instance could be initialized only once because all fields are final
	 * 
	 * @param path
	 *            the source path
	 */
	public SVNEntryReference(String path) {
		this(path, null);
	}

	/**
	 * The {@link SVNEntryRevisionReference} instance could be initialized only once because all fields are final
	 * 
	 * @param path
	 *            the source path
	 * @param pegRevision
	 *            the source peg revision
	 */
	public SVNEntryReference(String path, SVNRevision pegRevision) {
		this.path = FileUtility.normalizePath(path);
		this.pegRevision = pegRevision;
	}

	@Override
	public String toString() {
		return pegRevision == null ? path : path + "@" + pegRevision.toString(); //$NON-NLS-1$
	}

}
