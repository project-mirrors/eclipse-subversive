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

package org.eclipse.team.svn.ui.panel.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.SpellcheckedTextProvider;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;

/**
 * Edit comment templates panel
 *
 * @author Sergiy Logvin
 */
public class EditCommentTemplatePanel extends AbstractDialogPanel {

	protected StyledText templateText;

	protected String template;

	public EditCommentTemplatePanel(String template) {
		super();
		this.dialogTitle = template == null
				? SVNUIMessages.EditCommentTemplatePanel_Title_New
				: SVNUIMessages.EditCommentTemplatePanel_Title_Edit;
		this.defaultMessage = SVNUIMessages.EditCommentTemplatePanel_Message;
		this.dialogDescription = SVNUIMessages.EditCommentTemplatePanel_Description;
		this.template = template;
	}

	public void createControlsImpl(Composite parent) {
		GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
		data.heightHint = 180;
		this.templateText = SpellcheckedTextProvider.getTextWidget(parent, data, SWT.MULTI);
		this.templateText.setText(this.template == null ? "" : this.template); //$NON-NLS-1$
		this.templateText.selectAll();
		this.attachTo(this.templateText,
				new NonEmptyFieldVerifier(SVNUIMessages.EditCommentTemplatePanel_Template_Verifier));
	}

	public String getTemplate() {
		return this.template;
	}

	protected void saveChangesImpl() {
		this.template = this.templateText.getText().trim();
	}

	protected void cancelChangesImpl() {
	}

}
