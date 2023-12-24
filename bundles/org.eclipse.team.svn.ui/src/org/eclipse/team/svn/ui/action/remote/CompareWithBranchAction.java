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

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.team.svn.ui.composite.BranchTagSelectionComposite;

/**
 * Compare with branch action implementation
 * 
 * @author Alexander Gurov
 */
public class CompareWithBranchAction extends CompareWithBranchTagAction {

	public CompareWithBranchAction() {
		super(BranchTagSelectionComposite.BRANCH_OPERATED);
	}

}
