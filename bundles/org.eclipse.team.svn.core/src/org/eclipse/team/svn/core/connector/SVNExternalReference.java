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
 * Copy source information container
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNExternalReference extends SVNEntryRevisionReference {
	/**
	 * The reference's target path
	 */
	public final String target;

	/**
	 * The {@link SVNExternalReference} instance could be initialized only once because all fields are final
	 * 
	 * @param target
	 *            externals target path
	 * @param path
	 *            the source path
	 */
	public SVNExternalReference(String target, String path) {
		this(target, path, null, null);
	}

	/**
	 * The {@link SVNExternalReference} instance could be initialized only once because all fields are final
	 * 
	 * @param target
	 *            externals target path
	 * @param path
	 *            the source path
	 * @param pegRevision
	 *            the source peg revision
	 * @param revision
	 *            the source revision.
	 */
	public SVNExternalReference(String target, String path, SVNRevision pegRevision, SVNRevision revision) {
		super(path, pegRevision, revision);
		this.target = target;
	}

	/**
	 * The {@link SVNExternalReference} instance could be initialized only once because all fields are final
	 * 
	 * @param target
	 *            externals target path
	 * @param reference
	 *            the entry reference
	 * @param revision
	 *            the source revision.
	 */
	public SVNExternalReference(String target, SVNEntryReference reference, SVNRevision revision) {
		super(reference, revision);
		this.target = target;
	}

}
