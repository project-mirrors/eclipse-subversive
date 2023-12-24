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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.properties.ResourcePropertyEditPanel;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;

/**
 * Apply property method selection composite
 *
 * @author Sergiy Logvin
 */
public class ApplyPropertyMethodComposite extends Composite {
	protected static final String PROPERTY_FILTER_HISTORY_NAME = "propertyFilter"; //$NON-NLS-1$

	protected Button applyToAllResourcesButton;

	protected Button applyToFilesButton;

	protected Button applyToFoldersButton;

	protected Button useFilterButton;

	protected Label filterLabel;

	protected Combo filterMaskText;

	protected boolean applyToFiles;

	protected boolean applyToFolders;

	protected boolean useMask;

	protected int resourcesType;

	protected String filterMask;

	protected UserInputHistory propertyFilterHistory;

	protected IValidationManager validationManager;

	public ApplyPropertyMethodComposite(Composite parent, int style, IValidationManager validationManager,
			int resourcesType) {
		super(parent, style);
		this.validationManager = validationManager;
		this.resourcesType = resourcesType;
		createControls();
	}

	public String getFilterMask() {
		return filterMask;
	}

	public boolean useMask() {
		return useMask;
	}

	public int getApplyMethod() {
		if (applyToFiles) {
			return PropertiesComposite.APPLY_TO_FILES;
		} else if (applyToFolders) {
			return PropertiesComposite.APPLY_TO_FOLDERS;
		}
		return PropertiesComposite.APPLY_TO_ALL;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (resourcesType == ResourcePropertyEditPanel.MIXED_RESOURCES) {
			applyToAllResourcesButton.setEnabled(enabled);
			applyToFilesButton.setEnabled(enabled);
			applyToFoldersButton.setEnabled(enabled);
			useFilterButton.setEnabled(enabled);
		}
		filterMaskText.setEnabled(enabled && useFilterButton.getSelection());
		filterLabel.setEnabled(enabled && useFilterButton.getSelection());
	}

	public void saveChanges() {
		if (resourcesType == ResourcePropertyEditPanel.MIXED_RESOURCES) {
			applyToFiles = applyToFilesButton.getSelection();
			applyToFolders = applyToFoldersButton.getSelection();
		}
		filterMask = filterMaskText.getText();
		useMask = useFilterButton.getSelection() && useFilterButton.isEnabled();
		if (useMask) {
			propertyFilterHistory.addLine(filterMask);
		}
	}

	protected void createControls() {
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);
		GridData data = null;

		Group group = new Group(this, SWT.NONE);
		layout = new GridLayout();
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		group.setText(SVNUIMessages.ApplyPropertyMethodComposite_ResourcesOptions);

		if (resourcesType == ResourcePropertyEditPanel.MIXED_RESOURCES) {
			applyToAllResourcesButton = new Button(group, SWT.RADIO);
			applyToAllResourcesButton.setLayoutData(new GridData());
			applyToAllResourcesButton.setText(SVNUIMessages.ApplyPropertyMethodComposite_AllResources);
			applyToAllResourcesButton.setSelection(true);

			applyToFilesButton = new Button(group, SWT.RADIO);
			applyToFilesButton.setLayoutData(new GridData());
			applyToFilesButton.setText(SVNUIMessages.ApplyPropertyMethodComposite_FilesOnly);

			applyToFoldersButton = new Button(group, SWT.RADIO);
			applyToFoldersButton.setLayoutData(new GridData());
			applyToFoldersButton.setText(SVNUIMessages.ApplyPropertyMethodComposite_FoldersOnly);
		}

		Composite filter = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.makeColumnsEqualWidth = true;
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		filter.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		filter.setLayoutData(data);

		useFilterButton = new Button(filter, SWT.CHECK);
		useFilterButton.setText(SVNUIMessages.ApplyPropertyMethodComposite_ResourceNameFiltration);
		useFilterButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		useFilterButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ApplyPropertyMethodComposite.this.setEnabled(true);
				validationManager.validateContent();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Composite subFilter = new Composite(filter, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		subFilter.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		subFilter.setLayoutData(data);

		filterLabel = new Label(subFilter, SWT.NONE);
		filterLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		filterLabel.setText(SVNUIMessages.ApplyPropertyMethodComposite_Mask);

		propertyFilterHistory = new UserInputHistory(ApplyPropertyMethodComposite.PROPERTY_FILTER_HISTORY_NAME);

		filterMaskText = new Combo(subFilter, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		filterMaskText.setEnabled(useFilterButton.getSelection());
		filterMaskText.setLayoutData(data);
		filterMaskText.setVisibleItemCount(propertyFilterHistory.getDepth());
		filterMaskText.setItems(propertyFilterHistory.getHistory());
		validationManager.attachTo(filterMaskText, new AbstractVerifierProxy(
				new NonEmptyFieldVerifier(SVNUIMessages.ApplyPropertyMethodComposite_Mask_Verifier)) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return useFilterButton.getSelection() && useFilterButton.isEnabled();
			}
		});
		filterMaskText.setText("*"); //$NON-NLS-1$
	}

}
