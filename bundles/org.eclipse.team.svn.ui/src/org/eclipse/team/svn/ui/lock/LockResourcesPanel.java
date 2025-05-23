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

package org.eclipse.team.svn.ui.lock;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;

/**
 * Lock resources panel implementation
 * 
 * @author Igor Burilo
 */
public class LockResourcesPanel extends AbstractDialogPanel {

	protected LockResource[] resources;

	protected boolean hasComment;

	protected LockResourceSelectionComposite selectionComposite;

	protected CommentComposite comment;

	protected SashForm sForm;

	protected Button forceButton;

	protected boolean forceLock;

	public LockResourcesPanel(LockResource[] resources, String dialogTitle, String dialogDescription,
			String defaultMessage) {
		this(resources, false, false, dialogTitle, dialogDescription, defaultMessage);
	}

	public LockResourcesPanel(LockResource[] resources, boolean hasComment, boolean forceLock, String dialogTitle,
			String dialogDescription, String defaultMessage) {
		super(new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL });

		this.resources = resources;
		this.hasComment = hasComment;
		this.forceLock = forceLock;

		this.dialogTitle = dialogTitle;
		this.dialogDescription = dialogDescription;
		this.defaultMessage = defaultMessage;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		if (hasComment) {
			sForm = new SashForm(composite, SWT.VERTICAL);
			layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 0;
			layout.verticalSpacing = 3;
			sForm.setLayout(layout);
			data = new GridData(GridData.FILL_BOTH);
			data.heightHint = 400;
			sForm.setLayoutData(data);

			Composite commentParent = new Composite(sForm, SWT.NONE);
			commentParent.setLayoutData(new GridData(GridData.FILL_BOTH));
			layout = new GridLayout();
			layout.verticalSpacing = 4;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			commentParent.setLayout(layout);

			Group group = new Group(commentParent, SWT.NULL);
			group.setLayout(new GridLayout());
			data = new GridData(GridData.FILL_BOTH);
			group.setLayoutData(data);
			group.setText(SVNUIMessages.LockPanel_Comment);

			comment = new CommentComposite(group, this);
			data = new GridData(GridData.FILL_BOTH);
			comment.setLayoutData(data);

			forceButton = new Button(commentParent, SWT.CHECK);
			data = new GridData();
			forceButton.setLayoutData(data);
			forceButton.setText(SVNUIMessages.LockResourcesPanel_StealLocks);
			forceButton.setSelection(forceLock);
			forceButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					forceLock = forceButton.getSelection();
				}
			});

			Label separator = new Label(commentParent, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		selectionComposite = new LockResourceSelectionComposite(sForm != null ? sForm : composite, SWT.NONE, true,
				true);
		data = new GridData(GridData.FILL_BOTH);
		selectionComposite.setLayoutData(data);
		selectionComposite.setInput(resources);
		selectionComposite.addResourcesSelectionChangedListener(event -> LockResourcesPanel.this.validateContent());
		attachTo(selectionComposite, new AbstractVerifier() {
			@Override
			protected String getErrorMessage(Control input) {
				LockResource[] selection = LockResourcesPanel.this.getSelectedResources();
				if (selection == null || selection.length == 0) {
					return SVNUIMessages.ResourceSelectionComposite_Verifier_Error;
				}
				return null;
			}

			@Override
			protected String getWarningMessage(Control input) {
				return null;
			}
		});

		if (hasComment) {
			IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
			int first = SVNTeamPreferences.getDialogInt(store, SVNTeamPreferences.LOCK_DIALOG_WEIGHT_NAME);
			sForm.setWeights(new int[] { first, 100 - first });
		}
	}

	public String getMessage() {
		return hasComment ? comment.getMessage() : null;
	}

	public boolean getForce() {
		return hasComment ? forceLock : false;
	}

	public LockResource[] getSelectedResources() {
		return selectionComposite.getSelectedResources();
	}

	@Override
	protected void saveChangesImpl() {
		if (hasComment) {
			comment.saveChanges();
			savePreferences();
		}
	}

	protected void savePreferences() {
		int[] weights = sForm.getWeights();
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		SVNTeamPreferences.setDialogInt(store, SVNTeamPreferences.LOCK_DIALOG_WEIGHT_NAME, weights[0] / 10);
	}

	@Override
	protected void cancelChangesImpl() {
		if (hasComment) {
			comment.cancelChanges();
			savePreferences();
		}
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.lockDialogContext"; //$NON-NLS-1$
	}
}
