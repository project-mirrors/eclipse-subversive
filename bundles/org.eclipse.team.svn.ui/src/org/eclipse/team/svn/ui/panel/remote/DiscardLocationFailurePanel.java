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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.remote;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.ProjectListComposite;
import org.eclipse.team.svn.ui.panel.ItemListPanel;

/**
 * Discard location failure panel
 * 
 * @author Sergiy Logvin
 */
public class DiscardLocationFailurePanel extends ItemListPanel {

	protected IProject[] projects;

	public DiscardLocationFailurePanel(String[] locations, IProject[] projects) {
		super(locations, SVNTeamUIPlugin.instance().getImageDescriptor("icons/objects/repository.gif"), //$NON-NLS-1$
				locations.length == 1
						? SVNUIMessages.DiscardLocationFailurePanel_Title_Single
						: SVNUIMessages.DiscardLocationFailurePanel_Title_Multi,
				locations.length == 1
						? SVNUIMessages.DiscardLocationFailurePanel_Description_Single
						: SVNUIMessages.DiscardLocationFailurePanel_Description_Multi,
				locations.length == 1
						? SVNUIMessages.DiscardLocationFailurePanel_Message_Single
						: SVNUIMessages.DiscardLocationFailurePanel_Message_Multi,
				new String[] { SVNUIMessages.DiscardLocationFailurePanel_Disconnect,
						SVNUIMessages.DiscardLocationFailurePanel_Delete, IDialogConstants.CANCEL_LABEL });
		this.projects = projects;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		super.createControlsImpl(parent);

		GridData data = (GridData) table.getLayoutData();
		data.heightHint = 50;
		table.setLayoutData(data);

		ProjectListComposite composite = new ProjectListComposite(parent, SWT.NONE, projects, true);
		composite.initialize();

		data = (GridData) composite.getLayoutData();
		data.heightHint = 100;
		composite.setLayoutData(data);
	}

}
