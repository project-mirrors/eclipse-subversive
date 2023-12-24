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

package org.eclipse.team.svn.ui.panel.view;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;

/**
 * History filter modification dialog
 * 
 * @author Alexander Gurov
 */
public class HistoryFilterPanel extends AbstractDialogPanel {
	protected static final String FILTER_AUTHOR_HISTORY_NAME = "filterAuthor"; //$NON-NLS-1$

	protected static final String FILTER_COMMENT_HISTORY_NAME = "filterComment"; //$NON-NLS-1$

	protected static final String FILTER_PATH_HISTORY_NAME = "filterPath"; //$NON-NLS-1$

	protected String filter;

	protected Button authorButton;

	protected Button commentButton;

	protected Button pathButton;

	protected Combo authorsCombo;

	protected Combo commentsCombo;

	protected Combo pathCombo;

	protected String authorInput;

	protected String commentInput;

	protected String pathInput;

	protected String[] selectedAuthors;

	protected UserInputHistory authorsHistory;

	protected UserInputHistory commentsHistory;

	protected UserInputHistory pathHistory;

	public HistoryFilterPanel(String authorInput, String commentInput, String pathInput, String[] selectedAuthors) {
		dialogTitle = SVNUIMessages.HistoryFilterPanel_Title;
		dialogDescription = SVNUIMessages.HistoryFilterPanel_Description;
		defaultMessage = SVNUIMessages.HistoryFilterPanel_Message;

		this.selectedAuthors = selectedAuthors;
		this.authorInput = authorInput;
		this.commentInput = commentInput;
		this.pathInput = pathInput;
	}

	public String getAuthor() {
		return authorInput;
	}

	public String getComment() {
		return commentInput;
	}

	public String getChangedPath() {
		return pathInput;
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(470, 100);
	}

	@Override
	public void createControlsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		authorButton = new Button(composite, SWT.CHECK);
		authorButton.setText(SVNUIMessages.HistoryFilterPanel_Author);
		data = new GridData();
		authorButton.setLayoutData(data);
		boolean enabledAuthor = authorInput != null;
		authorButton.setSelection(enabledAuthor);
		authorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				authorsCombo.setEnabled(((Button) e.widget).getSelection());
			}
		});

		authorsHistory = new UserInputHistory(HistoryFilterPanel.FILTER_AUTHOR_HISTORY_NAME);
		authorsCombo = new Combo(composite, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		authorsCombo.setLayoutData(data);
		authorsCombo.setEnabled(enabledAuthor);
		authorsCombo.setVisibleItemCount(authorsHistory.getDepth());
		authorsCombo.setItems(mergeAuthorsList());
		authorsCombo.setText(authorInput == null ? "" : authorInput); //$NON-NLS-1$

		commentButton = new Button(composite, SWT.CHECK);
		commentButton.setText(SVNUIMessages.HistoryFilterPanel_Comment);
		data = new GridData();
		commentButton.setLayoutData(data);
		boolean enabledComment = commentInput != null;
		commentButton.setSelection(enabledComment);
		commentButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commentsCombo.setEnabled(((Button) e.widget).getSelection());
			}
		});

		commentsHistory = new UserInputHistory(HistoryFilterPanel.FILTER_COMMENT_HISTORY_NAME);
		commentsCombo = new Combo(composite, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		commentsCombo.setLayoutData(data);
		commentsCombo.setEnabled(enabledComment);
		commentsCombo.setVisibleItemCount(commentsHistory.getDepth());
		commentsCombo.setItems(commentsHistory.getHistory());
		commentsCombo.setText(commentInput == null ? "" : commentInput); //$NON-NLS-1$

		pathButton = new Button(composite, SWT.CHECK);
		pathButton.setText(SVNUIMessages.HistoryFilterPanel_Path);
		data = new GridData();
		pathButton.setLayoutData(data);
		boolean enabledPath = pathInput != null;
		pathButton.setSelection(enabledPath);
		pathButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathCombo.setEnabled(((Button) e.widget).getSelection());
				HistoryFilterPanel.this.validateContent();
			}
		});

		pathHistory = new UserInputHistory(HistoryFilterPanel.FILTER_PATH_HISTORY_NAME);
		pathCombo = new Combo(composite, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		pathCombo.setLayoutData(data);
		pathCombo.setEnabled(enabledPath);
		pathCombo.setVisibleItemCount(pathHistory.getDepth());
		pathCombo.setItems(pathHistory.getHistory());
		pathCombo.setText(pathInput == null ? "" : pathInput); //$NON-NLS-1$
		attachTo(pathCombo, new NonEmptyFieldVerifier(SVNUIMessages.HistoryFilterPanel_Path) {
			@Override
			protected String getErrorMessageImpl(Control input) {
				if (pathCombo.isEnabled()) {
					return super.getErrorMessageImpl(input);
				}
				return null;
			}
		});
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.historyDialogContext"; //$NON-NLS-1$
	}

	@Override
	protected void saveChangesImpl() {
		if (authorButton.getSelection()) {
			authorInput = authorsCombo.getText();
			authorsHistory.addLine(authorInput);
		} else {
			authorInput = null;
		}
		if (commentButton.getSelection()) {
			commentInput = commentsCombo.getText();
			commentsHistory.addLine(commentInput);
		} else {
			commentInput = null;
		}
		if (pathButton.getSelection()) {
			pathInput = pathCombo.getText();
			pathHistory.addLine(pathInput);
		} else {
			pathInput = null;
		}
	}

	@Override
	protected void cancelChangesImpl() {
	}

	protected String[] mergeAuthorsList() {
		HashSet<String> merged = new HashSet<>(Arrays.asList(selectedAuthors));
		merged.addAll(Arrays.asList(authorsHistory.getHistory()));
		String[] retVal = merged.toArray(new String[merged.size()]);
		Arrays.sort(retVal);
		return retVal;
	}

}
