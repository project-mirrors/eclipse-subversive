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

package org.eclipse.team.svn.ui.wizard.shareproject;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.extension.impl.ISelectProjectNamePageData;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.verifier.AbstractValidationManagerProxy;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Select repository folder name for project
 * 
 * @author Alexander Gurov
 */
public class SelectProjectNamePage extends AbstractVerifiedWizardPage {

	protected boolean isSimpleMode;

	protected Button simpleModeRadionButton;

	protected Button advancedModeRadionButton;

	protected SelectProjectNamePageSimpleModeComposite simpleModeComposite;

	protected ShareProjectNameAdvancedModeComposite advancedModeComposite;

	public SelectProjectNamePage() {
		super(
				SelectProjectNamePage.class.getName(), "", //$NON-NLS-1$
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		isSimpleMode = SVNTeamPreferences.getRepositoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.REPOSITORY_SIMPLE_SHARE_NAME);
	}

	protected class SelectProjectNamePageValidationManager extends AbstractValidationManagerProxy {

		protected boolean isSimpleValidationManager;

		public SelectProjectNamePageValidationManager(IValidationManager validationManager,
				boolean isSimpleValidationManager) {
			super(validationManager);
			this.isSimpleValidationManager = isSimpleValidationManager;
		}

		@Override
		protected boolean isVerificationEnabled(Control input) {
			return isSimpleMode == SelectProjectNamePageValidationManager.this.isSimpleValidationManager;
		}
	}

	/*
	 * Listens to changes on mode buttons
	 */
	protected class ModeListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			//change controls area mode
			Button modeButton = (Button) e.widget;
			if (simpleModeRadionButton == modeButton && !isSimpleMode) {
				isSimpleMode = true;
				enableControlsArea();
			} else if (advancedModeRadionButton == modeButton && isSimpleMode) {
				isSimpleMode = false;
				enableControlsArea();
			}
		}
	}

	public IRepositoryLocation getLocation() {
		return getActivePageData().getRepositoryLocation();
	}

	public boolean isSimpleMode() {
		return isSimpleMode;
	}

	public void setProjectsAndLocation(IProject[] projects, IRepositoryLocation location) {
		boolean multiProject = projects.length > 1;

		setTitle(SVNUIMessages.SelectProjectNamePage_Title);
		setDescription(SVNUIMessages.SelectProjectNamePage_Description);

		simpleModeComposite.setProjectsAndLocation(projects, location, multiProject);
		advancedModeComposite.setProjectsAndLocation(projects, location, multiProject);
	}

	public boolean isManagementFoldersEnabled() {
		return getActivePageData().isManagementFoldersEnabled();
	}

	public int getLayoutType() {
		return getActivePageData().getLayoutType();
	}

	public ShareProjectOperation.IFolderNameMapper getSelectedNames() {
		return getActivePageData().getSelectedNames();
	}

	public String getRootProjectName() {
		return getActivePageData().getRootProjectName();
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		//controls area
		ModeListener modeListener = new ModeListener();

		simpleModeRadionButton = new Button(composite, SWT.RADIO);
		simpleModeRadionButton.setText(SVNUIMessages.SelectProjectNamePage_SimpleModeButton);
		simpleModeRadionButton.setSelection(isSimpleMode);
		simpleModeRadionButton.addSelectionListener(modeListener);

		//simple mode controls
		IValidationManager simpleModeValidationManager = new SelectProjectNamePageValidationManager(this, true);
		simpleModeComposite = new SelectProjectNamePageSimpleModeComposite(composite, SWT.NONE,
				simpleModeValidationManager);
		layout = (GridLayout) simpleModeComposite.getLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		GridData gridData = (GridData) simpleModeComposite.getLayoutData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;

		//line separator
		Label label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.verticalIndent = 5;
		label.setLayoutData(gridData);

		advancedModeRadionButton = new Button(composite, SWT.RADIO);
		advancedModeRadionButton.setText(SVNUIMessages.SelectProjectNamePage_AdvancedModeButton);
		advancedModeRadionButton.setSelection(!isSimpleMode);
		layout = new GridLayout();
		gridData = new GridData();
		gridData.verticalIndent = 5;
		advancedModeRadionButton.setLayoutData(gridData);
		advancedModeRadionButton.addSelectionListener(modeListener);

		//advanced mode controls
		IValidationManager advancedModeValidationManager = new SelectProjectNamePageValidationManager(this, false);
		advancedModeComposite = new ShareProjectNameAdvancedModeComposite(composite, SWT.NONE,
				advancedModeValidationManager);
		layout = (GridLayout) advancedModeComposite.getLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		gridData = (GridData) advancedModeComposite.getLayoutData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;

		enableControlsArea();

		//Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, "org.eclipse.team.svn.help.projectNameContext"); //$NON-NLS-1$

		return composite;
	}

	protected void enableControlsArea() {
		if (isSimpleMode) {
			simpleModeComposite.setEnabled(true);
			advancedModeComposite.setEnabled(false);
		} else {
			simpleModeComposite.setEnabled(false);
			advancedModeComposite.setEnabled(true);
		}

		//update validators
		simpleModeComposite.validateContent();
		advancedModeComposite.validateContent();
	}

	protected ISelectProjectNamePageData getActivePageData() {
		return isSimpleMode ? simpleModeComposite : advancedModeComposite;
	}

	@Override
	public IWizardPage getNextPage() {
		getActivePageData().save();
		SVNTeamPreferences.setRepositoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.REPOSITORY_SIMPLE_SHARE_NAME, isSimpleMode);
		return super.getNextPage();
	}
}
