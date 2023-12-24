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
 *    Gabor Liptak - Speedup Pattern's usage
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.wizard.checkoutas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.ExistingResourceVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * Project location selection page
 * 
 * @author Alexander Gurov
 */
public class ProjectLocationSelectionPage extends AbstractVerifiedWizardPage {
	protected static String DEFAULT_WORKING_SET;

	protected String location;

	protected String defaultLocation;

	protected String workingSetName;

	protected boolean useDefaultLocation;

	protected ProjectsSelectionPage projectsSelectionPage;

	protected Button browse;

	protected Button useDefaultLocationButton;

	protected Combo workingSetNameCombo;

	protected Text locationField;

	public ProjectLocationSelectionPage(boolean multiple, ProjectsSelectionPage projectsSelectionPage) {
		super(ProjectLocationSelectionPage.class.getName(), SVNUIMessages.ProjectLocationSelectionPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$

		ProjectLocationSelectionPage.DEFAULT_WORKING_SET = SVNUIMessages.ProjectLocationSelectionPage_DefaultWS;

		setDescription(multiple
				? SVNUIMessages.ProjectLocationSelectionPage_Description_Multi
				: SVNUIMessages.ProjectLocationSelectionPage_Description_Single);

		this.projectsSelectionPage = projectsSelectionPage;
		defaultLocation = SVNTeamPreferences.getCheckoutBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.CHECKOUT_USE_DEFAULT_LOCATION_NAME)
						? ResourcesPlugin.getWorkspace().getRoot().getLocation().toString()
						: SVNTeamPreferences.getCheckoutString(SVNTeamUIPlugin.instance().getPreferenceStore(),
								SVNTeamPreferences.CHECKOUT_SPECIFIED_LOCATION_NAME);
		location = defaultLocation;
	}

	public String getLocation() {
		return useDefaultLocation ? defaultLocation : location;
	}

	public String getWorkingSetName() {
		return workingSetName.equals(ProjectLocationSelectionPage.DEFAULT_WORKING_SET) ? null : workingSetName;
	}

	public void setUseDefaultLocation(boolean defaultLocation) {
		useDefaultLocationButton.setSelection(defaultLocation);
		useDefaultLocationButton.setEnabled(defaultLocation);
		refreshControls();
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		// GridLayout
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);

		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		setControl(composite);

		Group locationSelectionGroup = new Group(composite, SWT.NONE);
		locationSelectionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout = new GridLayout();
		layout.numColumns = 2;
		locationSelectionGroup.setLayout(layout);
		locationSelectionGroup.setText(SVNUIMessages.ProjectLocationSelectionPage_Location);

		useDefaultLocationButton = new Button(locationSelectionGroup, SWT.CHECK);

		locationField = new Text(locationSelectionGroup, SWT.SINGLE | SWT.BORDER);
		browse = new Button(locationSelectionGroup, SWT.PUSH);

		data = new GridData();
		data.horizontalSpan = 2;
		useDefaultLocationButton.setLayoutData(data);
		useDefaultLocationButton.setSelection(useDefaultLocation);
		useDefaultLocationButton.setText(SVNUIMessages.ProjectLocationSelectionPage_UseLocationFromSettings);
		useDefaultLocationButton.addListener(SWT.Selection, event -> {
			ProjectLocationSelectionPage.this.validateContent();
			ProjectLocationSelectionPage.this.refreshControls();
		});

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 300;
		locationField.setLayoutData(data);
		locationField.setText(location);
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new ProjectLocationSelectionPage.LocationVerifier(projectsSelectionPage, defaultLocation,
				useDefaultLocationButton));
		verifier.add(new AbstractVerifierProxy(
				new ExistingResourceVerifier(SVNUIMessages.ProjectLocationSelectionPage_Location_Verifier, false)) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return !useDefaultLocationButton.getSelection();
			}
		});
		attachTo(locationField, verifier);
		locationField.addModifyListener(e -> location = locationField.getText());
		locationField.setEnabled(!useDefaultLocationButton.getSelection());

		browse.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browse);
		browse.setLayoutData(data);
		browse.addListener(SWT.Selection, event -> {
			DirectoryDialog fileDialog = new DirectoryDialog(getShell());
			fileDialog.setFilterPath(locationField.getText());
			String res = fileDialog.open();
			if (res != null) {
				locationField.setText(res);
			}
		});
		browse.setEnabled(!useDefaultLocationButton.getSelection());

		Composite workingSetComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.numColumns = 2;
		data = new GridData(GridData.FILL_HORIZONTAL);
		workingSetComposite.setLayout(layout);
		workingSetComposite.setLayoutData(data);

		Label wSetLabel = new Label(workingSetComposite, SWT.NONE);
		wSetLabel.setText(SVNUIMessages.ProjectLocationSelectionPage_SelectWS);

		workingSetNameCombo = new Combo(workingSetComposite, SWT.NULL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		workingSetNameCombo.setLayoutData(data);
		String[] wSetNames = getWorkingSetNames();
		List names = new ArrayList();
		names.add(ProjectLocationSelectionPage.DEFAULT_WORKING_SET);
		names.addAll(Arrays.asList(wSetNames));
		workingSetNameCombo.setItems((String[]) names.toArray(new String[names.size()]));

		Listener workingSetNameComboListener = event -> workingSetName = workingSetNameCombo.getText();
		workingSetNameCombo.addListener(SWT.Selection, workingSetNameComboListener);
		workingSetNameCombo.addListener(SWT.Modify, workingSetNameComboListener);

		workingSetNameCombo.setText(ProjectLocationSelectionPage.DEFAULT_WORKING_SET);
		attachTo(workingSetNameCombo,
				new NonEmptyFieldVerifier(SVNUIMessages.ProjectLocationSelectionPage_WorkingSet_Verifier));

//		Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(composite, "org.eclipse.team.svn.help.projectLocationSelectionContext"); //$NON-NLS-1$

		return composite;
	}

	protected void refreshControls() {
		useDefaultLocation = useDefaultLocationButton.isEnabled() && useDefaultLocationButton.getSelection();
		locationField.setEnabled(!useDefaultLocation);
		browse.setEnabled(!useDefaultLocation);
	}

	protected String[] getWorkingSetNames() {
		List wSetNames = new ArrayList();
		IWorkingSetManager workingSetManager = SVNTeamUIPlugin.instance().getWorkbench().getWorkingSetManager();
		IWorkingSet[] workingSets = workingSetManager.getWorkingSets();
		for (IWorkingSet workingSet : workingSets) {
			wSetNames.add(workingSet.getName());
		}
		return (String[]) wSetNames.toArray(new String[wSetNames.size()]);
	}

	public static class LocationVerifier extends AbstractVerifier {

		protected String defaultLocation;

		protected ProjectsSelectionPage projectsSelectionPage;

		protected Button useDefaultLocationButton;

		public LocationVerifier(ProjectsSelectionPage projectsSelectionPage, String defaultLocation,
				Button useDefaultLocationButton) {
			this.defaultLocation = defaultLocation;
			this.projectsSelectionPage = projectsSelectionPage;
			this.useDefaultLocationButton = useDefaultLocationButton;
		}

		@Override
		protected String getErrorMessage(Control input) {
			String parent = projectsSelectionPage != null && projectsSelectionPage.projects != null
					? SVNUtility.getResourceParent(projectsSelectionPage.projects[0])
					: ""; //$NON-NLS-1$
			String inputLocation = useDefaultLocationButton.getSelection()
					? defaultLocation
					: FileUtility.formatPath(getText(input));
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject project : projects) {
				IPath location = project.getLocation();
				if (location != null && location.isPrefixOf(new Path(inputLocation + parent))) {
					return BaseMessages.format(
							SVNUIMessages.ProjectLocationSelectionPage_Location_Verifier_Error_ExistingProject,
							new String[] { location.toString() });
				}
			}
			return null;
		}

		@Override
		protected String getWarningMessage(Control input) {
			return null;
		}

	}

}
