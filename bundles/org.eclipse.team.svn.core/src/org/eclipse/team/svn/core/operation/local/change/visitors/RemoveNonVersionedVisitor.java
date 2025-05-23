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

package org.eclipse.team.svn.core.operation.local.change.visitors;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.local.change.IActionOperationProcessor;
import org.eclipse.team.svn.core.operation.local.change.IResourceChangeVisitor;
import org.eclipse.team.svn.core.operation.local.change.ResourceChange;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Remove non versioned resources visitor
 * 
 * @author Alexander Gurov
 */
public class RemoveNonVersionedVisitor implements IResourceChangeVisitor {
	protected boolean addedAlso;

	public RemoveNonVersionedVisitor(boolean addedAlso) {
		this.addedAlso = addedAlso;
	}

	@Override
	public void postVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {

	}

	@Override
	public void preVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {
		ILocalResource local = change.getLocal();
		//don't delete ignored resources
		if (IStateFilter.SF_UNVERSIONED.accept(local) && !IStateFilter.SF_IGNORED.accept(local)
				|| addedAlso && local.getStatus() == IStateFilter.ST_ADDED) {
			File real = new File(FileUtility.getWorkingCopyPath(local.getResource()));
			FileUtility.deleteRecursive(real);
		}
	}

}
