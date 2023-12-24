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

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.mapping.ModelHelper;
import org.eclipse.team.svn.ui.operation.ShowUpdateViewOperation;
import org.eclipse.team.ui.synchronize.WorkingSetScope;
import org.eclipse.ui.IWorkingSet;

/**
 * Synchronize workspace resources action
 * 
 * @author Alexander Gurov
 */
public class SynchronizeAction extends AbstractWorkingCopyAction {

	public SynchronizeAction() {
		super();
	}

	public void runImpl(IAction action) {
		IWorkingSet []sets = this.getSelectedWorkingSets();
		ShowUpdateViewOperation op;
		
		if (ModelHelper.isShowModelSync()) {
			ResourceMapping[] resourcesMapping = getSelectedResourceMappings(SVNTeamPlugin.NATURE_ID);			 
			op = new ShowUpdateViewOperation(resourcesMapping, this.getTargetPart());	
		} else if (sets != null && sets.length > 0) {
			op = new ShowUpdateViewOperation(new WorkingSetScope(sets), this.getTargetPart());
		}
		else {
			IResource []resources = this.getSelectedResources(IStateFilter.SF_VERSIONED);
			op = new ShowUpdateViewOperation(resources, this.getTargetPart());
		}

		this.runScheduled(op);
	}
	
	public boolean isEnabled() {
		return this.checkForResourcesPresence(IStateFilter.SF_VERSIONED);
	}

	protected boolean needsToSaveDirtyEditors() {
		return true;
	}
	
	protected IWorkingSet []getSelectedWorkingSets() {
		return (IWorkingSet [])this.getAdaptedSelection(IWorkingSet.class);
	}
	
}
