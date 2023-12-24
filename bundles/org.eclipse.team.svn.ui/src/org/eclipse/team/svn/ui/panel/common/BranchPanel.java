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

package org.eclipse.team.svn.ui.panel.common;

import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;

/**
 * Select branch folder panel
 * 
 * @author Alexander Gurov
 */
public class BranchPanel extends AbstractBranchTagPanel {

	public BranchPanel(IRepositoryRoot branchTo, boolean showStartsWith, Set existingNames, IRepositoryResource[] selectedRemoteResources) {
		this(branchTo, showStartsWith, existingNames, new IResource[0], selectedRemoteResources);
    }
	
	public BranchPanel(IRepositoryRoot branchTo, boolean showStartsWith, Set existingNames, IResource[] resources, IRepositoryResource[] selectedRemoteResources) {
		super(branchTo, showStartsWith, existingNames, "BranchPanel", "branch", resources, selectedRemoteResources);
	}
	
	public String getHelpId() {
    	return "org.eclipse.team.svn.help.branchDialogContext"; //$NON-NLS-1$
    }
}
