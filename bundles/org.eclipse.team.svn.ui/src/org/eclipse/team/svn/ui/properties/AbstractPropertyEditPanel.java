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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.properties.PredefinedProperty;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositePropertiesVerifier;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.ExistingResourceVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.PropertyNameVerifier;
import org.eclipse.team.svn.ui.verifier.PropertyVerifier;

/**
 * Abstract panel for editing properties implementation.
 * 
 * @author Alexei Goncharov
 */
public abstract class AbstractPropertyEditPanel extends AbstractDialogPanel {

	protected SVNProperty[] source;

	protected HashMap<String, String> alreadyExistent;

	protected Combo nameField;

	protected Text valueField;

	protected Text fileField;

	protected Text descriptionField;

	protected boolean fileSelected;

	protected String propertyName;

	protected String propertyValue;

	protected String propertyFile;

	protected List<PredefinedProperty> predefinedProperties;

	protected HashMap<String, AbstractFormattedVerifier> verifiers;

	public AbstractPropertyEditPanel(SVNProperty[] propertyData, String dialogTitle, String dialogDescription) {
		if (propertyData != null) {
			propertyName = propertyData[0].name;
			propertyValue = propertyData[0].value;
		}
		verifiers = new HashMap<>();
		this.dialogTitle = dialogTitle;
		this.dialogDescription = dialogDescription;
		source = propertyData;
		fileSelected = false;
		alreadyExistent = new HashMap<>();
		if (propertyData != null) {
			for (SVNProperty current : propertyData) {
				alreadyExistent.put(current.name, current.value);
			}
		}
	}

	public boolean isFileSelected() {
		return fileSelected;
	}

	public String getPropertyFile() {
		return propertyFile;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getPropertyValue() {
		return propertyValue;
	}

	public void setPropertyToEdit(SVNProperty propertyToEdit) {
		if (propertyToEdit != null) {
			propertyName = propertyToEdit.name;
			propertyValue = propertyToEdit.value;
		} else {
			propertyName = propertyValue = ""; //$NON-NLS-1$
		}
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.AbstractPropertyEditPanel_Name);

		nameField = new Combo(composite, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		nameField.setLayoutData(data);
		nameField.setVisibleItemCount(10);
		nameField.setItems(getPropertyNames(predefinedProperties));
		nameField.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selected = nameField.getItem(nameField.getSelectionIndex());
				String value = alreadyExistent.get(selected);
				if (value != null) {
					valueField.setText(value);
				} else {
					PredefinedProperty prop = AbstractPropertyEditPanel.this.getPredefinedProperty(selected);
					if (prop != null) {
						valueField.setText(prop.value);
					}
				}
				descriptionField.setText(AbstractPropertyEditPanel.this.getDescriptionText());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		Listener nameFieldListener = event -> {
			PredefinedProperty prop = AbstractPropertyEditPanel.this.getPredefinedProperty(nameField.getText());
			if (prop != null) {
				valueField.setText(prop.value);
			}
			descriptionField.setText(AbstractPropertyEditPanel.this.getDescriptionText());
		};
		nameField.addListener(SWT.Selection, nameFieldListener);
		nameField.addListener(SWT.Modify, nameFieldListener);

		Composite descriptionComposite = new Composite(composite, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		descriptionComposite.setLayoutData(data);
		GridLayout descriptionLayout = new GridLayout();
		descriptionLayout.horizontalSpacing = 5;
		descriptionLayout.numColumns = 2;
		descriptionComposite.setLayout(descriptionLayout);

		Label bulb = new Label(descriptionComposite, SWT.NONE);
		bulb.setImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/dialogs/bulb.png").createImage()); //$NON-NLS-1$
		data = new GridData();
		data.verticalAlignment = SWT.TOP;
		bulb.setLayoutData(data);

		descriptionField = new Text(descriptionComposite, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		data.grabExcessVerticalSpace = true;
		data.heightHint = 100;
		descriptionField.setLayoutData(data);
		descriptionField.setText(getDescriptionText());
		descriptionField.setBackground(nameField.getBackground());
		descriptionComposite.setBackground(nameField.getBackground());
		bulb.setBackground(nameField.getBackground());
		descriptionField.setEditable(false);

		CompositeVerifier verifier = new CompositeVerifier();
		String name = SVNUIMessages.AbstractPropertyEditPanel_Name_Verifier;
		verifier.add(new NonEmptyFieldVerifier(name));
		verifier.add(new PropertyNameVerifier(name));
		attachTo(nameField, verifier);

		final Button editManual = new Button(composite, SWT.RADIO);

		valueField = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		CompositePropertiesVerifier valueVerifier = new CompositePropertiesVerifier(nameField, verifiers);
		attachTo(valueField, new AbstractVerifierProxy(valueVerifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return editManual.getSelection();
			}
		});

		data = new GridData();
		editManual.setLayoutData(data);
		editManual.setText(SVNUIMessages.AbstractPropertyEditPanel_EnterValue);
		editManual.addListener(SWT.Selection, event -> {
			Button button = (Button) event.widget;
			valueField.setEnabled(button.getSelection());
			fileSelected = false;
			AbstractPropertyEditPanel.this.validateContent();
		});
		editManual.setSelection(true);

		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 100;
		valueField.setLayoutData(data);
		valueField.selectAll();
		valueField.setEnabled(true);

		final Button loadFromFile = new Button(composite, SWT.RADIO);

		Composite subComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		subComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		subComposite.setLayoutData(data);

		fileField = new Text(subComposite, SWT.SINGLE | SWT.BORDER);
		final Button browse = new Button(subComposite, SWT.PUSH);
		browse.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browse);
		browse.setLayoutData(data);
		browse.setEnabled(false);
		browse.addListener(SWT.Selection, event -> {
			FileDialog fileDialog = new FileDialog(AbstractPropertyEditPanel.this.manager.getShell(), SWT.OPEN);
			String res = fileDialog.open();
			if (res != null) {
				fileField.setText(res);
				AbstractPropertyEditPanel.this.validateContent();
			}
		});

		data = new GridData();
		loadFromFile.setLayoutData(data);
		loadFromFile.setText(SVNUIMessages.AbstractPropertyEditPanel_LoadValue);
		loadFromFile.addListener(SWT.Selection, event -> {
			Button button = (Button) event.widget;
			fileField.setEnabled(button.getSelection());
			browse.setEnabled(button.getSelection());
			fileSelected = true;
			AbstractPropertyEditPanel.this.validateContent();
		});

		data = new GridData(GridData.FILL_HORIZONTAL);
		fileField.setLayoutData(data);
		fileField.setEnabled(false);
		attachTo(fileField, new AbstractVerifierProxy(
				new ExistingResourceVerifier(SVNUIMessages.AbstractPropertyEditPanel_File_Verifier, true)) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return loadFromFile.getSelection();
			}
		});
		if (source != null && source.length > 0) {
			nameField.setText(propertyName);
			valueField.setText(propertyValue);
		}
		nameField.setFocus();
	}

	/**
	 * 
	 * @return a list of predefined properties. Must not be null.
	 */
	protected List<PredefinedProperty> getPredefinedProperties() {
		ArrayList<PredefinedProperty> properties = new ArrayList<>();
		for (PredefinedProperty property : CoreExtensionsManager.instance()
				.getPredefinedPropertySet()
				.getPredefinedProperties()) {
			if (isPropertyAccepted(property)) {
				properties.add(property);
			}
		}
		return properties;
	}

	protected void fillVerifiersMap() {
		predefinedProperties = getPredefinedProperties();
		IRepositoryResource base = getRepostioryResource();
		for (PredefinedProperty current : predefinedProperties) {
			verifiers.put(current.name, new PropertyVerifier("EditPropertiesInputField", //$NON-NLS-1$
					"".equals(current.validationRegexp) ? null : current.validationRegexp, current.name, base)); //$NON-NLS-1$
		}
	}

	protected abstract boolean isPropertyAccepted(PredefinedProperty property);

	protected abstract IRepositoryResource getRepostioryResource();

	protected String[] getPropertyNames(List<PredefinedProperty> predefinedProperties) {
		List<String> names = new ArrayList<>();
		for (Iterator<PredefinedProperty> it = predefinedProperties.iterator(); it.hasNext();) {
			names.add(it.next().name);
		}
		return names.toArray(new String[names.size()]);
	}

	protected String getDescriptionText() {
		String propName = nameField.getText();
		PredefinedProperty prop = getPredefinedProperty(propName);
		if (prop != null) {
			return prop.description != null && prop.description.trim().length() > 0
					? prop.description
					: SVNUIMessages.AbstractPropertyEditPanel_NoDescription;
		}
		return SVNUIMessages.AbstractPropertyEditPanel_UserDefined;
	}

	protected PredefinedProperty getPredefinedProperty(String name) {
		int idx = predefinedProperties.indexOf(new PredefinedProperty(name));
		if (idx >= 0) {
			return predefinedProperties.get(idx);
		}
		return null;
	}

	@Override
	protected void saveChangesImpl() {
		propertyName = nameField.getText();
		propertyValue = valueField.getText();
		propertyFile = fileField.getText();
	}

	@Override
	protected void cancelChangesImpl() {
	}

	@Override
	public String getDefaultMessage() {
		return SVNUIMessages.AbstractPropertyEditPanel_DefaultMessage;
	}
}
