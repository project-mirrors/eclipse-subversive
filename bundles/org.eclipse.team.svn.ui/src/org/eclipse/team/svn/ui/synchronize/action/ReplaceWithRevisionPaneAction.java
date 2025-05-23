/*******************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *   See git history
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.action.local.ReplaceWithRevisionAction;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Replace with revision action
 * 
 * @author Igor Burilo
 */
public class ReplaceWithRevisionPaneAction extends AbstractSynchronizeModelAction {

	public ReplaceWithRevisionPaneAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IResource[] selectedResources = getAllSelectedResources();
		IActionOperation op = ReplaceWithRevisionAction.getReplaceOperation(selectedResources,
				UIMonitorUtility.getShell());
		return op;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		if (super.updateSelection(selection)) {
			if (selection.size() == 1) {
				IResource[] selectedResources = getAllSelectedResources();
				return FileUtility.checkForResourcesPresence(selectedResources, IStateFilter.SF_ONREPOSITORY,
						IResource.DEPTH_ZERO);
			}
		}
		return false;
	}
}