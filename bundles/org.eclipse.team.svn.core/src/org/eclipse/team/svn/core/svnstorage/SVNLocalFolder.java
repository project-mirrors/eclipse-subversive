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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.operation.local.GetAllResourcesOperation;
import org.eclipse.team.svn.core.resource.ILocalFolder;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Working Copy folder representation
 * 
 * @author Alexander Gurov
 */
public class SVNLocalFolder extends SVNLocalResource implements ILocalFolder {
	public SVNLocalFolder(IResource resource, long revision, long baseRevision, String textStatus, String propStatus,
			int changeMask, String author, long lastCommitDate, SVNConflictDescriptor treeConflictDescriptor) {
		super(resource, revision, baseRevision, textStatus, propStatus, changeMask, author, lastCommitDate,
				treeConflictDescriptor);
	}

	@Override
	public ILocalResource[] getChildren() {
		IContainer root = (IContainer) resource;
		List<ILocalResource> members = new ArrayList<>();

		GetAllResourcesOperation op = new GetAllResourcesOperation(root);
		ProgressMonitorUtility.doTaskExternalDefault(op, new NullProgressMonitor());
		IResource[] resources = op.getChildren();
		for (IResource element : resources) {
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(element);
			if (!IStateFilter.SF_INTERNAL_INVALID.accept(local) && local.getStatus() != IStateFilter.ST_NOTEXISTS) {
				members.add(local);
			}
		}

		return members.toArray(new ILocalResource[members.size()]);
	}

}
