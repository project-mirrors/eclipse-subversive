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

package org.eclipse.team.svn.ui.wizard.checkoutas;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.DepthSelectionComposite;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourceNameVerifier;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Checkout method selection page
 * 
 * @author Alexander Gurov
 */
public class CheckoutMethodSelectionPage extends AbstractVerifiedWizardPage {
	protected static final int USE_NEW_PROJECT_WIZARD = 0;

	protected static final int FIND_PROJECTS = 1;

	protected static final int CHECKOUT_AS_PROJECT = 2;

	protected static final int CHECKOUT_AS_FOLDER = 3;

	protected String defaultName;

	protected Button selectLocationButton;

	protected Text nameField;

	protected RevisionComposite revisionComposite;

	protected DepthSelectionComposite depthSelector;

	protected String projectName;

	protected int checkoutType;

	protected IRepositoryResource resource;

	public CheckoutMethodSelectionPage(String defaultName, boolean newProjectSelectionEnabled,
			IRepositoryResource resource) {
		super(CheckoutMethodSelectionPage.class.getName(), SVNUIMessages.CheckoutMethodSelectionPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$

		setDescription(SVNUIMessages.CheckoutMethodSelectionPage_Description);

		projectName = this.defaultName = defaultName;
		checkoutType = newProjectSelectionEnabled
				? CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD
				: CheckoutMethodSelectionPage.CHECKOUT_AS_PROJECT;
		this.resource = resource;
	}

	public String getProjectName() {
		return projectName;
	}

	public boolean isUseNewProjectWizard() {
		return checkoutType == CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD;
	}

	public boolean isFindProjectsSelected() {
		return checkoutType == CheckoutMethodSelectionPage.FIND_PROJECTS;
	}

	public boolean isCheckoutAsFolderSelected() {
		return checkoutType == CheckoutMethodSelectionPage.CHECKOUT_AS_FOLDER;
	}

	public SVNDepth getdepth() {
		return depthSelector.getDepth();
	}

	public SVNRevision getSelectedRevision() {
		return revisionComposite.getSelectedRevision();
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		initializeDialogUnits(composite);

		// GridLayout
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);

		// GridData
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		setControl(composite);

		Label description = new Label(composite, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.heightHint = convertHeightInCharsToPixels(
				checkoutType == CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD ? 1 : 2);
		description.setLayoutData(data);
		String message = BaseMessages.format(SVNUIMessages.CheckoutMethodSelectionPage_HintHead,
				new String[] { defaultName });
		description.setText(message + (checkoutType != CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD
				? " " + SVNUIMessages.CheckoutMethodSelectionPage_HintTail //$NON-NLS-1$
				: "")); //$NON-NLS-1$
		projectName = defaultName = FileUtility.formatResourceName(defaultName);

		Button useNewProjectWizardButton = new Button(composite, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		useNewProjectWizardButton.setLayoutData(data);
		useNewProjectWizardButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CheckoutMethodSelectionPage.this.selectionChanged(CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD);
			}
		});
		useNewProjectWizardButton.setText(SVNUIMessages.CheckoutMethodSelectionPage_NewWizard);
		useNewProjectWizardButton.setSelection(checkoutType == CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD);
		useNewProjectWizardButton.setEnabled(checkoutType == CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD);

		Button findProjectsButton = new Button(composite, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		findProjectsButton.setLayoutData(data);
		findProjectsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CheckoutMethodSelectionPage.this.selectionChanged(CheckoutMethodSelectionPage.FIND_PROJECTS);
			}
		});
		findProjectsButton.setText(SVNUIMessages.CheckoutMethodSelectionPage_Find);
		findProjectsButton.setEnabled(checkoutType == CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD
				|| ExtensionsManager.getInstance().getCurrentCheckoutFactory().findProjectsOptionEnabled());

		Button checkoutAsFolder = new Button(composite, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		checkoutAsFolder.setLayoutData(data);
		checkoutAsFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CheckoutMethodSelectionPage.this.selectionChanged(CheckoutMethodSelectionPage.CHECKOUT_AS_FOLDER);
			}
		});
		checkoutAsFolder.setText(SVNUIMessages.CheckoutMethodSelectionPage_Folder);

		selectLocationButton = new Button(composite, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		selectLocationButton.setLayoutData(data);
		selectLocationButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CheckoutMethodSelectionPage.this.selectionChanged(CheckoutMethodSelectionPage.CHECKOUT_AS_PROJECT);
			}
		});
		selectLocationButton.setText(SVNUIMessages.CheckoutMethodSelectionPage_Project);
		selectLocationButton.setSelection(checkoutType != CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD);

		nameField = new Text(composite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		nameField.setLayoutData(data);
		nameField.setText(defaultName);
		nameField.setEnabled(checkoutType != CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD);
		nameField.addModifyListener(e -> {
			String name = nameField.getText().trim();
			projectName = name;
		});
		String name = SVNUIMessages.CheckoutMethodSelectionPage_ProjectName_Verifier;
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new ResourceNameVerifier(name, false));
		verifier.add(new NonEmptyFieldVerifier(name));
		attachTo(nameField, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return selectLocationButton.getSelection();
			}
		});

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.verticalIndent = 5;
		separator.setLayoutData(data);

		data = new GridData(GridData.FILL_HORIZONTAL);
		depthSelector = new DepthSelectionComposite(composite, SWT.NONE, false);
		depthSelector.setLayoutData(data);

		revisionComposite = new RevisionComposite(composite, this, false,
				new String[] { SVNUIMessages.RevisionComposite_Revision, SVNUIMessages.RevisionComposite_HeadRevision },
				SVNRevision.HEAD, false);
		data = new GridData(GridData.FILL_HORIZONTAL);
		revisionComposite.setLayoutData(data);
		revisionComposite.setSelectedResource(resource);

//		Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(composite, "org.eclipse.team.svn.help.checkoutMethodSelectionContext"); //$NON-NLS-1$

		return composite;
	}

	protected void selectionChanged(int newSelection) {
		checkoutType = newSelection;
		switch (checkoutType) {
			case CheckoutMethodSelectionPage.USE_NEW_PROJECT_WIZARD:
			case CheckoutMethodSelectionPage.FIND_PROJECTS:
			case CheckoutMethodSelectionPage.CHECKOUT_AS_FOLDER: {
				projectName = defaultName;
				nameField.setEnabled(false);
				break;
			}
			case CheckoutMethodSelectionPage.CHECKOUT_AS_PROJECT: {
				projectName = nameField.getText().trim();
				nameField.setEnabled(true);
				break;
			}
		}
		validateContent();
	}

}
