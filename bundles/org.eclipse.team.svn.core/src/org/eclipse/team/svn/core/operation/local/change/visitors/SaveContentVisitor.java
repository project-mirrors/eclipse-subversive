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
import org.eclipse.team.svn.core.resource.ILocalFile;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Saves file content
 * 
 * @author Alexander Gurov
 */
public class SaveContentVisitor implements IResourceChangeVisitor {

	@Override
	public void preVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {
		ILocalResource local = change.getLocal();
		if (local instanceof ILocalFile) {
			if (IStateFilter.SF_DELETED.accept(local) && !IStateFilter.SF_PREREPLACEDREPLACED.accept(local)
					|| IStateFilter.SF_UNVERSIONED.accept(local) && "dir_conflicts.prej".equals(local.getName())) { //$NON-NLS-1$
				return;//skip save file content for deleted files; skip directory property reject files
			}
			File real = new File(FileUtility.getWorkingCopyPath(local.getResource()));
			// optimize operation performance using "move on FS" if possible
			if (real.exists() && !real.renameTo(change.getTemporary())) {
				FileUtility.copyFile(change.getTemporary(), real, monitor);
				real.delete();
			}
		}
	}

	@Override
	public void postVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {
	}

}
