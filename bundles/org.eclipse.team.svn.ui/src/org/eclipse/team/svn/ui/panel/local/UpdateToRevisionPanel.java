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

package org.eclipse.team.svn.ui.panel.local;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.DepthSelectionComposite;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;

/**
 * Update to revision panel implementation
 * 
 * @author Igor Burilo
 */
public class UpdateToRevisionPanel extends AbstractDialogPanel {

	protected RevisionComposite revisionComposite;

	protected DepthSelectionComposite depthSelector;

	protected IRepositoryResource selectedResource;

	protected boolean canShowUpdateDepthPath;

	//output
	protected SVNRevision revision;

	protected SVNDepth depth;

	protected boolean isStickyDepth;

	protected String updatePath;

	public UpdateToRevisionPanel(IRepositoryResource selectedResource, boolean canShowUpdateDepthPath) {
		dialogTitle = SVNUIMessages.UpdateToRevisionPanel_Title;
		dialogDescription = SVNUIMessages.UpdateToRevisionPanel_Description;
		defaultMessage = SVNUIMessages.UpdateToRevisionPanel_Message;

		this.selectedResource = selectedResource;
		this.canShowUpdateDepthPath = canShowUpdateDepthPath;
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		revisionComposite = new RevisionComposite(parent, this, false,
				new String[] { SVNUIMessages.RevisionComposite_Revision, SVNUIMessages.RevisionComposite_HeadRevision },
				SVNRevision.HEAD, false);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		revisionComposite.setLayoutData(data);
		revisionComposite.setSelectedResource(selectedResource);

		depthSelector = new DepthSelectionComposite(parent, SWT.NONE, true, true, canShowUpdateDepthPath,
				selectedResource, this);
		data = new GridData(GridData.FILL_HORIZONTAL);
		depthSelector.setLayoutData(data);
	}

	@Override
	protected void cancelChangesImpl() {
	}

	@Override
	protected void saveChangesImpl() {
		revision = revisionComposite.getSelectedRevision();
		depth = depthSelector.getDepth();
		isStickyDepth = depthSelector.isStickyDepth();
		if (isStickyDepth) {
			updatePath = depthSelector.getUpdatePath();
		}
	}

	public SVNRevision getRevision() {
		return revision;
	}

	public SVNDepth getDepth() {
		return depth;
	}

	public boolean isStickyDepth() {
		return isStickyDepth;
	}

	public String getUpdateDepthPath() {
		return updatePath;
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.updateDialogContext"; //$NON-NLS-1$
	}

}
