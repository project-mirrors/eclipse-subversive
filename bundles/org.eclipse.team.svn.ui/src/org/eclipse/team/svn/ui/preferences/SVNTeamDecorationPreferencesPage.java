/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - Initial API and implementation
 *    Alexander Gurov - adaptation for Subversive
 *    Dann Martens - [patch] Text decorations 'ascendant' variable, More decoration options
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.compare.internal.TabFolderLayout;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.decorator.DecoratorVariables;
import org.eclipse.team.svn.ui.decorator.IVariable;
import org.eclipse.team.svn.ui.decorator.IVariableContentProvider;
import org.eclipse.team.svn.ui.decorator.TextVariableSetProvider;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.ListSelectionPanel;
import org.eclipse.team.svn.ui.utility.DateFormatter;
import org.eclipse.team.svn.ui.utility.OverlayedImageDescriptor;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * Resource decoration preferences page implementation
 * 
 * @author Alexander Gurov
 */
public class SVNTeamDecorationPreferencesPage extends AbstractSVNTeamPreferencesPage
		implements org.eclipse.jface.util.IPropertyChangeListener {
	protected Button useFontsButton;

	protected boolean useFontsDecor;

	protected Button indicateConflictedButton;

	protected Button indicateModifiedButton;

	protected Button indicateRemoteButton;

	protected Button indicateAddedButton;

	protected Button indicateNewButton;

	protected Button indicateLockedButton;

	protected Button indicateNeedsLockButton;

	protected Button indicateSwitchedButton;

	protected boolean indicateConflicted;

	protected boolean indicateModified;

	protected boolean indicateRemote;

	protected boolean indicateAdded;

	protected boolean indicateNew;

	protected boolean indicateLocked;

	protected boolean indicateNeedsLock;

	protected boolean indicateSwitched;

	protected Text fileFormatField;

	protected Text folderFormatField;

	protected Text projectFormatField;

	protected String fileFormat;

	protected String folderFormat;

	protected String projectFormat;

	protected Preview preview;

	protected Text outgoingCharsField;

	protected Text addedCharsField;

	protected String outgoingChars;

	protected String addedChars;

	protected Text trunkPrefixField;

	protected Text branchPrefixField;

	protected Text tagPrefixField;

	protected String trunkPrefix;

	protected String branchPrefix;

	protected String tagPrefix;

	protected static final Collection<PreviewFile> ROOT;
	static {
		//name, type, added, new, dirty, ignored, hasRemote, locked
		PreviewFile branchProject = new PreviewFile("ProjectBranch", IResource.PROJECT, false, false, false, false, //$NON-NLS-1$
				false, true, false, false, false, true, false, false);
		PreviewFile tagProject = new PreviewFile("ProjectTag", IResource.PROJECT, false, false, false, false, false, //$NON-NLS-1$
				true, false, false, false, false, true, false);

		PreviewFile project = new PreviewFile("Project", IResource.PROJECT, false, false, true, false, false, true, //$NON-NLS-1$
				false, false, true, false, false, false);

		PreviewFile modifiedFolder = new PreviewFile("folder", IResource.FOLDER, false, false, true, false, false, true, //$NON-NLS-1$
				false, false, true, false, false, false);
		ArrayList<PreviewFile> children = new ArrayList<>();
		children.add(new PreviewFile("switched", IResource.FOLDER, false, false, false, false, false, true, false, //$NON-NLS-1$
				false, true, false, false, true));
		children.add(new PreviewFile("normal.txt", IResource.FILE, false, false, false, false, false, true, false, //$NON-NLS-1$
				false, true, false, false, false));
		children.add(new PreviewFile("modified.cpp", IResource.FILE, false, false, true, false, false, true, false, //$NON-NLS-1$
				false, true, false, false, false));
		children.add(new PreviewFile("conflicting.cpp", IResource.FILE, false, false, true, true, false, true, false, //$NON-NLS-1$
				false, true, false, false, false));
		children.add(new PreviewFile("ignored.txt", IResource.FILE, false, false, false, false, true, false, false, //$NON-NLS-1$
				false, true, false, false, false));
		modifiedFolder.children = children;

		children = new ArrayList<>();
		children.add(modifiedFolder);
		children.add(new PreviewFile("new", IResource.FILE, false, true, false, false, false, false, false, false, true, //$NON-NLS-1$
				false, false, false));
		children.add(new PreviewFile("added.java", IResource.FILE, true, false, true, false, false, false, false, false, //$NON-NLS-1$
				true, false, false, false));
		children.add(new PreviewFile("locked", IResource.FILE, false, false, false, false, false, true, true, false, //$NON-NLS-1$
				true, false, false, false));
		children.add(new PreviewFile("needsLock", IResource.FILE, false, false, false, false, false, true, false, true, //$NON-NLS-1$
				true, false, false, false));

		project.children = children;
		ROOT = new ArrayList<>();
		ROOT.add(project);
		ROOT.add(branchProject);
		ROOT.add(tagProject);
	}

	public SVNTeamDecorationPreferencesPage() {
		getPreferenceStore().addPropertyChangeListener(this);
	}

	@Override
	public void dispose() {
		super.dispose();
		getPreferenceStore().removePropertyChangeListener(this);
	}

	@Override
	protected void saveValues(IPreferenceStore store) {
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_FILE_NAME, fileFormat);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_FOLDER_NAME, folderFormat);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_PROJECT_NAME, projectFormat);

		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_OUTGOING_NAME, outgoingChars);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_ADDED_NAME, addedChars);

		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_TRUNK_PREFIX_NAME, trunkPrefix);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_BRANCH_PREFIX_NAME, branchPrefix);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_TAG_PREFIX_NAME, tagPrefix);

		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_CONFLICTED_NAME,
				indicateConflicted);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_MODIFIED_NAME,
				indicateModified);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_REMOTE_NAME, indicateRemote);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_ADDED_NAME, indicateAdded);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_NEW_NAME, indicateNew);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_LOCKED_NAME, indicateLocked);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_NEEDS_LOCK_NAME,
				indicateNeedsLock);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_SWITCHED_NAME,
				indicateSwitched);

		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_USE_FONT_COLORS_DECOR_NAME,
				useFontsDecor);
	}

	@Override
	protected void loadDefaultValues(IPreferenceStore store) {
		fileFormat = SVNTeamPreferences.DECORATION_FORMAT_FILE_DEFAULT;
		folderFormat = SVNTeamPreferences.DECORATION_FORMAT_FOLDER_DEFAULT;
		projectFormat = SVNTeamPreferences.DECORATION_FORMAT_PROJECT_DEFAULT;

		outgoingChars = SVNTeamPreferences.DECORATION_FLAG_OUTGOING_DEFAULT;
		addedChars = SVNTeamPreferences.DECORATION_FLAG_ADDED_DEFAULT;

		trunkPrefix = SVNTeamPreferences.DECORATION_TRUNK_PREFIX_DEFAULT;
		branchPrefix = SVNTeamPreferences.DECORATION_BRANCH_PREFIX_DEFAULT;
		tagPrefix = SVNTeamPreferences.DECORATION_TAG_PREFIX_DEFAULT;

		indicateConflicted = SVNTeamPreferences.DECORATION_ICON_CONFLICTED_DEFAULT;
		indicateModified = SVNTeamPreferences.DECORATION_ICON_MODIFIED_DEFAULT;
		indicateRemote = SVNTeamPreferences.DECORATION_ICON_REMOTE_DEFAULT;
		indicateAdded = SVNTeamPreferences.DECORATION_ICON_ADDED_DEFAULT;
		indicateNew = SVNTeamPreferences.DECORATION_ICON_NEW_DEFAULT;
		indicateLocked = SVNTeamPreferences.DECORATION_ICON_LOCKED_DEFAULT;
		indicateNeedsLock = SVNTeamPreferences.DECORATION_ICON_NEEDS_LOCK_DEFAULT;
		indicateSwitched = SVNTeamPreferences.DECORATION_ICON_SWITCHED_DEFAULT;

		useFontsDecor = SVNTeamPreferences.DECORATION_USE_FONT_COLORS_DECOR_DEFAULT;
	}

	@Override
	protected void loadValues(IPreferenceStore store) {
		fileFormat = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_FILE_NAME);
		folderFormat = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_FOLDER_NAME);
		projectFormat = SVNTeamPreferences.getDecorationString(store,
				SVNTeamPreferences.DECORATION_FORMAT_PROJECT_NAME);

		outgoingChars = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_OUTGOING_NAME);
		addedChars = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_ADDED_NAME);

		trunkPrefix = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_TRUNK_PREFIX_NAME);
		branchPrefix = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_BRANCH_PREFIX_NAME);
		tagPrefix = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_TAG_PREFIX_NAME);

		indicateConflicted = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_ICON_CONFLICTED_NAME);
		indicateModified = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_ICON_MODIFIED_NAME);
		indicateRemote = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_REMOTE_NAME);
		indicateAdded = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_ADDED_NAME);
		indicateNew = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_NEW_NAME);
		indicateLocked = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_LOCKED_NAME);
		indicateNeedsLock = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_ICON_NEEDS_LOCK_NAME);
		indicateSwitched = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_ICON_SWITCHED_NAME);

		useFontsDecor = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_USE_FONT_COLORS_DECOR_NAME);
	}

	@Override
	protected void initializeControls() {
		fileFormatField.setText(fileFormat);
		folderFormatField.setText(folderFormat);
		projectFormatField.setText(projectFormat);

		outgoingCharsField.setText(outgoingChars);
		addedCharsField.setText(addedChars);

		trunkPrefixField.setText(trunkPrefix);
		branchPrefixField.setText(branchPrefix);
		tagPrefixField.setText(tagPrefix);

		indicateConflictedButton.setSelection(indicateConflicted);
		indicateModifiedButton.setSelection(indicateModified);
		indicateRemoteButton.setSelection(indicateRemote);
		indicateAddedButton.setSelection(indicateAdded);
		indicateNewButton.setSelection(indicateNew);
		indicateLockedButton.setSelection(indicateLocked);
		indicateNeedsLockButton.setSelection(indicateNeedsLock);
		indicateSwitchedButton.setSelection(indicateSwitched);

		useFontsButton.setSelection(useFontsDecor);

		refreshPreview();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().startsWith(SVNTeamPreferences.DATE_FORMAT_BASE)) {
			refreshPreview();
		}
	}

	@Override
	protected Control createContentsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.FILL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayout(new TabFolderLayout());
		tabFolder.setLayoutData(data = new GridData(GridData.FILL_HORIZONTAL));
		data.heightHint = 200;

		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.PreferencePage_generalTabName);
		tabItem.setControl(createGeneralSettingsPage(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.PreferencePage_textTabName);
		tabItem.setControl(createTextSettingsPage(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.PreferencePage_iconsTabName);
		tabItem.setControl(createIconsSettingsPage(tabFolder));

		preview = new Preview(composite);

//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.decorsPreferencesContext"); //$NON-NLS-1$

		return composite;
	}

	protected Composite createTextSettingsPage(Composite parent) {
		GridLayout layout = null;
		GridData data = null;
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		data = new GridData();
		data.grabExcessVerticalSpace = false;
		composite.setLayoutData(data);

		Label label = new Label(composite, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.PreferencePage_textPrompt);

		Composite groups = new Composite(composite, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		groups.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		groups.setLayoutData(data);

		Group formatGroup = new Group(groups, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.verticalSpacing = 1;
		formatGroup.setLayout(layout);
		formatGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		formatGroup.setText(SVNUIMessages.PreferencePage_formatGroup);

		List<IVariable> fileOptions = new ArrayList<>();
		fileOptions.add(TextVariableSetProvider.VAR_OUTGOING_FLAG);
		fileOptions.add(TextVariableSetProvider.VAR_ADDED_FLAG);
		fileOptions.add(TextVariableSetProvider.VAR_NAME);
		fileOptions.add(TextVariableSetProvider.VAR_REVISION);
		fileOptions.add(TextVariableSetProvider.VAR_DATE);
		fileOptions.add(TextVariableSetProvider.VAR_AUTHOR);
		fileOptions.add(TextVariableSetProvider.VAR_RESOURCE_URL);
		fileFormatField = createFormatControl(formatGroup, "PreferencePage_textFileFormat", fileOptions, //$NON-NLS-1$
				Collections.emptyList());
		fileFormatField.addModifyListener(e -> fileFormat = fileFormatField.getText());

		List<IVariable> folderOptions = new ArrayList<>(fileOptions);
		folderFormatField = createFormatControl(formatGroup, "PreferencePage_textFolderFormat", folderOptions, //$NON-NLS-1$
				Collections.emptyList());
		folderFormatField.addModifyListener(e -> folderFormat = folderFormatField.getText());

		List<IVariable> projectOptions = new ArrayList<>();
		projectOptions.add(TextVariableSetProvider.VAR_OUTGOING_FLAG);
		projectOptions.add(TextVariableSetProvider.VAR_NAME);
		projectOptions.add(TextVariableSetProvider.VAR_REVISION);
		projectOptions.add(TextVariableSetProvider.VAR_LOCATION_LABEL);
		projectOptions.add(TextVariableSetProvider.VAR_LOCATION_URL);
		projectOptions.add(TextVariableSetProvider.VAR_ROOT_PREFIX); //5
		projectOptions.add(TextVariableSetProvider.VAR_ASCENDANT);
		projectOptions.add(TextVariableSetProvider.VAR_DESCENDANT);
		projectOptions.add(TextVariableSetProvider.VAR_FULLNAME);
		projectOptions.add(TextVariableSetProvider.VAR_FULLPATH);
		projectOptions.add(TextVariableSetProvider.VAR_RESOURCE_URL);
		projectOptions.add(TextVariableSetProvider.VAR_SHORT_RESOURCE_URL);
		projectOptions.add(TextVariableSetProvider.VAR_REMOTE_NAME);
		projectOptions.add(TextVariableSetProvider.VAR_DATE);
		projectOptions.add(TextVariableSetProvider.VAR_AUTHOR);

		projectFormatField = createFormatControl(formatGroup, "PreferencePage_textProjectFormat", //$NON-NLS-1$
				projectOptions, Collections.emptyList());
		projectFormatField.addModifyListener(e -> projectFormat = projectFormatField.getText());

		List<Object> grayedOptions = new ArrayList<>();
		grayedOptions.add(projectOptions.get(5));

		Group prefixGroup = new Group(groups, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.verticalSpacing = 1;
		prefixGroup.setLayout(layout);
		prefixGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		prefixGroup.setText(SVNUIMessages.PreferencePage_rootPrefixGroup);

		trunkPrefixField = createFormatControl(prefixGroup, "PreferencePage_textTrunkPrefix", projectOptions, //$NON-NLS-1$
				grayedOptions);
		trunkPrefixField.addModifyListener(e -> trunkPrefix = trunkPrefixField.getText());

		branchPrefixField = createFormatControl(prefixGroup, "PreferencePage_textBranchPrefix", //$NON-NLS-1$
				projectOptions, grayedOptions);
		branchPrefixField.addModifyListener(e -> branchPrefix = branchPrefixField.getText());

		tagPrefixField = createFormatControl(prefixGroup, "PreferencePage_textTagPrefix", projectOptions, //$NON-NLS-1$
				grayedOptions);
		tagPrefixField.addModifyListener(e -> tagPrefix = tagPrefixField.getText());

		Composite outFlagComposite = new Composite(groups, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		outFlagComposite.setLayout(layout);
		outFlagComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(outFlagComposite, SWT.NONE);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.PreferencePage_textOutgoingFlag);

		outgoingCharsField = new Text(outFlagComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		outgoingCharsField.setLayoutData(data);
		outgoingCharsField.addModifyListener(e -> {
			outgoingChars = outgoingCharsField.getText();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		Composite addFlagComposite = new Composite(groups, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		addFlagComposite.setLayout(layout);
		addFlagComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(addFlagComposite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.PreferencePage_textAddedFlag);

		addedCharsField = new Text(addFlagComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		addedCharsField.setLayoutData(data);
		addedCharsField.addModifyListener(e -> {
			addedChars = addedCharsField.getText();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		return composite;
	}

	protected Text createFormatControl(Composite parent, String labelId, final List<IVariable> variables,
			final List<Object> grayedVariables) {
		Label label = new Label(parent, SWT.NULL);
		label.setLayoutData(new GridData());
		if (labelId != null) {
			label.setText(SVNUIMessages.getString(labelId));
		} else {
			label.setText(""); //$NON-NLS-1$
		}

		final Text format = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 100;
		data.grabExcessHorizontalSpace = true;
		format.setLayoutData(data);

		Button addVariables = new Button(parent, SWT.PUSH);
		addVariables.setText(SVNUIMessages.PreferencePage_textAddVariables);
		data = new GridData();
		data.widthHint = 25;
		addVariables.setLayoutData(data);
		addVariables.addListener(SWT.Selection, event -> SVNTeamDecorationPreferencesPage.this.variableConfigurationDialog(format, variables, grayedVariables));
		format.addModifyListener(e -> SVNTeamDecorationPreferencesPage.this.refreshPreview());
		return format;
	}

	protected void variableConfigurationDialog(Text field, List<IVariable> variableList,
			List<Object> grayedVariableList) {
		final IVariable[] variables = variableList.toArray(new IVariable[variableList.size()]);
		final IVariable[] grayedVariables = grayedVariableList.toArray(new IVariable[grayedVariableList.size()]);
		IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				return variables;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		};
		ILabelProvider labelProvider = new LabelProvider() {
			@Override
			public String getText(Object element) {
				IVariable var = (IVariable) element;
				return var.getName() + " - " + var.getDescription(); //$NON-NLS-1$
			}
		};
		String dialogPrompt = SVNUIMessages.PreferencePage_textAddVariablesPrompt;
		String dialogMessage = SVNUIMessages.PreferencePage_textAddVariablesMessage;

		DecoratorVariables decorator = new DecoratorVariables(TextVariableSetProvider.instance);
		IVariable[] realVars = decorator.parseFormatLine(field.getText());

		ListSelectionPanel panel = new ListSelectionPanel(this, contentProvider, labelProvider, dialogPrompt,
				dialogMessage);
		panel.setInitialSelections(realVars);
		panel.setInitialGrayed(grayedVariables);
		if (new DefaultDialog(getShell(), panel).open() == 0) {
			List<Object> result = new ArrayList<>();
			List<Object> newSelection = Arrays.asList(panel.getResultSelections());
			for (IVariable realVar : realVars) {
				if (TextVariableSetProvider.instance.getVariable(realVar.getName()) == null) {
					result.add(realVar);
				} else if (newSelection.contains(realVar)) {
					result.add(realVar);
				}
			}
			for (int i = 0; i < newSelection.size(); i++) {
				if (!result.contains(newSelection.get(i))) {
					result.add(newSelection.get(i));
				}
			}
			realVars = result.toArray(new IVariable[result.size()]);

			field.setText(DecoratorVariables.prepareFormatLine(realVars));
		}
	}

	protected Composite createIconsSettingsPage(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(composite, SWT.NULL);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.PreferencePage_iconsPrompt);

		indicateModifiedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indicateModifiedButton.setLayoutData(data);
		indicateModifiedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateModified);
		indicateModifiedButton.addListener(SWT.Selection, event -> {
			indicateModified = indicateModifiedButton.getSelection();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		indicateConflictedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indicateConflictedButton.setLayoutData(data);
		indicateConflictedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateConflicted);
		indicateConflictedButton.addListener(SWT.Selection, event -> {
			indicateConflicted = indicateConflictedButton.getSelection();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		indicateRemoteButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indicateRemoteButton.setLayoutData(data);
		indicateRemoteButton.setText(SVNUIMessages.PreferencePage_iconsIndicateRemote);
		indicateRemoteButton.addListener(SWT.Selection, event -> {
			indicateRemote = indicateRemoteButton.getSelection();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		indicateLockedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indicateLockedButton.setLayoutData(data);
		indicateLockedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateLocked);
		indicateLockedButton.addListener(SWT.Selection, event -> {
			indicateLocked = indicateLockedButton.getSelection();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		indicateAddedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indicateAddedButton.setLayoutData(data);
		indicateAddedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateAdded);
		indicateAddedButton.addListener(SWT.Selection, event -> {
			indicateAdded = indicateAddedButton.getSelection();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		indicateNeedsLockButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indicateNeedsLockButton.setLayoutData(data);
		indicateNeedsLockButton.setText(SVNUIMessages.PreferencePage_iconsIndicateNeedsLock);
		indicateNeedsLockButton.addListener(SWT.Selection, event -> {
			indicateNeedsLock = indicateNeedsLockButton.getSelection();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		indicateNewButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indicateNewButton.setLayoutData(data);
		indicateNewButton.setText(SVNUIMessages.PreferencePage_iconsIndicateNew);
		indicateNewButton.addListener(SWT.Selection, event -> {
			indicateNew = indicateNewButton.getSelection();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		indicateSwitchedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indicateSwitchedButton.setLayoutData(data);
		indicateSwitchedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateSwitched);
		indicateSwitchedButton.addListener(SWT.Selection, event -> {
			indicateSwitched = indicateSwitchedButton.getSelection();
			SVNTeamDecorationPreferencesPage.this.refreshPreview();
		});

		return composite;
	}

	protected Composite createGeneralSettingsPage(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite noteComposite = new Composite(composite, SWT.FILL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		noteComposite.setLayout(layout);
		noteComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label noteLabel = new Label(noteComposite, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		data.heightHint = convertHeightInCharsToPixels(4);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		noteLabel.setLayoutData(data);
		noteLabel.setText(SVNUIMessages.PreferencePage_noteLabel);

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		new PreferenceLinkArea(composite, SWT.NONE, SVNUIMessages.PreferencePage_generalUseLabels_1,
				SVNUIMessages.PreferencePage_generalUseLabels_2, (IWorkbenchPreferenceContainer) getContainer(), null);

		useFontsButton = new Button(composite, SWT.CHECK);
		useFontsButton.setLayoutData(new GridData());
		useFontsButton.setText(SVNUIMessages.PreferencePage_generalUseFonts_2);
		new PreferenceLinkArea(composite, SWT.NONE, SVNUIMessages.PreferencePage_generalUseFonts_1,
				SVNUIMessages.PreferencePage_generalUseFonts_3, (IWorkbenchPreferenceContainer) getContainer(), null);
		useFontsButton.addListener(SWT.Selection, event -> useFontsDecor = useFontsButton.getSelection());

		return composite;
	}

	protected ImageDescriptor getOverlay(PreviewFile element) {
		if (element.ignored) {
			return null;
		} else if (element.locked && indicateLocked) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/lock.gif"); //$NON-NLS-1$
		} else if (element.isSwitched && indicateSwitched) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/switched.gif"); //$NON-NLS-1$
		} else if (element.needsLock && indicateNeedsLock) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/needs_lock.gif"); //$NON-NLS-1$
		} else if (element.newResource && indicateNew) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/new_resource.gif"); //$NON-NLS-1$
		} else if (element.added && indicateAdded) {
			return TeamImages.getImageDescriptor(ISharedImages.IMG_HOURGLASS_OVR);
		} else if (element.conflicted && indicateConflicted) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/conflicted_unresolved.gif"); //$NON-NLS-1$
		} else if (element.dirty && indicateModified) {
			return TeamImages.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR);
		} else if (element.hasRemote && indicateRemote) {
			return TeamImages.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR);
		}
		return null;
	}

	protected void refreshPreview() {
		if (preview != null) {
			preview.refresh();
		}
	}

	protected class DemoDecoration {

		protected String fullName;

		public DemoDecoration(String baseName) {
			fullName = baseName;
		}

		public String getFullName() {
			return fullName;
		}

		public void addPrefix(String prefix) {
			fullName = prefix + fullName;
		}

		public void addSuffix(String suffix) {
			fullName += suffix;
		}

		public void addOverlay(ImageDescriptor overlay) {

		}

		public void addOverlay(ImageDescriptor overlay, int quadrant) {

		}

		public void setFont(Font font) {

		}

		public void setForegroundColor(Color color) {

		}

		public void setBackgroundColor(Color color) {

		}

	}

	protected class DemoVariableContentProvider implements IVariableContentProvider {
		protected PreviewFile preview;

		protected String demoRevision;

		public DemoVariableContentProvider(PreviewFile preview, String demoRevision) {
			this.preview = preview;
			this.demoRevision = demoRevision;
		}

		@Override
		public String getValue(IVariable var) {
			if (var.equals(TextVariableSetProvider.VAR_ADDED_FLAG)) {
				if (preview.added) {
					return addedChars;
				}
				return ""; //$NON-NLS-1$
			} else if (var.equals(TextVariableSetProvider.VAR_OUTGOING_FLAG)) {
				if (preview.dirty) {
					return outgoingChars;
				}
				return ""; //$NON-NLS-1$
			} else if (var.equals(TextVariableSetProvider.VAR_ROOT_PREFIX)) {
				if (preview.isTag) {
					return tagPrefix;
				} else if (preview.isBranch) {
					return branchPrefix;
				} else if (preview.isTrunk) {
					return trunkPrefix;
				}
				return ""; //$NON-NLS-1$
			} else if (var.equals(TextVariableSetProvider.VAR_ASCENDANT)) {
				return SVNUIMessages.PreferencePage_demoAscendant;
			} else if (var.equals(TextVariableSetProvider.VAR_DESCENDANT)) {
				return SVNUIMessages.PreferencePage_demoDescendant;
			} else if (var.equals(TextVariableSetProvider.VAR_FULLNAME)) {
				return SVNUIMessages.PreferencePage_demoFullname;
			} else if (var.equals(TextVariableSetProvider.VAR_FULLPATH)) {
				return SVNUIMessages.PreferencePage_demoFullpath;
			} else if (var.equals(TextVariableSetProvider.VAR_AUTHOR)) {
				return SVNUIMessages.PreferencePage_demoAuthor;
			} else if (var.equals(TextVariableSetProvider.VAR_NAME)) {
				return preview.name;
			} else if (var.equals(TextVariableSetProvider.VAR_LOCATION_URL)) {
				return SVNUIMessages.PreferencePage_demoLocationURL;
			} else if (var.equals(TextVariableSetProvider.VAR_LOCATION_LABEL)) {
				return SVNUIMessages.PreferencePage_demoLocationLabel;
			} else if (var.equals(TextVariableSetProvider.VAR_RESOURCE_URL)) {
				return SVNUIMessages.PreferencePage_demoResourceURL;
			} else if (var.equals(TextVariableSetProvider.VAR_SHORT_RESOURCE_URL)) {
				return SVNUIMessages.PreferencePage_demoShortURL;
			} else if (var.equals(TextVariableSetProvider.VAR_REMOTE_NAME)) {
				return SVNUIMessages.PreferencePage_demoRemoteName;
			} else if (var.equals(TextVariableSetProvider.VAR_DATE)) {
				return DateFormatter.formatDate(new Date());
			} else if (var.equals(TextVariableSetProvider.VAR_REVISION)) {
				return demoRevision;
			}
			return var.toString();
		}

	}

	protected class Preview extends LabelProvider implements Observer, ITreeContentProvider {

		protected Map<ImageDescriptor, Image> images;

		private final TreeViewer fViewer;

		protected DecoratorVariables decoratorVariables;

		public Preview(Composite parent) {
			decoratorVariables = new DecoratorVariables(TextVariableSetProvider.instance);
			images = new HashMap<>();
			Composite composite = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 0;
			composite.setLayout(layout);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.grabExcessVerticalSpace = true;
			composite.setLayoutData(data);
			Label label = new Label(composite, SWT.NULL);
			label.setLayoutData(new GridData());
			label.setText(SVNUIMessages.PreferencePage_preview);
			fViewer = new TreeViewer(composite);
			data = new GridData(GridData.FILL_BOTH);
			data.heightHint = Math.max(convertHeightInCharsToPixels(1), 16) * 13;
			data.grabExcessVerticalSpace = true;
			fViewer.getControl().setLayoutData(data);
			fViewer.setContentProvider(this);
			fViewer.setLabelProvider(this);
			fViewer.setInput(ROOT);
			fViewer.expandAll();
			fViewer.setSelection(new StructuredSelection(ROOT.iterator().next()));
			fViewer.getTree().showSelection();
			fViewer.setSelection(null);
		}

		public void refresh() {
			fViewer.refresh(true);
		}

		@Override
		public void update(Observable o, Object arg) {
			refresh();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return ((PreviewFile) parentElement).children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return !((PreviewFile) element).children.isEmpty();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((Collection<?>) inputElement).toArray();
		}

		@Override
		public void dispose() {
			for (Image image : images.values()) {
				image.dispose();
			}
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public String getText(Object element) {
			IVariableContentProvider provider = null;
			IVariable[] realVars = null;
			PreviewFile previewFile = (PreviewFile) element;
			switch (previewFile.type) {
				case IResource.PROJECT:
					provider = new DemoVariableContentProvider(previewFile,
							SVNUIMessages.PreferencePage_demoProjectRevision);
					realVars = decoratorVariables.parseFormatLine(projectFormatField.getText());
					break;
				case IResource.FOLDER:
					provider = new DemoVariableContentProvider(previewFile,
							SVNUIMessages.PreferencePage_demoFolderRevision);
					realVars = decoratorVariables.parseFormatLine(folderFormatField.getText());
					break;
				default:
					provider = new DemoVariableContentProvider(previewFile,
							SVNUIMessages.PreferencePage_demoFileRevision);
					realVars = decoratorVariables.parseFormatLine(fileFormatField.getText());
					break;
			}
			DemoDecoration decoration = new DemoDecoration(previewFile.name);
			decorateText(decoration, realVars, provider);
			return decoration.getFullName();
		}

		private String getValue(IVariable var, IVariableContentProvider provider) {
			return amend(var, provider);
		}

		/**
		 * Helper method which recurses through variables in variables, first order only.
		 * 
		 * @param var
		 *            A variable wrapper.
		 * @param provider
		 *            A <code>IVariableContentProvider</code>
		 * @return The amended value of this variable.
		 */
		private String amend(IVariable var, IVariableContentProvider provider) {
			IVariable[] variables = decoratorVariables.parseFormatLine(provider.getValue(var));
			String value = ""; //$NON-NLS-1$
			for (IVariable variable : variables) {
				String variableValue = provider.getValue(variable);
				if (!variable.equals(var)) {
					value += variableValue;
				} else if (variableValue.equals(variable.getName())) {
					value += variableValue;
				} else {
					value += "?{" + variable.getName() + "}?"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			return value;
		}

		public void decorateText(DemoDecoration decoration, IVariable[] format, IVariableContentProvider provider) {
			int centerPoint = Arrays.asList(format).indexOf(TextVariableSetProvider.instance.getCenterVariable());
			String prefix = ""; //$NON-NLS-1$
			String suffix = ""; //$NON-NLS-1$
			for (int i = 0; i < format.length; i++) {
				if (!format[i].equals(TextVariableSetProvider.instance.getCenterVariable())) {
					if (centerPoint != -1 && i < centerPoint) {
						prefix += getValue(format[i], provider);
					} else {
						suffix += getValue(format[i], provider);
					}
				}
			}
			decoration.addPrefix(prefix);
			decoration.addSuffix(suffix);
		}

		@Override
		public Image getImage(Object element) {
			ImageDescriptor descriptor = null;

			switch (((PreviewFile) element).type) {
				case IResource.PROJECT:
					descriptor = SVNTeamUIPlugin.instance().getImageDescriptor("icons/objects/project.gif"); //$NON-NLS-1$
					break;
				case IResource.FOLDER:
					descriptor = SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/folder.gif"); //$NON-NLS-1$
					break;
				default:
					descriptor = SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/file.gif"); //$NON-NLS-1$
					break;
			}
			Image image = images.get(descriptor);
			if (image == null) {
				images.put(descriptor, image = descriptor.createImage());
			}

			ImageDescriptor overlay = getOverlay((PreviewFile) element);
			if (overlay == null) {
				return image;
			}
			try {
				ImageDescriptor imgDescr = new OverlayedImageDescriptor(image, overlay,
						new Point(image.getBounds().width, image.getBounds().height),
						OverlayedImageDescriptor.BOTTOM | OverlayedImageDescriptor.RIGHT);
				Image overlayedImg = images.get(imgDescr);
				if (overlayedImg == null) {
					overlayedImg = imgDescr.createImage();
					images.put(imgDescr, overlayedImg);
				}
				return overlayedImg;
			} catch (Exception e) {
				LoggedOperation.reportError(SVNUIMessages.Error_DecoratorImage, e);
			}
			return null;
		}
	}

	private static class PreviewFile {
		public final String name;

		public final int type;

		public final boolean added, dirty, conflicted, hasRemote, ignored, newResource, locked, needsLock, isTrunk,
				isBranch, isTag, isSwitched;

		public Collection<PreviewFile> children;

		public PreviewFile(String name, int type, boolean added, boolean newResource, boolean dirty, boolean conflicted,
				boolean ignored, boolean hasRemote, boolean locked, boolean needsLock, boolean isTrunk,
				boolean isBranch, boolean isTag, boolean isSwitched) {
			this.name = name;
			this.type = type;
			this.added = added;
			this.ignored = ignored;
			this.dirty = dirty;
			this.conflicted = conflicted;
			this.hasRemote = hasRemote;
			this.newResource = newResource;
			this.locked = locked;
			this.needsLock = needsLock;
			children = Collections.emptyList();
			this.isTrunk = isTrunk;
			this.isBranch = isBranch;
			this.isTag = isTag;
			this.isSwitched = isSwitched;
		}
	}

}
