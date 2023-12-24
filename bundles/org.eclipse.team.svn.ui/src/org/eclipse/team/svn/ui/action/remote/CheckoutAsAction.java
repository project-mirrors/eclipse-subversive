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

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.action.AbstractRepositoryModifyWorkspaceAction;
import org.eclipse.team.svn.ui.wizard.CheckoutAsWizard;

/**
 * UI Checkout As action
 * 
 * @author Alexander Gurov
 */
public class CheckoutAsAction extends AbstractRepositoryModifyWorkspaceAction {
	public CheckoutAsAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IRepositoryResource[] selectedResources = getSelectedRepositoryResources();
		IRepositoryLocation[] locations = getSelectedRepositoryLocations();
		if (selectedResources.length == 0) {
			selectedResources = new IRepositoryResource[locations.length];
			for (int i = 0; i < locations.length; i++) {
				selectedResources[i] = locations[i].getRoot();
			}
		}

		CheckoutAsWizard checkoutWizard = new CheckoutAsWizard(selectedResources);
		WizardDialog dialog = new WizardDialog(getShell(), checkoutWizard);
		dialog.create();
		dialog.getShell()
				.setSize(Math.max(CheckoutAsWizard.SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
						CheckoutAsWizard.SIZING_WIZARD_HEIGHT);
		dialog.open();
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
