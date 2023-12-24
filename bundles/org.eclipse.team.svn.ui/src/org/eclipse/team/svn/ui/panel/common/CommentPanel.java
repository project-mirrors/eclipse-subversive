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
 *    Takashi Okamoto - [patch] Fix bugtraq integration bug (when bug id is empty)
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.properties.bugtraq.BugtraqModel;

/**
 * Operation comment panel
 * 
 * @author Alexander Gurov
 */
public class CommentPanel extends AbstractDialogPanel {
	protected CommentComposite comment;

	protected BugtraqModel bugtraqModel;

	protected String message;

	public CommentPanel(String title) {
		dialogTitle = title;
		dialogDescription = SVNUIMessages.CommentPanel_Description;
		defaultMessage = SVNUIMessages.CommentPanel_Message;
	}

	public CommentPanel(String title, BugtraqModel bugtraqModel) {
		this(title);
		this.bugtraqModel = bugtraqModel;
	}

	public String getMessage() {
		if (comment != null) {
			message = comment.getMessage();
		}
		return appendBugtraqMessage(message);
	}

	public void setMessage(String message) {
		this.message = message;
		if (comment != null) {
			comment.setMessage(message);
		}
	}

	@Override
	public void createControlsImpl(Composite parent) {
		comment = new CommentComposite(parent, this);
		GridData data = new GridData(GridData.FILL_BOTH);
		comment.setLayoutData(data);
	}

	@Override
	protected Point getPrefferedSizeImpl() {
		return new Point(510, SWT.DEFAULT);
	}

	@Override
	public void postInit() {
		super.postInit();
		comment.postInit(manager);
		if (message != null) {
			comment.setMessage(message);
		}
	}

	@Override
	protected void saveChangesImpl() {
		comment.saveChanges();
	}

	@Override
	protected void cancelChangesImpl() {
		comment.cancelChanges();
	}

	protected String appendBugtraqMessage(String message) {
		String bugtraqMessage;
		if (bugtraqModel != null && (bugtraqMessage = bugtraqModel.getMessage()) != null) {
			String addString = comment.getBugID();
			if (addString != null && addString.trim().length() > 0) {
				bugtraqMessage = bugtraqMessage.replaceAll(BugtraqModel.BUG_ID, addString);
				if (bugtraqModel.isAppend()) {
					message += "\n" + bugtraqMessage; //$NON-NLS-1$
				} else {
					message = bugtraqMessage + "\n" + message; //$NON-NLS-1$
				}
			}
		}
		return message;
	}

}
