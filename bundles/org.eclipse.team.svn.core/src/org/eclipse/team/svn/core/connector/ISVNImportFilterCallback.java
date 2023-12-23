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
 * Allows to filter out specific nodes during import.
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library
 * is not EPL compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is
 * providing our own connector interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 * 
 * @since 1.8
 */
public interface ISVNImportFilterCallback {
	/**
	 * This method should return true for the nodes that should be filtered out with the entire their subtree. 
	 * 
	 * @param path
	 *            the entry's path
	 * @param kind
	 *            the entry's kind
	 * @param special
	 *            
	 * @return {@link Boolean}
	 */
	public boolean filterOut(String path, SVNEntry.Kind kind, boolean special);
}
