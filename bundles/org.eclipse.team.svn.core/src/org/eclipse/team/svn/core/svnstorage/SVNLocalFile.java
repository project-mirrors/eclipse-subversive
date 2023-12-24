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

package org.eclipse.team.svn.core.svnstorage;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.resource.ILocalFile;

/**
 * Working Copy file representation
 * 
 * @author Alexander Gurov
 */
public class SVNLocalFile extends SVNLocalResource implements ILocalFile {
	public SVNLocalFile(IResource resource, long revision, long baseRevision, String textStatus, String propStatus,
			int changeMask, String author, long lastCommitDate, SVNConflictDescriptor treeConflictDescriptor) {
		super(resource, revision, baseRevision, textStatus, propStatus, changeMask, author, lastCommitDate,
				treeConflictDescriptor);
	}

}
