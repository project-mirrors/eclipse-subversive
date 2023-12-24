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
 *    Ken Geis - [patch] fixed bug in validation messages
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.verifier.AbsolutePathVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourceNameVerifier;

/**
 * Composite to choose trunk, branches and tags locations
 *
 * @author Sergiy Logvin
 */
public class RepositoryRootsComposite extends Composite implements IPropertiesPanel {
	protected Button structureCheckBox;

	protected Text trunkRight;

	protected Text branchesRight;

	protected Text tagsRight;

	protected boolean createLocation;

	protected String trunkLocation;

	protected String branchesLocation;

	protected String tagsLocation;

	protected boolean structureEnabled;

	protected boolean forceDisableRoots;

	protected IValidationManager validationManager;

	public RepositoryRootsComposite(Composite parent, int style, IValidationManager validationManager) {
		super(parent, style);
		this.validationManager = validationManager;
	}

	@Override
	public void saveChanges() {
		structureEnabled = structureCheckBox.getSelection();

		trunkLocation = trunkRight.getText();
		branchesLocation = branchesRight.getText();
		tagsLocation = tagsRight.getText();
	}

	@Override
	public void resetChanges() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();

		if (createLocation) {
			trunkLocation = SVNTeamPreferences.getRepositoryString(store, SVNTeamPreferences.REPOSITORY_HEAD_NAME);
			branchesLocation = SVNTeamPreferences.getRepositoryString(store,
					SVNTeamPreferences.REPOSITORY_BRANCHES_NAME);
			tagsLocation = SVNTeamPreferences.getRepositoryString(store, SVNTeamPreferences.REPOSITORY_TAGS_NAME);
			structureEnabled = !forceDisableRoots;
		}
		structureCheckBox.setSelection(structureEnabled);

		trunkRight.setText(trunkLocation);
		branchesRight.setText(branchesLocation);
		tagsRight.setText(tagsLocation);

		refreshButtons();
	}

	@Override
	public void cancelChanges() {

	}

	@Override
	public void initialize() {
		GridLayout layout = null;
		GridData data = null;

		layout = new GridLayout();
		layout.marginHeight = 7;
		layout.verticalSpacing = 3;
		setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		setLayoutData(data);

		structureCheckBox = new Button(this, SWT.CHECK);
		data = new GridData();
		structureCheckBox.setLayoutData(data);
		structureCheckBox.setText(SVNUIMessages.RepositoryRootsComposite_EnableDetection);
		structureCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = ((Button) e.widget).getSelection();
				trunkRight.setEnabled(enabled);
				branchesRight.setEnabled(enabled);
				tagsRight.setEnabled(enabled);
				validationManager.validateContent();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Group standardLocations = new Group(this, SWT.NONE);
		standardLocations.setText(SVNUIMessages.RepositoryRootsComposite_ResourceNames);
		layout = new GridLayout();
		layout.numColumns = 2;
		standardLocations.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		standardLocations.setLayoutData(data);

		trunkRight = createControl(standardLocations, "RepositoryRootsComposite_Trunk"); //$NON-NLS-1$
		branchesRight = createControl(standardLocations, "RepositoryRootsComposite_Branches"); //$NON-NLS-1$
		tagsRight = createControl(standardLocations, "RepositoryRootsComposite_Tags"); //$NON-NLS-1$
	}

	public boolean isStructureEnabled() {
		return structureEnabled;
	}

	public void setStructureEnabled(boolean structureEnabled) {
		this.structureEnabled = structureEnabled;
	}

	public String getBranchesLocation() {
		return branchesLocation;
	}

	public void setBranchesLocation(String branchesLocation) {
		this.branchesLocation = branchesLocation;
	}

	public String getTagsLocation() {
		return tagsLocation;
	}

	public void setTagsLocation(String tagsLocation) {
		this.tagsLocation = tagsLocation;
	}

	public String getTrunkLocation() {
		return trunkLocation;
	}

	public void setTrunkLocation(String trunkLocation) {
		this.trunkLocation = trunkLocation;
	}

	public boolean isCreateLocation() {
		return createLocation;
	}

	public void setCreateLocation(boolean createLocation) {
		this.createLocation = createLocation;
	}

	public void setForceDisableRoots(boolean force) {
		forceDisableRoots = force;
		resetChanges();
	}

	protected Text createControl(Composite standardLocations, String id) {
		Label label = new Label(standardLocations, SWT.NONE);
		GridData data = new GridData();
		label.setLayoutData(data);
		label.setText(SVNUIMessages.getString(id));

		Text field = new Text(standardLocations, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		field.setLayoutData(data);
		String name = SVNUIMessages.getString(id + "_Verifier"); //$NON-NLS-1$
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new ResourceNameVerifier(name, false));
		verifier.add(new NonEmptyFieldVerifier(name));
		verifier.add(new AbsolutePathVerifier(name));
		validationManager.attachTo(field, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return structureCheckBox.getSelection();
			}
		});

		return field;
	}

	protected void refreshButtons() {
		trunkRight.setEnabled(structureCheckBox.getSelection());
		branchesRight.setEnabled(structureCheckBox.getSelection());
		tagsRight.setEnabled(structureCheckBox.getSelection());
	}

}
