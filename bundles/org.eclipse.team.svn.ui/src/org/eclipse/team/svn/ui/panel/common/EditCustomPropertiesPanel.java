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

package org.eclipse.team.svn.ui.panel.common;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.extension.properties.PredefinedProperty;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.SpellcheckedTextProvider;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.PropertyNameVerifier;

/**
 * Panel to be used in a dialog for entering user defined property and description.
 * 
 * @author Alexei Goncharov
 */
public class EditCustomPropertiesPanel extends AbstractDialogPanel {

	protected PredefinedProperty property;

	protected Text propName;

	protected Text propRegexp;

	protected StyledText propDescription;

	public EditCustomPropertiesPanel(PredefinedProperty property) {
		this.property = property;
		dialogTitle = property == null
				? SVNUIMessages.EditCustomPropertiesPanel_Title_Add
				: SVNUIMessages.EditAutoPropertiesPanel_Title_Edit;
		dialogDescription = SVNUIMessages.EditCustomPropertiesPanel_Description;
		defaultMessage = SVNUIMessages.EditCustomPropertiesPanel_Message;
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 10;
		parent.setLayout(layout);
		Label propNameLabel = new Label(parent, SWT.NONE);
		propNameLabel.setText(SVNUIMessages.EditCustomPropertiesPanel_PropName);
		propName = new Text(parent, SWT.BORDER);
		propName.setText(property == null ? "" : property.name); //$NON-NLS-1$
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
		propName.setLayoutData(layoutData);

		CompositeVerifier verifier = new CompositeVerifier();
		String name = SVNUIMessages.EditCustomPropertiesPanel_PropName_Verifier;
		verifier.add(new NonEmptyFieldVerifier(name));
		verifier.add(new PropertyNameVerifier(name));
		attachTo(propName, verifier);

		Group optional = new Group(parent, SWT.NONE);
		optional.setText(SVNUIMessages.EditCustomPropertiesPanel_Optional);
		layoutData = new GridData(GridData.FILL_BOTH);
		layoutData.horizontalSpan = 2;
		optional.setLayoutData(layoutData);
		layout = new GridLayout();
		layout.numColumns = 1;
		optional.setLayout(layout);

		Label propRegexpLabel = new Label(optional, SWT.NONE);
		propRegexpLabel.setText(SVNUIMessages.EditCustomPropertiesPanel_PropRegExp);
		propRegexp = new Text(optional, SWT.BORDER);
		propRegexp.setText(property == null ? "" : property.validationRegexp); //$NON-NLS-1$
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		propRegexp.setLayoutData(layoutData);
		attachTo(propRegexp, new AbstractFormattedVerifier("EditCustomProperty_Regexp") { //$NON-NLS-1$
			@Override
			protected String getErrorMessageImpl(Control input) {
				try {
					Pattern.compile(getText(input));
				} catch (Exception ex) {
					return SVNUIMessages.EditCustomPropertiesPanel_Validator_RegExp;
				}
				return null;
			}

			@Override
			protected String getWarningMessageImpl(Control input) {
				return null;
			}
		});

		Label propDescriptionLabel = new Label(optional, SWT.NONE);
		propDescriptionLabel.setText(SVNUIMessages.EditCustomPropertiesPanel_PropDescription);
		layoutData = new GridData();
		propDescriptionLabel.setLayoutData(layoutData);
		layoutData = new GridData(GridData.FILL_BOTH);
		layoutData.heightHint = 80;
		layoutData.widthHint = 0;
		propDescription = SpellcheckedTextProvider.getTextWidget(optional, layoutData,
				SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		propDescription.setText(property == null ? "" : property.description); //$NON-NLS-1$
	}

	public PredefinedProperty getProperty() {
		return property;
	}

	@Override
	protected void cancelChangesImpl() {
	}

	@Override
	protected void saveChangesImpl() {
		property = new PredefinedProperty(propName.getText(), propDescription.getText(), "", //$NON-NLS-1$
				propRegexp.getText());
	}

}
