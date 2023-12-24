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

package org.eclipse.team.svn.ui.wizard.shareproject;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Page to edit commit comment
 * 
 * @author Sergiy Logvin
 */
public class EditCommentPage extends AbstractVerifiedWizardPage {
	protected String commitComment;

	protected IRepositoryLocation location;

	protected CommentComposite commentComposite;

	protected IResourceProvider provider;

	protected boolean showCommitDialog;

	public EditCommentPage(IResourceProvider provider) {
		super(EditCommentPage.class.getName(), SVNUIMessages.EditCommentPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		setDescription(SVNUIMessages.EditCommentPage_Description);
		this.provider = provider;
	}

	public boolean isShowCommitDialog() {
		return showCommitDialog;
	}

	public String getCommitComment() {
		commentComposite.saveChanges();
		return commentComposite.getMessage();
	}

	public void setSelectedRepositoryLocation(IRepositoryLocation location) {
		this.location = location;
	}

	public void setDefaultCommitMessage() {
		commitComment = ""; //$NON-NLS-1$
		IProject[] projects = getProjects();
		for (IProject project : projects) {
			String commentPart = ShareProjectOperation.getDefaultComment(project, location.getRoot());
			commitComment += commitComment.length() == 0 ? commentPart : "\n" + commentPart; //$NON-NLS-1$
		}
		commentComposite.setMessage(commitComment);
	}

	protected IProject[] getProjects() {
		return (IProject[]) provider.getResources();
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		commentComposite = new CommentComposite(composite, commitComment, this, null, null);
		GridData data = new GridData(GridData.FILL_BOTH);
		commentComposite.setLayoutData(data);

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		separator.setVisible(false);

		Button showComment = new Button(composite, SWT.CHECK);
		showComment.setText(SVNUIMessages.EditCommentPage_LaunchCommit);
		showComment.setSelection(showCommitDialog = true);
		showComment.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showCommitDialog = ((Button) e.widget).getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, "org.eclipse.team.svn.help.editCommentContext"); //$NON-NLS-1$

		return composite;
	}

}
