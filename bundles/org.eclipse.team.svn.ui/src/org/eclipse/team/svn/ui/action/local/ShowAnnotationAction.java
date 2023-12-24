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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.annotate.BuiltInAnnotate;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Show annotation for local versioned file
 * 
 * @author Alexander Gurov
 */
public class ShowAnnotationAction extends AbstractWorkingCopyAction {

	public ShowAnnotationAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource resource = this.getSelectedResources(IStateFilter.SF_ONREPOSITORY)[0];
		IWorkbenchPage page = getTargetPage();
		// could be called by keyboard actions for any resource, or there could be no page to show annotation in
		if (resource.getType() == IResource.FILE && page != null) {
			IActionOperation op = new BuiltInAnnotate().getAnnotateOperation(page, (IFile) resource, getShell());
			if (op != null) {
				runScheduled(op);
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return this.getSelectedResources().length == 1 && checkForResourcesPresence(IStateFilter.SF_ONREPOSITORY);
	}

}
