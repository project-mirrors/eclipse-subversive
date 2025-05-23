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
 * The conflict resolution method description
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNConflictResolution {
	public enum Choice {
		/**
		 * Nothing done to resolve the conflict; conflict remains.
		 */
		POSTPONE(0),
		/**
		 * Resolve the conflict by choosing the base version.
		 */
		CHOOSE_BASE(1),
		/**
		 * Resolve the conflict by choosing the repository version.
		 */
		CHOOSE_REMOTE_FULL(2),
		/**
		 * Resolve the conflict by choosing local version.
		 */
		CHOOSE_LOCAL_FULL(3),
		/**
		 * Resolve the conflict by choosing the incoming (repository) version of the object (for conflicted hunks only).
		 */
		CHOOSE_REMOTE(4),
		/**
		 * Resolve the conflict by choosing own (local) version of the object (for conflicted hunks only).
		 */
		CHOOSE_LOCAL(5),
		/**
		 * Resolve the conflict by choosing the merged object (potentially manually edited).
		 */
		CHOOSE_MERGED(6);

		public final int id;

		Choice(int id) {
			this.id = id;
		}
	}

	/**
	 * The acceptable choice.
	 */
	public final Choice choice;

	/**
	 * The path to the result of a merge. Could be <code>null</code>.
	 */
	public final String mergedPath;

	/**
	 * The {@link SVNConflictResolution} instance could be initialized only once because all fields are final
	 * 
	 * @param choice
	 *            the acceptable choice
	 * @param mergedPath
	 *            the path to the result of merge
	 */
	public SVNConflictResolution(Choice choice, String mergedPath) {
		this.choice = choice;
		this.mergedPath = mergedPath;
	}

}
