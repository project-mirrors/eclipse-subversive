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

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.EditTreeConflictsPanel;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Edit tree conflicts action helper implementation for Synchronize view
 * 
 * @author Igor Burilo
 */
public class EditTreeConflictsActionHelper extends AbstractActionHelper {

	public EditTreeConflictsActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	@Override
	public IActionOperation getOperation() {
		AbstractSVNSyncInfo syncInfo = getSelectedSVNSyncInfo();
		if (syncInfo != null) {
			ILocalResource local = syncInfo.getLocalResource();
			if (local.hasTreeConflict()) {
				EditTreeConflictsPanel editConflictsPanel = new EditTreeConflictsPanel(local);
				DefaultDialog dialog = new DefaultDialog(configuration.getSite().getShell(), editConflictsPanel);
				if (dialog.open() == 0) {
					return editConflictsPanel.getOperation();
				}
			}
		}
		return null;
	}
}
