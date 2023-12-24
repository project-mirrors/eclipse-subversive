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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.wizard.shareproject;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.extension.impl.ISelectProjectNamePageData;
import org.eclipse.team.svn.ui.verifier.AbsolutePathVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourceNameVerifier;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * @author Igor Burilo
 *
 */
public class ShareProjectNameAdvancedModeComposite extends Composite implements ISelectProjectNamePageData {

	protected int layoutType;

	protected String rootProjectName;

	protected String projectName;

	protected String selectedName;

	protected boolean managementFoldersEnabled;

	protected Text projectNameField;

	protected Text rootProjectNameField;

	protected Label targetUrlField;

	protected Button managementFoldersEnabledButton;

	protected IRepositoryLocation location;

	protected boolean multiProject;

	protected Button defaultLayoutButton;

	protected Button singleLayoutButton;

	protected Group nameGroup;

	protected Group layoutGroup;

	protected Point savedPosition;

	protected Button multipleLayoutButton;

	protected Button useEmptyNameButton;

	protected IValidationManager validationManager;

	protected List<Control> controls = new ArrayList<>();

	public ShareProjectNameAdvancedModeComposite(Composite parent, int style, IValidationManager validationManager) {
		super(parent, style);
		this.validationManager = validationManager;
		layoutType = ShareProjectOperation.LAYOUT_DEFAULT;
		createControls();
	}

	protected void createControls() {
		GridLayout layout = new GridLayout();

		setLayout(layout);
		GridData gridData = new GridData();
		setLayoutData(gridData);

		CompositeVerifier verifier;

		nameGroup = new Group(this, SWT.NONE);
		layout = new GridLayout();
		nameGroup.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		nameGroup.setLayoutData(data);
		nameGroup.setText(SVNUIMessages.SelectProjectNamePage_NameOnRepository);
		controls.add(nameGroup);

		Button useProjectNameButton = new Button(nameGroup, SWT.RADIO);
		useProjectNameButton.setLayoutData(makeGridData());
		useProjectNameButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validationManager.validateContent();
				Button button = (Button) e.widget;
				if (button.getSelection()) {
					selectedName = projectName;
					//ShareProjectNameAdvancedModeComposite.this.setPageComplete(true);
				}
				ShareProjectNameAdvancedModeComposite.this.showTargetUrl();
			}
		});
		useProjectNameButton.setText(SVNUIMessages.SelectProjectNamePage_UseProjectName);
		useProjectNameButton.setSelection(true);
		controls.add(useProjectNameButton);

		useEmptyNameButton = new Button(nameGroup, SWT.RADIO);
		useEmptyNameButton.setLayoutData(makeGridData());
		useEmptyNameButton.setText(SVNUIMessages.SelectProjectNamePage_UseEmptyName);
		useEmptyNameButton.setSelection(false);
		controls.add(useEmptyNameButton);

		final Button useRedefinedNameButton = new Button(nameGroup, SWT.RADIO);
		useRedefinedNameButton.setLayoutData(makeGridData());
		useRedefinedNameButton.setText(SVNUIMessages.SelectProjectNamePage_UseSpecifiedName);
		useRedefinedNameButton.setSelection(false);
		useRedefinedNameButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validationManager.validateContent();
				Button button = (Button) e.widget;
				if (button.getSelection()) {
					selectedName = projectNameField.getText();
					projectNameField.setEditable(true);
				} else {
					projectNameField.setEditable(false);
				}
				ShareProjectNameAdvancedModeComposite.this.showTargetUrl();
			}
		});
		controls.add(useRedefinedNameButton);

		projectNameField = new Text(nameGroup, SWT.SINGLE | SWT.BORDER);
		projectNameField.setLayoutData(makeGridData());
		projectNameField.setEditable(false);
		projectNameField.addModifyListener(e -> {
			selectedName = projectNameField.getText();
			ShareProjectNameAdvancedModeComposite.this.showTargetUrl();
		});
		verifier = new CompositeVerifier();
		String name = SVNUIMessages.SelectProjectNamePage_ProjectName;
		verifier.add(new NonEmptyFieldVerifier(name));
		verifier.add(new ResourceNameVerifier(name, true));
		verifier.add(new AbsolutePathVerifier(name));
		validationManager.attachTo(projectNameField, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return useRedefinedNameButton.getSelection();
			}
		});
		controls.add(projectNameField);

		layoutGroup = new Group(this, SWT.NONE);
		layout = new GridLayout();
		layoutGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		layoutGroup.setLayoutData(data);
		layoutGroup.setText(SVNUIMessages.SelectProjectNamePage_ProjectLayout);
		controls.add(layoutGroup);

		defaultLayoutButton = new Button(layoutGroup, SWT.RADIO);
		data = makeGridData();
		defaultLayoutButton.setLayoutData(data);
		defaultLayoutButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
				if (button.getSelection()) {
					layoutType = ShareProjectOperation.LAYOUT_DEFAULT;
				}
				validationManager.validateContent();
				ShareProjectNameAdvancedModeComposite.this.showTargetUrl();
				ShareProjectNameAdvancedModeComposite.this.enableManagementFoldersEnabledButton();
			}
		});
		defaultLayoutButton.setText(SVNUIMessages.SelectProjectNamePage_RepositoryLocationLayout);
		defaultLayoutButton.setSelection(true);
		controls.add(defaultLayoutButton);

		singleLayoutButton = new Button(layoutGroup, SWT.RADIO);
		data = makeGridData();
		singleLayoutButton.setLayoutData(data);
		singleLayoutButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
				if (button.getSelection()) {
					layoutType = ShareProjectOperation.LAYOUT_SINGLE;
				}
				validationManager.validateContent();
				ShareProjectNameAdvancedModeComposite.this.showTargetUrl();
				ShareProjectNameAdvancedModeComposite.this.enableManagementFoldersEnabledButton();
			}
		});
		singleLayoutButton.setText(SVNUIMessages.SelectProjectNamePage_SingleProjectLayout);
		controls.add(singleLayoutButton);

		multipleLayoutButton = new Button(layoutGroup, SWT.RADIO);
		data = makeGridData();
		multipleLayoutButton.setLayoutData(data);
		multipleLayoutButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
				if (button.getSelection()) {
					layoutType = ShareProjectOperation.LAYOUT_MULTIPLE;
					rootProjectName = rootProjectNameField.getText();
				}
				rootProjectNameField.setEditable(button.getSelection());
				validationManager.validateContent();
				ShareProjectNameAdvancedModeComposite.this.showTargetUrl();
				ShareProjectNameAdvancedModeComposite.this.enableManagementFoldersEnabledButton();
			}
		});
		multipleLayoutButton.setText(SVNUIMessages.SelectProjectNamePage_MultiProjectLayout);
		controls.add(multipleLayoutButton);

		rootProjectNameField = new Text(layoutGroup, SWT.SINGLE | SWT.BORDER);
		rootProjectNameField.setLayoutData(makeGridData());
		rootProjectNameField.setEditable(false);
		rootProjectNameField.addModifyListener(e -> {
			rootProjectName = rootProjectNameField.getText();
			ShareProjectNameAdvancedModeComposite.this.showTargetUrl();
		});
		verifier = new CompositeVerifier();
		name = SVNUIMessages.SelectProjectNamePage_RootName;
		verifier.add(new NonEmptyFieldVerifier(name));
		verifier.add(new ResourceNameVerifier(name, true));
		verifier.add(new AbsolutePathVerifier(name));
		validationManager.attachTo(rootProjectNameField, new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return multipleLayoutButton.getSelection();
			}
		});
		controls.add(rootProjectNameField);

		Label label = new Label(layoutGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(makeGridData());
		controls.add(label);

		managementFoldersEnabledButton = new Button(layoutGroup, SWT.CHECK);
		managementFoldersEnabledButton.setLayoutData(new GridData());
		managementFoldersEnabledButton.setSelection(true);
		managementFoldersEnabled = true;
		managementFoldersEnabledButton.setText(SVNUIMessages.SelectProjectNamePage_UseRecommended);
		managementFoldersEnabledButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				managementFoldersEnabled = ((Button) e.widget).getSelection();
				ShareProjectNameAdvancedModeComposite.this.showTargetUrl();
			}
		});
		controls.add(managementFoldersEnabledButton);

		label = new Label(layoutGroup, SWT.WRAP);
		data = makeGridData();
		data.heightHint = DefaultDialog.convertHeightInCharsToPixels(this, 2);
		//this.convertHeightInCharsToPixels(2);
		label.setLayoutData(data);
		label.setText(SVNUIMessages.SelectProjectNamePage_Hint);
		controls.add(label);

		Composite urlComposite = new Composite(layoutGroup, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 2;
		urlComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		urlComposite.setLayoutData(data);
		controls.add(urlComposite);

		label = new Label(urlComposite, SWT.NONE);
		label.setLayoutData(new GridData());
		label.setImage(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER)
				.createImage());
		controls.add(label);

		targetUrlField = new Label(urlComposite, SWT.SINGLE);
		targetUrlField.setLayoutData(makeGridData());
		projectNameField.setEditable(true);
		targetUrlField.setBackground(projectNameField.getBackground());
		controls.add(targetUrlField);

		label.setBackground(projectNameField.getBackground());
		urlComposite.setBackground(projectNameField.getBackground());
		projectNameField.setEditable(false);

		useEmptyNameButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validationManager.validateContent();
				selectedName = ""; //$NON-NLS-1$
				Button button = (Button) e.widget;

				if (button.getSelection()) {
					if (multipleLayoutButton.getSelection()) {
						multipleLayoutButton.setSelection(false);
						rootProjectNameField.setEditable(false);
						defaultLayoutButton.setSelection(true);
					}
				}

				ShareProjectNameAdvancedModeComposite.this.showTargetUrl();
				ShareProjectNameAdvancedModeComposite.this.enableMultipleLayoutButton();
				ShareProjectNameAdvancedModeComposite.this.enableManagementFoldersEnabledButton();
			}
		});
	}

	@Override
	public void setProjectsAndLocation(IProject[] projects, IRepositoryLocation location, boolean multiProject) {
		this.multiProject = multiProject;
		this.location = location;

		if (this.multiProject) {
			selectedName = projectName = ""; //$NON-NLS-1$
			savedPosition = layoutGroup.getLocation();
			layoutGroup.setLocation(nameGroup.getLocation());
			nameGroup.setVisible(false);
			defaultLayoutButton.setSelection(true);
		} else {
			selectedName = projectName = projects[0].getName();
			if (savedPosition != null) {
				nameGroup.setVisible(true);
				layoutGroup.setLocation(savedPosition);
				savedPosition = null;
			}
		}

		projectNameField.setText(projectName);
		rootProjectNameField.setText(projectName);
		showTargetUrl();
	}

	protected GridData makeGridData() {
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.horizontalSpan = 1;
		return data;
	}

	protected void showTargetUrl() {
		String targetUrl = ShareProjectOperation.getTargetUrl(location, layoutType, selectedName, rootProjectName,
				isManagementFoldersEnabled());
		showTargetUrlImpl(targetUrl);
	}

	protected void showTargetUrlImpl(String targetUrl) {
		if (targetUrlField != null) {
			targetUrlField.setText(targetUrl);
		}
	}

	@Override
	public boolean isManagementFoldersEnabled() {
		return layoutType == ShareProjectOperation.LAYOUT_DEFAULT ? true : managementFoldersEnabled;
	}

	@Override
	public int getLayoutType() {
		return layoutType;
	}

	@Override
	public ShareProjectOperation.IFolderNameMapper getSelectedNames() {
		return multiProject ? null : project -> selectedName;
	}

	@Override
	public String getRootProjectName() {
		return rootProjectName;
	}

	public boolean isMultiProject() {
		return multiProject;
	}

	@Override
	public void save() {
		//do nothing
	}

	@Override
	public void validateContent() {
		validationManager.validateContent();
	}

	@Override
	public IRepositoryLocation getRepositoryLocation() {
		return location;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (controls != null && !controls.isEmpty()) {
			for (Control control : controls) {
				if (enabled) {
					/*
					 * Handle special cases for enabling controls as
					 * there are dependencies among controls for enabling/disabling
					 */
					if (control == multipleLayoutButton) {
						enableMultipleLayoutButton();
					} else if (control == managementFoldersEnabledButton) {
						enableManagementFoldersEnabledButton();
					} else if (control == singleLayoutButton) {
						enableSingleLayoutButton();
					} else {
						control.setEnabled(enabled);
					}
				} else {
					control.setEnabled(enabled);
				}
			}
		}
	}

	protected void enableMultipleLayoutButton() {
		multipleLayoutButton.setEnabled(!useEmptyNameButton.getSelection());
	}

	protected void enableSingleLayoutButton() {
		singleLayoutButton.setEnabled(!multiProject);
	}

	protected void enableManagementFoldersEnabledButton() {
		if (defaultLayoutButton.getSelection()) {
			managementFoldersEnabledButton.setEnabled(false);
		} else if (singleLayoutButton.getSelection()) {
			managementFoldersEnabledButton.setEnabled(true);
		} else if (multipleLayoutButton.getSelection()) {
			managementFoldersEnabledButton.setEnabled(true);
		}

		if (useEmptyNameButton.getSelection() && multipleLayoutButton.getSelection()) {
			managementFoldersEnabledButton.setEnabled(false);
		}
	}
}
