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
 * Property data call-back interface
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public interface ISVNPropertyCallback {
	public static class Pair {
		public final String path;

		public final SVNProperty[] data;

		public Pair(String path, SVNProperty[] data) {
			this.path = path;
			this.data = data;
		}
	}

	/**
	 * This method will be called by the connector library for each found entry
	 * 
	 * @param personalProps
	 *            the pair of a path and property set belonging to the entry itself
	 * @param inheritedProps
	 *            the list of properties that were inherited from the parent nodes
	 */
	void next(Pair personalProps, Pair[] inheritedProps);
}
