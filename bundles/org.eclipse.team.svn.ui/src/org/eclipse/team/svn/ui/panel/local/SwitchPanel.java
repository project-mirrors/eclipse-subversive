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

package org.eclipse.team.svn.ui.panel.local;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.DepthSelectionComposite;
import org.eclipse.team.svn.ui.composite.RepositoryResourceSelectionComposite;
import org.eclipse.team.svn.ui.panel.common.AbstractRepositoryResourceSelectionPanel;

/**
 * Switch panel implementation
 * 
 * @author Alexander Gurov
 */
public class SwitchPanel extends AbstractRepositoryResourceSelectionPanel {

	protected boolean containFolders;

	protected DepthSelectionComposite depthSelector;

	public SwitchPanel(IRepositoryResource baseResource, long currentRevision, boolean containFolders) {
		super(baseResource, currentRevision, SVNUIMessages.SwitchPanel_Title, SVNUIMessages.SwitchPanel_Description,
				"SwitchPanel_URL_HISTORY_NAME", SVNUIMessages.SwitchPanel_Selection_Title, //$NON-NLS-1$
				SVNUIMessages.SwitchPanel_Selection_Description, RepositoryResourceSelectionComposite.TEXT_BASE);
		defaultMessage = SVNUIMessages.SwitchPanel_Message;
		this.containFolders = containFolders;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		super.createControlsImpl(parent);
		if (containFolders) {
			Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			separator.setVisible(false);

			boolean canShowUpdateDepthPath = false;
			depthSelector = new DepthSelectionComposite(parent, SWT.NONE, true, true, canShowUpdateDepthPath,
					selectedResource, this);

			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			depthSelector.setLayoutData(data);
		}
	}

	public SVNDepth getDepth() {
		if (depthSelector == null) {
			return SVNDepth.INFINITY;
		}
		return depthSelector.getDepth();
	}

	public boolean isStickyDepth() {
		if (depthSelector == null) {
			return false;
		}
		return depthSelector.isStickyDepth();
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.switchDialogContext"; //$NON-NLS-1$
	}

}
