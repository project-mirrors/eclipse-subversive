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
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;

/**
 * Panel to type or select one of existent revisions
 * 
 * @author Sergiy Logvin
 */
public class InputRevisionPanel extends AbstractDialogPanel {

	protected IRepositoryResource resource;

	protected boolean isEdit;

	protected String revisionComment;

	protected RevisionComposite revComposite;

	protected CommentComposite commentComposite;

	protected SVNRevision selectedRevision;

	public InputRevisionPanel(IRepositoryResource resource, boolean isEdit, String revisionComment) {
		this.resource = resource;
		this.isEdit = isEdit;
		this.revisionComment = revisionComment;

		dialogTitle = isEdit ? SVNUIMessages.InputRevisionPanel_EditTitle : SVNUIMessages.InputRevisionPanel_AddTitle;
		dialogDescription = this.resource == null
				? SVNUIMessages.InputRevisionPanel_SingleDescription
				: SVNUIMessages.InputRevisionPanel_MultipleDescription;
		defaultMessage = this.resource == null
				? SVNUIMessages.InputRevisionPanel_SingleMessage
				: SVNUIMessages.InputRevisionPanel_MultipleMessage;
	}

	public SVNRevision getSelectedRevision() {
		return selectedRevision;
	}

	public String getRevisionComment() {
		return revisionComment;
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		if (resource != null) {
			revComposite = new RevisionComposite(parent, this, false, new String[] {
					SVNUIMessages.InputRevisionPanel_Caption_First, SVNUIMessages.InputRevisionPanel_Caption_Second },
					SVNRevision.HEAD, false);
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			revComposite.setLayoutData(data);

			revComposite.setSelectedResource(resource);
			revComposite.setRevisionValue(resource.getSelectedRevision());
		}

		Group group = new Group(parent, SWT.NULL);
		group.setLayout(new GridLayout());
		GridData data = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(data);
		group.setText(SVNUIMessages.InputRevisionPanel_Comment);

		commentComposite = new CommentComposite(group, this);
		data = new GridData(GridData.FILL_BOTH);
		commentComposite.setLayoutData(data);

		if (revisionComment != null) {
			commentComposite.setMessage(revisionComment);
		}
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.revisionDialogContext"; //$NON-NLS-1$
	}

	@Override
	protected void saveChangesImpl() {
		if (resource != null) {
			resource = revComposite.getSelectedResource();
			selectedRevision = revComposite.getSelectedRevision();
		}
		commentComposite.saveChanges();
		revisionComment = commentComposite.getMessage();
	}

	@Override
	protected void cancelChangesImpl() {
		commentComposite.cancelChanges();
	}

}
