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

package org.eclipse.team.svn.ui.action.local.management;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.AddRepositoryPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * New repository action implementation
 * 
 * @author Igor Burilo
 */
public class NewRepositoryAction extends TeamAction {

	@Override
	public void run(IAction action) {
		AddRepositoryPanel addRepositoryPanel = new AddRepositoryPanel();
		DefaultDialog dialog = new DefaultDialog(getShell(), addRepositoryPanel);
		if (dialog.open() == 0) {
			IActionOperation op = addRepositoryPanel.getOperationToPeform();
			if (op != null) {
				UIMonitorUtility.doTaskScheduledActive(op);
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		// compatibility with 3.3
	}

	public static boolean checkEnablement() {
		return (CoreExtensionsManager.instance().getSVNConnectorFactory().getSupportedFeatures()
				& ISVNConnectorFactory.OptionalFeatures.CREATE_REPOSITORY) != 0;
	}

}
