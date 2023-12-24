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
		dialogTitle = template == null
				? SVNUIMessages.EditCommentTemplatePanel_Title_New
				: SVNUIMessages.EditCommentTemplatePanel_Title_Edit;
		defaultMessage = SVNUIMessages.EditCommentTemplatePanel_Message;
		dialogDescription = SVNUIMessages.EditCommentTemplatePanel_Description;
		this.template = template;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
		data.heightHint = 180;
		templateText = SpellcheckedTextProvider.getTextWidget(parent, data, SWT.MULTI);
		templateText.setText(template == null ? "" : template); //$NON-NLS-1$
		templateText.selectAll();
		attachTo(templateText, new NonEmptyFieldVerifier(SVNUIMessages.EditCommentTemplatePanel_Template_Verifier));
	}

	public String getTemplate() {
		return template;
	}

	@Override
	protected void saveChangesImpl() {
		template = templateText.getText().trim();
	}

	@Override
	protected void cancelChangesImpl() {
	}

}
