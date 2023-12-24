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

package org.eclipse.team.svn.ui.panel.common;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;

/**
 * Show annotation panel implementation It allows to select from and to revisions
 * 
 * @author Igor Burilo
 */
public class ShowAnnotationPanel extends AbstractDialogPanel {

	protected IRepositoryResource resource;

	protected RevisionComposite fromRevision;

	protected RevisionComposite toRevision;

	protected SVNRevisionRange revisions;

	public ShowAnnotationPanel(IRepositoryResource resource) {
		dialogTitle = SVNUIMessages.ShowAnnotationPanel_Title;
		dialogDescription = SVNUIMessages.ShowAnnotationPanel_Description;
		defaultMessage = SVNUIMessages.ShowAnnotationPanel_DefaultMessage;

		this.resource = resource;
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		//from
		fromRevision = new RevisionComposite(parent, this, false, new String[] {
				SVNUIMessages.ShowAnnotationPanel_FromRevision, SVNUIMessages.RevisionComposite_HeadRevision },
				SVNRevision.HEAD, false);
		GridLayout layout = new GridLayout();
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fromRevision.setLayout(layout);
		fromRevision.setLayoutData(data);

		IRepositoryResource fromResource = SVNUtility.copyOf(resource);
		fromResource.setSelectedRevision(resource.getSelectedRevision());
		fromRevision.setSelectedResource(fromResource);
		fromRevision.setRevisionValue(SVNRevision.fromNumber(1));

		//to
		toRevision = new RevisionComposite(parent, this, false, new String[] {
				SVNUIMessages.ShowAnnotationPanel_ToRevision, SVNUIMessages.RevisionComposite_HeadRevision },
				SVNRevision.HEAD, false);
		layout = new GridLayout();
		data = new GridData(GridData.FILL_HORIZONTAL);
		toRevision.setLayout(layout);
		toRevision.setLayoutData(data);

		IRepositoryResource toResource = SVNUtility.copyOf(resource);
		toResource.setSelectedRevision(resource.getSelectedRevision());
		toRevision.setSelectedResource(toResource);
	}

	public SVNRevisionRange getRevisions() {
		return revisions;
	}

	@Override
	protected void cancelChangesImpl() {

	}

	@Override
	protected void saveChangesImpl() {
		revisions = new SVNRevisionRange(fromRevision.getSelectedRevision(), toRevision.getSelectedRevision());
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.showAnnotationDialogContext"; //$NON-NLS-1$
	}
}
