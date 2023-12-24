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

package org.eclipse.team.svn.ui.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.SpellcheckedTextProvider;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.common.EditCommentTemplatePanel;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IntegerFieldVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.ui.PlatformUI;

/**
 * Comments templates preferences page implementation
 *
 * @author Sergiy Logvin
 */
public class SVNTeamCommentTemplatesPreferencesPage extends AbstractSVNTeamPreferencesPage
		implements ISelectionChangedListener {
	protected StyledText previewText;

	protected ListViewer listViewer;

	protected Button newButton;

	protected Button editButton;

	protected Button removeButton;

	protected Button useLogTemplatesButton;

	protected Button useTemplatesButton;

	protected Button useShiftEnterButton;

	protected int savedCommentsCount;

	protected int savedPathsCount;

	protected Text savedCommentsCountField;

	protected Text savedPathsCountField;

	protected boolean logTemplatesEnabled;

	protected boolean userTemplatesEnabled;

	protected boolean useShiftEnter;

	public SVNTeamCommentTemplatesPreferencesPage() {
	}

	@Override
	protected void saveValues(IPreferenceStore store) {
		SVNTeamPreferences.setCommentTemplatesInt(store, SVNTeamPreferences.COMMENT_SAVED_COMMENTS_COUNT_NAME,
				savedCommentsCount);
		SVNTeamPreferences.setCommentTemplatesInt(store, SVNTeamPreferences.COMMENT_SAVED_PATHS_COUNT_NAME,
				savedPathsCount);
		SVNTeamPreferences.setCommentTemplatesBoolean(store, SVNTeamPreferences.COMMENT_LOG_TEMPLATES_ENABLED_NAME,
				logTemplatesEnabled);
		SVNTeamPreferences.setCommentTemplatesBoolean(store, SVNTeamPreferences.COMMENT_TEMPLATES_LIST_ENABLED_NAME,
				userTemplatesEnabled);
		SVNTeamPreferences.setCommentTemplatesBoolean(store, SVNTeamPreferences.COMMENT_USE_SHIFT_ENTER_NAME,
				useShiftEnter);

		int numTemplates = listViewer.getList().getItemCount();
		String[] templates = new String[numTemplates];
		for (int i = 0; i < numTemplates; i++) {
			templates[i] = (String) listViewer.getElementAt(i);
		}
		SVNTeamPreferences.setCommentTemplatesString(store, SVNTeamPreferences.COMMENT_TEMPLATES_LIST_NAME,
				FileUtility.encodeArrayToString(templates));
	}

	@Override
	protected void loadDefaultValues(IPreferenceStore store) {
		savedCommentsCount = SVNTeamPreferences.COMMENT_SAVED_COMMENTS_COUNT_DEFAULT;
		savedPathsCount = SVNTeamPreferences.COMMENT_SAVED_PATHS_COUNT_DEFAULT;
		logTemplatesEnabled = SVNTeamPreferences.COMMENT_LOG_TEMPLATES_ENABLED_DEFAULT;
		userTemplatesEnabled = SVNTeamPreferences.COMMENT_TEMPLATES_LIST_ENABLED_DEFAULT;
		useShiftEnter = SVNTeamPreferences.COMMENT_USE_SHIFT_ENTER_DEFAULT;
	}

	@Override
	protected void loadValues(IPreferenceStore store) {
		savedCommentsCount = SVNTeamPreferences.getCommentTemplatesInt(store,
				SVNTeamPreferences.COMMENT_SAVED_COMMENTS_COUNT_NAME);
		savedPathsCount = SVNTeamPreferences.getCommentTemplatesInt(store,
				SVNTeamPreferences.COMMENT_SAVED_PATHS_COUNT_NAME);
		logTemplatesEnabled = SVNTeamPreferences.getCommentTemplatesBoolean(store,
				SVNTeamPreferences.COMMENT_LOG_TEMPLATES_ENABLED_NAME);
		userTemplatesEnabled = SVNTeamPreferences.getCommentTemplatesBoolean(store,
				SVNTeamPreferences.COMMENT_TEMPLATES_LIST_ENABLED_NAME);
		useShiftEnter = SVNTeamPreferences.getCommentTemplatesBoolean(store,
				SVNTeamPreferences.COMMENT_USE_SHIFT_ENTER_NAME);
	}

	@Override
	protected void initializeControls() {
		savedCommentsCountField.setText(String.valueOf(savedCommentsCount));
		savedPathsCountField.setText(String.valueOf(savedPathsCount));
		useLogTemplatesButton.setSelection(logTemplatesEnabled);
		useTemplatesButton.setSelection(userTemplatesEnabled);
		useShiftEnterButton.setSelection(useShiftEnter);

		listViewer.getControl().setEnabled(userTemplatesEnabled);
		newButton.setEnabled(userTemplatesEnabled);
		previewText.setEnabled(userTemplatesEnabled);

		IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
		editButton.setEnabled(userTemplatesEnabled && selection.size() == 1);
		removeButton.setEnabled(userTemplatesEnabled && selection.size() > 0);
		previewText.setText(selection.size() == 1 ? (String) selection.getFirstElement() : ""); //$NON-NLS-1$
	}

	@Override
	protected Control createContentsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite checkBoxComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		checkBoxComposite.setLayout(layout);
		checkBoxComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(checkBoxComposite, SWT.NONE);
		GridData data = new GridData();
		label.setLayoutData(data);
		String labelText = SVNUIMessages.CommentTemplatesPreferencePage_historySavedCommentsCount;
		label.setText(labelText);

		savedCommentsCountField = new Text(checkBoxComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		savedCommentsCountField.setLayoutData(data);
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(labelText));
		verifier.add(new IntegerFieldVerifier(labelText, true));
		attachTo(savedCommentsCountField, verifier);
		savedCommentsCountField.addModifyListener(e -> {
			try {
				savedCommentsCount = Integer.parseInt(savedCommentsCountField.getText());
			} catch (Exception ex) {
			}
		});

		label = new Label(checkBoxComposite, SWT.NONE);
		data = new GridData();
		label.setLayoutData(data);
		labelText = SVNUIMessages.CommentTemplatesPreferencePage_historySavedPathsCount;
		label.setText(labelText);

		savedPathsCountField = new Text(checkBoxComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		savedPathsCountField.setLayoutData(data);
		verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(labelText));
		verifier.add(new IntegerFieldVerifier(labelText, true));
		attachTo(savedPathsCountField, verifier);
		savedPathsCountField.addModifyListener(e -> {
			try {
				savedPathsCount = Integer.parseInt(savedPathsCountField.getText());
			} catch (Exception ex) {
			}
		});

		useShiftEnterButton = new Button(checkBoxComposite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		useShiftEnterButton.setLayoutData(data);
		useShiftEnterButton.setText(SVNUIMessages.CommentTemplatesPreferencePage_UseShiftEnter);
		useShiftEnterButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useShiftEnter = useShiftEnterButton.getSelection();
			}
		});

		useLogTemplatesButton = new Button(checkBoxComposite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		useLogTemplatesButton.setLayoutData(data);
		useLogTemplatesButton.setText(SVNUIMessages.CommentTemplatesPreferencePage_LogTemplates);
		useLogTemplatesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				logTemplatesEnabled = ((Button) e.widget).getSelection();
			}
		});

		useTemplatesButton = new Button(checkBoxComposite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		useTemplatesButton.setLayoutData(data);
		useTemplatesButton.setText(SVNUIMessages.CommentTemplatesPreferencePage_UserTemplates);
		useTemplatesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNTeamCommentTemplatesPreferencesPage.this.selectionChanged(null);
			}
		});

		Label separator = new Label(checkBoxComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		separator.setLayoutData(data);

		Label templatesLabel = new Label(composite, SWT.NONE);
		templatesLabel.setText(SVNUIMessages.CommentTemplatesPreferencePage_EditHint);

		createTemplatesList(composite);

		Dialog.applyDialogFont(parent);

//		Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(parent, "org.eclipse.team.svn.help.commentTemplatesPreferencesContext"); //$NON-NLS-1$

		return composite;
	}

	protected Composite createTemplatesList(Composite parent) {
		Composite listAndButtons = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		listAndButtons.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 430;
		listAndButtons.setLayoutData(data);

		listViewer = new ListViewer(listAndButtons);
		listViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String template = (String) element;
				return FileUtility.flattenText(template);
			}
		});
		listViewer.addSelectionChangedListener(this);
		listViewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				String template1 = FileUtility.flattenText((String) e1);
				String template2 = FileUtility.flattenText((String) e2);
				return template1.compareToIgnoreCase(template2);
			}
		});
		listViewer.addDoubleClickListener(event -> SVNTeamCommentTemplatesPreferencesPage.this.editTemplate());
		List list = listViewer.getList();
		list.setLayoutData(new GridData(GridData.FILL_BOTH));

		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		String[] templates = FileUtility.decodeStringToArray(
				SVNTeamPreferences.getCommentTemplatesString(store, SVNTeamPreferences.COMMENT_TEMPLATES_LIST_NAME));

		// populate list
		for (String template : templates) {
			listViewer.add(template);
		}

		createButtons(listAndButtons);

		Label previewLabel = new Label(listAndButtons, SWT.NONE);
		data = new GridData();
		data.horizontalSpan = 2;
		previewLabel.setLayoutData(data);
		previewLabel.setText(SVNUIMessages.CommentTemplatesPreferencePage_ViewHint);

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = convertHeightInCharsToPixels(5);
		previewText = SpellcheckedTextProvider.getTextWidget(listAndButtons, data, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		previewText.setEditable(false);

		return listAndButtons;
	}

	protected void createButtons(Composite parent) {
		Composite buttons = new Composite(parent, SWT.NONE);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		buttons.setLayout(layout);

		newButton = new Button(buttons, SWT.PUSH);
		newButton.setText(SVNUIMessages.Button_New);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.widthHint = DefaultDialog.computeButtonWidth(newButton);
		newButton.setLayoutData(data);
		newButton.setEnabled(true);
		newButton.addListener(SWT.Selection, event -> SVNTeamCommentTemplatesPreferencesPage.this.newTemplate());

		editButton = new Button(buttons, SWT.PUSH);
		editButton.setText(SVNUIMessages.Button_Edit);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.widthHint = DefaultDialog.computeButtonWidth(editButton);
		editButton.setLayoutData(data);
		editButton.setEnabled(false);
		editButton.addListener(SWT.Selection, e -> SVNTeamCommentTemplatesPreferencesPage.this.editTemplate());

		removeButton = new Button(buttons, SWT.PUSH);
		removeButton.setText(SVNUIMessages.Button_Remove);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.widthHint = DefaultDialog.computeButtonWidth(removeButton);
		removeButton.setLayoutData(data);
		removeButton.setEnabled(false);
		removeButton.addListener(SWT.Selection, e -> SVNTeamCommentTemplatesPreferencesPage.this.remove());
	}

	protected void newTemplate() {
		EditCommentTemplatePanel panel = new EditCommentTemplatePanel(null);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == 0) {
			listViewer.add(panel.getTemplate());
		}
	}

	protected void editTemplate() {
		IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
		if (selection.size() == 1) {
			String oldTemplate = (String) selection.getFirstElement();
			EditCommentTemplatePanel panel = new EditCommentTemplatePanel(oldTemplate);
			DefaultDialog dialog = new DefaultDialog(getShell(), panel);
			if (dialog.open() == 0) {
				listViewer.remove(oldTemplate);
				listViewer.add(panel.getTemplate());
			}
		}
	}

	protected void remove() {
		IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
		listViewer.remove(selection.toArray());
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		userTemplatesEnabled = useTemplatesButton.getSelection();
		listViewer.getControl().setEnabled(userTemplatesEnabled);
		newButton.setEnabled(userTemplatesEnabled);
		previewText.setEnabled(userTemplatesEnabled);

		IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
		editButton.setEnabled(userTemplatesEnabled && selection.size() == 1);
		removeButton.setEnabled(userTemplatesEnabled && selection.size() > 0);
		previewText.setText(selection.size() == 1 ? (String) selection.getFirstElement() : ""); //$NON-NLS-1$
	}

}
