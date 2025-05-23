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
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;

/**
 * Move projects to working set operation
 * 
 * @author Sergiy Logvin
 */
public class MoveProjectsToWorkingSetOperation extends AbstractWorkingCopyOperation {
	protected String workingSetName;

	public MoveProjectsToWorkingSetOperation(IResource[] projects, String workingSetName) {
		super("Operation_MoveToWorkingSet", SVNUIMessages.class, projects); //$NON-NLS-1$
		this.workingSetName = workingSetName;
	}

	public MoveProjectsToWorkingSetOperation(IResourceProvider provider, String workingSetName) {
		super("Operation_MoveToWorkingSet", SVNUIMessages.class, provider); //$NON-NLS-1$
		this.workingSetName = workingSetName;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] projects = operableData();
		IWorkingSet wSet = null;
		IWorkingSetManager workingSetManager = SVNTeamUIPlugin.instance().getWorkbench().getWorkingSetManager();
		IWorkingSet[] workingSets = workingSetManager.getWorkingSets();
		for (IWorkingSet workingSet : workingSets) {
			if (workingSet.getName().equals(workingSetName)) {
				wSet = workingSet;
				List<IAdaptable> existing = new ArrayList<>(Arrays.asList(wSet.getElements()));
				existing.addAll(Arrays.asList(projects));
				wSet.setElements(existing.toArray(new IAdaptable[existing.size()]));
				break;
			}
		}
		if (wSet == null) {
			wSet = workingSetManager.createWorkingSet(workingSetName, projects);
			wSet.setId("org.eclipse.ui.resourceWorkingSetPage"); //$NON-NLS-1$
			workingSetManager.addWorkingSet(wSet);
		}
	}

}
