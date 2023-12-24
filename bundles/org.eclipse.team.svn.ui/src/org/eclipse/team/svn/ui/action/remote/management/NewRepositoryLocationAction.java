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

package org.eclipse.team.svn.ui.action.remote.management;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.svn.ui.wizard.NewRepositoryLocationWizard;

/**
 * New repository location action implementation
 * 
 * @author Alexander Gurov
 */
public class NewRepositoryLocationAction extends TeamAction {

	public NewRepositoryLocationAction() {
		super();
	}
	
	public void run(IAction action) {
		NewRepositoryLocationWizard wizard = new NewRepositoryLocationWizard();
		WizardDialog dialog = new WizardDialog(this.getShell(), wizard);
		dialog.open();
	}

	public boolean isEnabled() {
		return true;
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		// compatibility with 3.3
	}

}
