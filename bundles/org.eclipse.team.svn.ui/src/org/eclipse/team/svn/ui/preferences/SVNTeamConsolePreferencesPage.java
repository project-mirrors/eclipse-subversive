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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.preferences;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IntegerFieldVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.ui.PlatformUI;

/**
 * Console preferences page
 * 
 * @author Alexander Gurov
 */
public class SVNTeamConsolePreferencesPage extends AbstractSVNTeamPreferencesPage {
	protected ColorFieldEditor cmdEditor;

	protected ColorFieldEditor okEditor;

	protected ColorFieldEditor wrnEditor;

	protected ColorFieldEditor errEditor;

	protected boolean hyperlinksEnabled;

	protected int autoshow;

	protected boolean wrapEnabled;

	protected boolean limitEnabled;

	protected int wrapWidth;

	protected int limitValue;

	protected Button hyperlinksEnabledButton;

	protected Button showNeverButton;

	protected Button showAlwaysButton;

	protected Button showErrorButton;

	protected Button showWarningErrorButton;

	protected Button wrapEnabledButton;

	protected Button limitEnabledButton;

	protected Text wrapWidthText;

	protected Text limitValueText;

	public SVNTeamConsolePreferencesPage() {
	}

	@Override
	protected void saveValues(IPreferenceStore store) {
		cmdEditor.store();
		okEditor.store();
		wrnEditor.store();
		errEditor.store();

		SVNTeamPreferences.setConsoleInt(store, SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_NAME, autoshow);

		SVNTeamPreferences.setConsoleBoolean(store, SVNTeamPreferences.CONSOLE_HYPERLINKS_ENABLED_NAME,
				hyperlinksEnabled);

		SVNTeamPreferences.setConsoleBoolean(store, SVNTeamPreferences.CONSOLE_WRAP_ENABLED_NAME, wrapEnabled);
		SVNTeamPreferences.setConsoleInt(store, SVNTeamPreferences.CONSOLE_WRAP_WIDTH_NAME, wrapWidth);

		SVNTeamPreferences.setConsoleBoolean(store, SVNTeamPreferences.CONSOLE_LIMIT_ENABLED_NAME, limitEnabled);
		SVNTeamPreferences.setConsoleInt(store, SVNTeamPreferences.CONSOLE_LIMIT_VALUE_NAME, limitValue);
	}

	@Override
	protected void loadDefaultValues(IPreferenceStore store) {
		autoshow = SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_DEFAULT;
		hyperlinksEnabled = SVNTeamPreferences.CONSOLE_HYPERLINKS_ENABLED_DEFAULT;
		wrapEnabled = SVNTeamPreferences.CONSOLE_WRAP_ENABLED_DEFAULT;
		wrapWidth = SVNTeamPreferences.CONSOLE_WRAP_WIDTH_DEFAULT;
		limitEnabled = SVNTeamPreferences.CONSOLE_LIMIT_ENABLED_DEFAULT;
		limitValue = SVNTeamPreferences.CONSOLE_LIMIT_VALUE_DEFAULT;

		cmdEditor.getColorSelector().setColorValue(SVNTeamPreferences.CONSOLE_CMD_COLOR_DEFAULT);
		okEditor.getColorSelector().setColorValue(SVNTeamPreferences.CONSOLE_OK_COLOR_DEFAULT);
		wrnEditor.getColorSelector().setColorValue(SVNTeamPreferences.CONSOLE_WRN_COLOR_DEFAULT);
		errEditor.getColorSelector().setColorValue(SVNTeamPreferences.CONSOLE_ERR_COLOR_DEFAULT);
	}

	@Override
	protected void loadValues(IPreferenceStore store) {
		autoshow = SVNTeamPreferences.getConsoleInt(store, SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_NAME);
		hyperlinksEnabled = SVNTeamPreferences.getConsoleBoolean(store,
				SVNTeamPreferences.CONSOLE_HYPERLINKS_ENABLED_NAME);
		wrapEnabled = SVNTeamPreferences.getConsoleBoolean(store, SVNTeamPreferences.CONSOLE_WRAP_ENABLED_NAME);
		wrapWidth = SVNTeamPreferences.getConsoleInt(store, SVNTeamPreferences.CONSOLE_WRAP_WIDTH_NAME);
		limitEnabled = SVNTeamPreferences.getConsoleBoolean(store, SVNTeamPreferences.CONSOLE_LIMIT_ENABLED_NAME);
		limitValue = SVNTeamPreferences.getConsoleInt(store, SVNTeamPreferences.CONSOLE_LIMIT_VALUE_NAME);

		cmdEditor.load();
		okEditor.load();
		wrnEditor.load();
		errEditor.load();
	}

	@Override
	protected void initializeControls() {
		showNeverButton.setSelection(false);
		showAlwaysButton.setSelection(false);
		showErrorButton.setSelection(false);
		if (autoshow == SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_NEVER) {
			showNeverButton.setSelection(true);
		} else if (autoshow == SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_ALWAYS) {
			showAlwaysButton.setSelection(true);
		} else if (autoshow == SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_ERROR) {
			showErrorButton.setSelection(true);
		} else {
			showWarningErrorButton.setSelection(true);
		}

		hyperlinksEnabledButton.setSelection(hyperlinksEnabled);

		wrapEnabledButton.setSelection(wrapEnabled);
		wrapWidthText.setEnabled(wrapEnabled);
		wrapWidthText.setText(String.valueOf(wrapWidth));

		limitEnabledButton.setSelection(limitEnabled);
		limitValueText.setEnabled(limitEnabled);
		limitValueText.setText(String.valueOf(limitValue));
	}

	@Override
	protected Control createContentsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.FILL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = false;
		composite.setLayoutData(data);

		hyperlinksEnabledButton = new Button(composite, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 2;
		hyperlinksEnabledButton.setLayoutData(data);
		hyperlinksEnabledButton.setText(SVNUIMessages.ConsolePreferencePage_hyperlinksIsEnabled);
		hyperlinksEnabledButton.addListener(SWT.Selection, event -> hyperlinksEnabled = hyperlinksEnabledButton.getSelection());

		Group showType = new Group(composite, SWT.FILL);
		showType.setText(SVNUIMessages.ConsolePreferencePage_textShowOnGroup);
		layout = new GridLayout();
		layout.numColumns = 4;
		layout.horizontalSpacing = 10;
		showType.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.grabExcessVerticalSpace = false;
		showType.setLayoutData(data);

		showNeverButton = new Button(showType, SWT.RADIO);
		data = new GridData();
		showNeverButton.setLayoutData(data);
		showNeverButton.setText(SVNUIMessages.ConsolePreferencePage_textShowNever);
		showNeverButton.addListener(SWT.Selection, event -> {
			if (showNeverButton.getSelection()) {
				autoshow = SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_NEVER;
			}
		});

		showAlwaysButton = new Button(showType, SWT.RADIO);
		data = new GridData();
		showAlwaysButton.setLayoutData(data);
		showAlwaysButton.setText(SVNUIMessages.ConsolePreferencePage_textShowAlways);
		showAlwaysButton.addListener(SWT.Selection, event -> {
			if (showAlwaysButton.getSelection()) {
				autoshow = SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_ALWAYS;
			}
		});

		showErrorButton = new Button(showType, SWT.RADIO);
		data = new GridData();
		showErrorButton.setLayoutData(data);
		showErrorButton.setText(SVNUIMessages.ConsolePreferencePage_textShowError);
		showErrorButton.addListener(SWT.Selection, event -> {
			if (showErrorButton.getSelection()) {
				autoshow = SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_ERROR;
			}
		});

		showWarningErrorButton = new Button(showType, SWT.RADIO);
		data = new GridData();
		showWarningErrorButton.setLayoutData(data);
		showWarningErrorButton.setText(SVNUIMessages.ConsolePreferencePage_textShowWarningError);
		showWarningErrorButton.addListener(SWT.Selection, event -> {
			if (showWarningErrorButton.getSelection()) {
				autoshow = SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_WARNING_ERROR;
			}
		});

		Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		separator.setLayoutData(data);
		separator.setVisible(false);

		wrapEnabledButton = new Button(composite, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 2;
		wrapEnabledButton.setLayoutData(data);
		wrapEnabledButton.setText(SVNUIMessages.ConsolePreferencePage_textWrapEnabled);
		wrapEnabledButton.addListener(SWT.Selection, event -> {
			wrapEnabled = wrapEnabledButton.getSelection();
			wrapWidthText.setEnabled(wrapEnabled);
		});

		Label label = new Label(composite, SWT.NULL);
		data = new GridData();
		label.setLayoutData(data);
		String labelText = SVNUIMessages.ConsolePreferencePage_textWrapWidth;
		label.setText(labelText);

		wrapWidthText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		wrapWidthText.setLayoutData(data);
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(labelText));
		verifier.add(new IntegerFieldVerifier(labelText, true));
		attachTo(wrapWidthText, verifier);
		wrapWidthText.addModifyListener(e -> {
			try {
				wrapWidth = Integer.parseInt(wrapWidthText.getText());
			} catch (Exception ex) {
			}
		});

		limitEnabledButton = new Button(composite, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 2;
		limitEnabledButton.setLayoutData(data);
		limitEnabledButton.setText(SVNUIMessages.ConsolePreferencePage_textLimitEnabled);
		limitEnabledButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				limitEnabled = limitEnabledButton.getSelection();
				limitValueText.setEnabled(limitEnabled);
			}
		});

		label = new Label(composite, SWT.NULL);
		data = new GridData();
		label.setLayoutData(data);
		labelText = SVNUIMessages.ConsolePreferencePage_textLimitValue;
		label.setText(labelText);

		limitValueText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		limitValueText.setLayoutData(data);
		verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(labelText));
		verifier.add(new IntegerFieldVerifier(labelText, true));
		attachTo(limitValueText, verifier);
		limitValueText.addModifyListener(e -> {
			try {
				limitValue = Integer.parseInt(limitValueText.getText());
			} catch (Exception ex) {
			}
		});

		separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		separator.setLayoutData(data);
		separator.setVisible(false);

		label = new Label(composite, SWT.NULL);
		data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.ConsolePreferencePage_textColorsGroup);

		cmdEditor = new ColorFieldEditor(
				SVNTeamPreferences.fullConsoleName(SVNTeamPreferences.CONSOLE_CMD_COLOR_NAME),
				SVNUIMessages.ConsolePreferencePage_textCmdMessage, composite);
		cmdEditor.setPage(this);
		cmdEditor.setPreferenceStore(getPreferenceStore());

		okEditor = new ColorFieldEditor(
				SVNTeamPreferences.fullConsoleName(SVNTeamPreferences.CONSOLE_OK_COLOR_NAME),
				SVNUIMessages.ConsolePreferencePage_textOkMessage, composite);
		okEditor.setPage(this);
		okEditor.setPreferenceStore(getPreferenceStore());

		wrnEditor = new ColorFieldEditor(
				SVNTeamPreferences.fullConsoleName(SVNTeamPreferences.CONSOLE_WRN_COLOR_NAME),
				SVNUIMessages.ConsolePreferencePage_textWrnMessage, composite);
		wrnEditor.setPage(this);
		wrnEditor.setPreferenceStore(getPreferenceStore());

		errEditor = new ColorFieldEditor(
				SVNTeamPreferences.fullConsoleName(SVNTeamPreferences.CONSOLE_ERR_COLOR_NAME),
				SVNUIMessages.ConsolePreferencePage_textErrMessage, composite);
		errEditor.setPage(this);
		errEditor.setPreferenceStore(getPreferenceStore());

//		Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(parent, "org.eclipse.team.svn.help.consolePreferencesContext"); //$NON-NLS-1$

		return composite;
	}

}
