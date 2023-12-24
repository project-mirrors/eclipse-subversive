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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.management.CleanupOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Cleanup action helper implementation for Synchronize view
 * 
 * @author Igor Burilo
 */
public class CleanUpActionHelper extends AbstractActionHelper {

	public CleanUpActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);	
	}

	public IActionOperation getOperation() {
		/*
		 * Cleanup versioned selected folder 
		 */
		IResource[] selectedResources = this.getAllSelectedResources();
		IResource[] filteredResources = FileUtility.filterResources(selectedResources, IStateFilter.SF_VERSIONED_FOLDERS, IResource.DEPTH_ZERO);
		CleanupOperation mainOp = new CleanupOperation(filteredResources);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(mainOp);
		op.add(new RefreshResourcesOperation(filteredResources));
		return op;
	}

}
