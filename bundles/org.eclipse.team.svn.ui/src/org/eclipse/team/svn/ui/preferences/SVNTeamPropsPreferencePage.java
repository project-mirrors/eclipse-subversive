/*******************************************************************************
 * Copyright (c) 2005, 2024 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexey Mikoyan - Initial implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *    Nikifor Fedorov (ArSysOp) - issue subversive/#245
 *******************************************************************************/

package org.eclipse.team.svn.ui.preferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.properties.PredefinedProperty;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.internal.ui.TabFolderLayout;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.SpellcheckedTextProvider;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.common.EditAutoPropertiesPanel;
import org.eclipse.team.svn.ui.panel.common.EditCustomPropertiesPanel;
import org.eclipse.ui.PlatformUI;

/**
 * Auto-props preferences page
 *
 * @author Alexey Mikoyan
 * 
 * @since 0.7.0 - Properties configuration preference page
 */
public class SVNTeamPropsPreferencePage extends AbstractSVNTeamPreferencesPage {
	protected CheckboxTableViewer autopropTableViewer;

	protected Button autopropBtnAdd;

	protected Button autopropBtnEdit;

	protected Button autopropBtnRemove;

	protected Button autopropBtnExport;

	protected Button autopropBtnImport;

	protected String autoPropsValue;

	protected String customPropsValue;

	protected boolean forceTextMIME;

	protected boolean ignoreMaskValidation;

	protected Button forceTextMIMEButton;

	protected Button ignoreMaskValidationButton;

	protected TableViewer custompropTableViewer;

	protected Button custompropBtnAdd;

	protected Button custompropBtnEdit;

	protected Button custompropBtnRemove;

	protected StyledText customPropDescription;

	public static final int COLUMN_INDEX_FILE_NAME = 1;

	public static final int COLUMN_INDEX_PROPERTIES = 2;

	public static final String AUTO_PROPS_CONFIG_FILE_NAME = "config"; //$NON-NLS-1$

	public static final String AUTO_PROPS_SECTION_HEADER = "[auto-props]"; //$NON-NLS-1$

	public static final String AUTO_PROPS_COMMENT_START = "#"; //$NON-NLS-1$

	public static final String AUTO_PROPS_PATTERN_SEPARATOR = "="; //$NON-NLS-1$

	public static final String AUTO_PROPS_PROPS_SEPARATOR = ";"; //$NON-NLS-1$

	public static class AutoProperty {
		public String fileName;

		public String properties;

		public boolean enabled;

		public AutoProperty(String fileName, String properties, boolean enabled) {
			this.fileName = fileName;
			this.properties = properties;
			this.enabled = enabled;
		}
	}

	@Override
	protected void saveValues(IPreferenceStore store) {
		int propsCount = autopropTableViewer.getTable().getItemCount();
		String[] props = new String[3 * propsCount];
		for (int i = 0; i < propsCount; i++) {
			SVNTeamPropsPreferencePage.AutoProperty property = (SVNTeamPropsPreferencePage.AutoProperty) autopropTableViewer
					.getElementAt(i);
			props[3 * i] = property.enabled ? "1" : "0"; //$NON-NLS-1$ //$NON-NLS-2$
			props[3 * i + 1] = property.fileName;
			props[3 * i + 2] = property.properties;
		}
		autoPropsValue = FileUtility.encodeArrayToString(props);
		SVNTeamPreferences.setAutoPropertiesList(store, SVNTeamPreferences.AUTO_PROPERTIES_LIST_NAME, autoPropsValue);

		propsCount = custompropTableViewer.getTable().getItemCount();
		props = new String[3 * propsCount];
		for (int i = 0; i < propsCount; i++) {
			PredefinedProperty property = (PredefinedProperty) custompropTableViewer.getElementAt(i);
			props[3 * i] = property.name;
			props[3 * i + 1] = property.validationRegexp;
			props[3 * i + 2] = property.description;
		}
		customPropsValue = FileUtility.encodeArrayToString(props);
		SVNTeamPreferences.setCustomPropertiesList(store, SVNTeamPreferences.CUSTOM_PROPERTIES_LIST_NAME,
				customPropsValue);
		SVNTeamPreferences.setPropertiesBoolean(store, SVNTeamPreferences.FORCE_TEXT_MIME_NAME, forceTextMIME);
		SVNTeamPreferences.setPropertiesBoolean(store, SVNTeamPreferences.IGNORE_MASK_VALIDATION_ENABLED_NAME,
				ignoreMaskValidation);
	}

	@Override
	protected void loadDefaultValues(IPreferenceStore store) {
		autoPropsValue = SVNTeamPreferences.AUTO_PROPERTIES_LIST_DEFAULT;
		customPropsValue = SVNTeamPreferences.CUSTOM_PROPERTIES_LIST_DEFAULT;
		forceTextMIME = SVNTeamPreferences.FORCE_TEXT_MIME_DEFAULT;
		ignoreMaskValidation = SVNTeamPreferences.IGNORE_MASK_VALIDATION_ENABLED_DEFAULT;
	}

	@Override
	protected void loadValues(IPreferenceStore store) {
		autoPropsValue = SVNTeamPreferences.getAutoPropertiesList(store, SVNTeamPreferences.AUTO_PROPERTIES_LIST_NAME);
		customPropsValue = SVNTeamPreferences.getCustomPropertiesList(store,
				SVNTeamPreferences.CUSTOM_PROPERTIES_LIST_NAME);
		forceTextMIME = SVNTeamPreferences.getPropertiesBoolean(store, SVNTeamPreferences.FORCE_TEXT_MIME_NAME);
		ignoreMaskValidation = SVNTeamPreferences.getPropertiesBoolean(store,
				SVNTeamPreferences.IGNORE_MASK_VALIDATION_ENABLED_NAME);
	}

	@Override
	protected void initializeControls() {
		removeAllProperties();
		forceTextMIMEButton.setSelection(forceTextMIME);
		ignoreMaskValidationButton.setSelection(ignoreMaskValidation);
		populateAutopropTable(SVNTeamPropsPreferencePage.loadAutoProperties(autoPropsValue));
		populateCustompropTable(SVNTeamPropsPreferencePage.loadCustomProperties(customPropsValue));
	}

	@Override
	protected Control createContentsImpl(Composite parent) {
		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayout(new TabFolderLayout());
		tabFolder.setLayoutData(new GridData());

		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.PreferencePage_automaticProperties);
		tabItem.setControl(createAutopropsComposite(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.PreferencePage_customProperties);
		tabItem.setControl(createCustompropsComposite(tabFolder));

		//Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(parent, "org.eclipse.team.svn.help.autoPropsPreferencesContext"); //$NON-NLS-1$

		return tabFolder;
	}

	protected Composite createAutopropsComposite(TabFolder tabFolder) {
		Composite composite = new Composite(tabFolder, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		forceTextMIMEButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		forceTextMIMEButton.setLayoutData(data);
		forceTextMIMEButton.setText(SVNUIMessages.AutoPropsPreferencePage_forceTextMIME);
		forceTextMIMEButton.addListener(SWT.Selection, event -> forceTextMIME = forceTextMIMEButton.getSelection());

		createAutopropTable(composite);
		createAutopropButtons(composite);

		autopropTableViewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			autopropBtnEdit.setEnabled(selection.size() == 1);
			autopropBtnRemove.setEnabled(selection.size() > 0);
		});
		autopropTableViewer.addDoubleClickListener(event -> SVNTeamPropsPreferencePage.this.editAutoProperty());
		autopropTableViewer.addCheckStateListener(event -> {
			((SVNTeamPropsPreferencePage.AutoProperty) event.getElement()).enabled = event.getChecked();
			autopropBtnExport.setEnabled(
					autopropTableViewer.getCheckedElements().length != 0);
		});

		autopropBtnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNTeamPropsPreferencePage.this.addAutoProperty();
				autopropBtnExport.setEnabled(
						autopropTableViewer.getCheckedElements().length != 0);
			}
		});

		autopropBtnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNTeamPropsPreferencePage.this.editAutoProperty();
			}
		});

		autopropBtnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNTeamPropsPreferencePage.this.removeAutoProperty();
				autopropBtnExport.setEnabled(
						autopropTableViewer.getCheckedElements().length != 0);
			}
		});

		autopropBtnExport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNTeamPropsPreferencePage.this.exportAutoProperties();
			}
		});

		autopropBtnImport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNTeamPropsPreferencePage.this.importAutoProperties();
				autopropBtnExport.setEnabled(
						autopropTableViewer.getCheckedElements().length != 0);
			}
		});

		return composite;
	}

	protected void createAutopropTable(Composite parent) {
		autopropTableViewer = CheckboxTableViewer.newCheckList(parent,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnPixelData(20, false));
		layout.addColumnData(new ColumnWeightData(25, true));
		layout.addColumnData(new ColumnWeightData(75, true));

		autopropTableViewer.getTable().setLayout(layout);
		autopropTableViewer.getTable().setLinesVisible(true);
		autopropTableViewer.getTable().setHeaderVisible(true);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 200;
		autopropTableViewer.getTable().setLayoutData(data);

		autopropTableViewer.setLabelProvider(new ITableLabelProvider() {

			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				if (columnIndex == SVNTeamPropsPreferencePage.COLUMN_INDEX_FILE_NAME) {
					return ((SVNTeamPropsPreferencePage.AutoProperty) element).fileName;
				} else if (columnIndex == SVNTeamPropsPreferencePage.COLUMN_INDEX_PROPERTIES) {
					return ((SVNTeamPropsPreferencePage.AutoProperty) element).properties;
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
			}

		});

		TableColumn column = new TableColumn(autopropTableViewer.getTable(), SWT.NONE);
		column.setResizable(false);

		column = new TableColumn(autopropTableViewer.getTable(), SWT.NONE);
		column.setText(SVNUIMessages.AutoPropsPreferencePage_columnHeaderFileName);

		column = new TableColumn(autopropTableViewer.getTable(), SWT.NONE);
		column.setText(SVNUIMessages.AutoPropsPreferencePage_columnHeaderProperties);
	}

	protected void createAutopropButtons(Composite parent) {
		Composite buttons = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttons.setLayout(layout);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		autopropBtnAdd = createButton(buttons, SVNUIMessages.AutoPropsPreferencePage_buttonTextAdd, true);
		autopropBtnEdit = createButton(buttons, SVNUIMessages.AutoPropsPreferencePage_buttonTextEdit, false);
		autopropBtnRemove = createButton(buttons, SVNUIMessages.AutoPropsPreferencePage_buttonTextRemove, false);
		autopropBtnExport = createButton(buttons, SVNUIMessages.AutoPropsPreferencePage_buttonTextExport,
				autopropTableViewer.getCheckedElements().length != 0);
		autopropBtnImport = createButton(buttons, SVNUIMessages.AutoPropsPreferencePage_buttonTextImport, true);
	}

	protected Button createButton(Composite parent, String text, boolean enabled) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.widthHint = DefaultDialog.computeButtonWidth(button);
		button.setLayoutData(data);
		button.setEnabled(enabled);
		return button;
	}

	public void addAutoProperty() {
		EditAutoPropertiesPanel panel = new EditAutoPropertiesPanel(null);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == 0) {
			SVNTeamPropsPreferencePage.AutoProperty property = new SVNTeamPropsPreferencePage.AutoProperty(
					panel.getFileName(), panel.getProperties(), true);
			autopropTableViewer.add(property);
			autopropTableViewer.setChecked(property, property.enabled);
		}
	}

	public void editAutoProperty() {
		IStructuredSelection selection = (IStructuredSelection) autopropTableViewer.getSelection();
		SVNTeamPropsPreferencePage.AutoProperty property = (SVNTeamPropsPreferencePage.AutoProperty) selection
				.getFirstElement();
		EditAutoPropertiesPanel panel = new EditAutoPropertiesPanel(property);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == 0) {
			property.fileName = panel.getFileName();
			property.properties = panel.getProperties();
			autopropTableViewer.update(property, null);
		}
	}

	public void removeAutoProperty() {
		IStructuredSelection selection = (IStructuredSelection) autopropTableViewer.getSelection();
		autopropTableViewer.remove(selection.toArray());
	}

	public void removeAllProperties() {
		autopropTableViewer.getTable().clearAll();
		autopropTableViewer.refresh();
		custompropTableViewer.getTable().clearAll();
		custompropTableViewer.refresh();
	}

	public void exportAutoProperties() {
		String filePath = findConfigFile(SVNUIMessages.AutoPropsPreferencePage_dialogTitleExport);
		if (filePath == null) {
			return;
		}

		File srcCfg = new File(filePath);
		File tmpCfg = null;
		BufferedReader srcReader = null;
		PrintWriter tmpWriter = null;
		try {
			tmpCfg = SVNTeamPlugin.instance().getTemporaryFile(null, srcCfg.getName());
			if (!srcCfg.exists()) {
				srcCfg.createNewFile();
			}
			srcReader = new BufferedReader(new FileReader(srcCfg));
			tmpWriter = new PrintWriter(new FileOutputStream(tmpCfg));
			String line;
			// Copy cfg content till [auto-props] section
			while ((line = srcReader.readLine()) != null
					&& !line.startsWith(SVNTeamPropsPreferencePage.AUTO_PROPS_SECTION_HEADER)) {
				tmpWriter.println(line);
			}
			// Write [auto-props] section header
			tmpWriter.println(SVNTeamPropsPreferencePage.AUTO_PROPS_SECTION_HEADER);
			// Copy [auto-props] section content
			while ((line = srcReader.readLine()) != null && !line.trim().equals("")) { //$NON-NLS-1$
				tmpWriter.println(line);
			}
			// Insert auto-properties
			Object[] checkedProps = autopropTableViewer.getCheckedElements();
			for (Object checkedProp : checkedProps) {
				SVNTeamPropsPreferencePage.AutoProperty property = (SVNTeamPropsPreferencePage.AutoProperty) checkedProp;
				if (!property.properties.equals("")) { //$NON-NLS-1$
					tmpWriter.println(property.fileName + " " + //$NON-NLS-1$
							SVNTeamPropsPreferencePage.AUTO_PROPS_PATTERN_SEPARATOR + " " + //$NON-NLS-1$
							property.properties);
				}
			}
			// Copy the rest of cfg
			while ((line = srcReader.readLine()) != null) {
				tmpWriter.println(line);
			}
			// Close streams
			srcReader.close();
			tmpWriter.close();
			// Copy tmpCfg file to srcCfg file
			srcCfg.delete();
			tmpCfg.renameTo(srcCfg);
		} catch (IOException ioe) {
			LoggedOperation.reportError(SVNUIMessages.Error_ExportProperties, ioe);
			return;
		} finally {
			try {
				if (srcReader != null) {
					srcReader.close();
				}
			} catch (IOException ioe) {
				// nothing to do
			}
			if (tmpWriter != null) {
				tmpWriter.close();
			}
			if (tmpCfg != null) {
				tmpCfg.delete();
			}
		}
	}

	public void importAutoProperties() {
		String filePath = findConfigFile(SVNUIMessages.AutoPropsPreferencePage_dialogTitleImport);
		if (filePath == null) {
			return;
		}

		BufferedReader cfgReader = null;
		ArrayList<AutoProperty> autoPropsList = new ArrayList<>();
		try {
			cfgReader = new BufferedReader(new FileReader(filePath));
			String line;
			// Skip cfg content till [auto-props] section header inclusive
			while ((line = cfgReader.readLine()) != null) {
				if (line.startsWith(SVNTeamPropsPreferencePage.AUTO_PROPS_SECTION_HEADER)) {
					break;
				}
			}

			//another section beginning pattern
			Pattern p = Pattern.compile("\\[.*\\]"); //$NON-NLS-1$
			// Process [auto-props] section content
			while ((line = cfgReader.readLine()) != null && !p.matcher(line).matches()) {
				if (line.startsWith(SVNTeamPropsPreferencePage.AUTO_PROPS_COMMENT_START) || line.equals("")) { //$NON-NLS-1$
					continue;
				}
				String fileName = ""; //$NON-NLS-1$
				String properties = ""; //$NON-NLS-1$
				int index = line.indexOf(SVNTeamPropsPreferencePage.AUTO_PROPS_PATTERN_SEPARATOR);
				if (index != -1) {
					fileName = line.substring(0, index).trim();
					if (index < line.length() - 1) {
						properties = line.substring(index + 1).trim();
					}
				} else {
					fileName = line.trim();
				}
				autoPropsList.add(new SVNTeamPropsPreferencePage.AutoProperty(fileName, properties, true));
			}

			// Set new properties
			populateAutopropTable(autoPropsList.toArray());
		} catch (IOException ioe) {
			LoggedOperation.reportError(SVNUIMessages.Error_ImportProperties, ioe);
			return;
		} finally {
			try {
				if (cfgReader != null) {
					cfgReader.close();
				}
			} catch (IOException ioe) {
				// nothing to do
			}
		}
	}

	public void populateAutopropTable(Object[] items) {
		for (Object item : items) {
			autopropTableViewer.add(item);
			autopropTableViewer.setChecked(item, ((SVNTeamPropsPreferencePage.AutoProperty) item).enabled);
		}
	}

	public void populateCustompropTable(Object[] items) {
		for (Object item : items) {
			custompropTableViewer.add(item);
		}
	}

	protected Composite createCustompropsComposite(TabFolder tabFolder) {
		Composite composite = new Composite(tabFolder, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		ignoreMaskValidationButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		ignoreMaskValidationButton.setLayoutData(data);
		ignoreMaskValidationButton.setText(SVNUIMessages.CustomPropsPreferencePage_ignoreMaskValidation);
		ignoreMaskValidationButton.addListener(SWT.Selection, event -> ignoreMaskValidation = ignoreMaskValidationButton.getSelection());

		createCustompropTable(composite);
		createCustompropButtons(composite);

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 80;
		data.widthHint = 0;
		customPropDescription = SpellcheckedTextProvider.getTextWidget(composite, data,
				SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		customPropDescription.setText(SVNUIMessages.CustomPropsPreferencePage_description);
		customPropDescription.setEditable(false);

		custompropTableViewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			SVNTeamPropsPreferencePage.this.refreshDescription();
			custompropBtnEdit.setEnabled(selection.size() == 1);
			custompropBtnRemove.setEnabled(selection.size() > 0);
		});

		custompropTableViewer.addDoubleClickListener(event -> SVNTeamPropsPreferencePage.this.editCustomProperty());

		custompropBtnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNTeamPropsPreferencePage.this.addCustomProperty();
			}
		});

		custompropBtnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNTeamPropsPreferencePage.this.editCustomProperty();
			}
		});

		custompropBtnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNTeamPropsPreferencePage.this.removeCustomProperty();
			}
		});

		return composite;
	}

	protected void createCustompropTable(Composite parent) {
		custompropTableViewer = new TableViewer(parent,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);// CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(70, true));

		custompropTableViewer.getTable().setLayout(layout);
		custompropTableViewer.getTable().setLinesVisible(true);
		custompropTableViewer.getTable().setHeaderVisible(true);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 150;
		custompropTableViewer.getTable().setLayoutData(data);
		custompropTableViewer.setLabelProvider(new ITableLabelProvider() {

			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				if (columnIndex == 0) {
					return ((PredefinedProperty) element).name;
				} else if (columnIndex == 1) {
					return ((PredefinedProperty) element).validationRegexp;
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
			}

		});

		TableColumn column = new TableColumn(custompropTableViewer.getTable(), SWT.NONE);
		column.setText(SVNUIMessages.CustomPropsPreferencePage_columnHeaderPropName);

		column = new TableColumn(custompropTableViewer.getTable(), SWT.NONE);
		column.setText(SVNUIMessages.CustomPropsPreferencePage_columnHeaderRegexp);

	}

	protected void createCustompropButtons(Composite parent) {
		Composite buttons = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttons.setLayout(layout);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		custompropBtnAdd = createButton(buttons, SVNUIMessages.AutoPropsPreferencePage_buttonTextAdd, true);
		custompropBtnEdit = createButton(buttons, SVNUIMessages.AutoPropsPreferencePage_buttonTextEdit, false);
		custompropBtnRemove = createButton(buttons, SVNUIMessages.AutoPropsPreferencePage_buttonTextRemove, false);
	}

	protected void refreshDescription() {
		IStructuredSelection selection = (IStructuredSelection) custompropTableViewer.getSelection();
		if (selection.size() == 0) {
			customPropDescription.setText(SVNUIMessages.CustomPropsPreferencePage_description);
			return;
		}
		String description = ((PredefinedProperty) selection.getFirstElement()).description;
		if (description.equals("")) { //$NON-NLS-1$
			customPropDescription.setText("No description available.");
		} else {
			customPropDescription.setText(description);
		}
	}

	public void addCustomProperty() {
		EditCustomPropertiesPanel panel = new EditCustomPropertiesPanel(null);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == 0) {
			custompropTableViewer.add(panel.getProperty());
		}
	}

	public void editCustomProperty() {
		IStructuredSelection selection = (IStructuredSelection) custompropTableViewer.getSelection();
		PredefinedProperty property = (PredefinedProperty) selection.getFirstElement();
		EditCustomPropertiesPanel panel = new EditCustomPropertiesPanel(property);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == 0) {
			property = panel.getProperty();
			custompropTableViewer.update(property, null);
			refreshDescription();
		}
	}

	public void removeCustomProperty() {
		IStructuredSelection selection = (IStructuredSelection) custompropTableViewer.getSelection();
		custompropTableViewer.remove(selection.toArray());
		refreshDescription();
	}

	public static PredefinedProperty[] loadCustomProperties(String encodedProps) {
		ArrayList<PredefinedProperty> propsList = new ArrayList<>();
		String[] props = FileUtility.decodeStringToArray(encodedProps);
		for (int i = 0; i < props.length; i += 3) {
			String propName = props[i];
			String regexp = i + 1 == props.length ? "" : props[i + 1]; //$NON-NLS-1$
			String description = i + 2 >= props.length ? "" : props[i + 2]; //$NON-NLS-1$
			PredefinedProperty property = new PredefinedProperty(propName, description, "", regexp); //$NON-NLS-1$
			propsList.add(property);
		}
		return propsList.toArray(new PredefinedProperty[propsList.size()]);
	}

	public static Object[] loadAutoProperties(String encodedProps) {
		ArrayList<AutoProperty> propsList = new ArrayList<>();
		String[] props = FileUtility.decodeStringToArray(encodedProps);
		for (int i = 0; i < props.length; i += 3) {
			boolean enabled = !props[i].equals("0"); //$NON-NLS-1$
			String fileName = props[i + 1];
			String properties = i + 2 == props.length ? "" : props[i + 2]; //$NON-NLS-1$
			SVNTeamPropsPreferencePage.AutoProperty property = new SVNTeamPropsPreferencePage.AutoProperty(fileName,
					properties, enabled);
			propsList.add(property);
		}
		return propsList.toArray();
	}

	public String findConfigFile(String dialogTitle) {
		ISVNConnector connector = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		String cfgDir;
		try {
			cfgDir = connector.getConfigDirectory();
		} catch (SVNConnectorException cwe) {
			LoggedOperation.reportError(SVNUIMessages.Error_FindConfigFile, cwe);
			return null;
		}
		FileDialog dlg = new FileDialog(getShell());
		dlg.setText(dialogTitle);
		dlg.setFilterPath(cfgDir);
		File cfgFile = new File(
		cfgDir + FileSystems.getDefault().getSeparator() + SVNTeamPropsPreferencePage.AUTO_PROPS_CONFIG_FILE_NAME);
		if (cfgFile.exists()) {
			dlg.setFileName(SVNTeamPropsPreferencePage.AUTO_PROPS_CONFIG_FILE_NAME);
		}
		return dlg.open();
	}

}
