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

package org.eclipse.team.svn.ui.properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.extension.properties.PredefinedProperty;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.ApplyPropertyMethodComposite;
import org.eclipse.team.svn.ui.composite.PropertiesComposite;

/**
 * Edit property panel
 * 
 * @author Sergiy Logvin
 */
public class ResourcePropertyEditPanel extends AbstractPropertyEditPanel {
	public static int SINGLE_FILE = 0;

	public static int MULTIPLE_FILES = 1;

	public static int MIXED_RESOURCES = 2;

	protected Button recursiveButton;

	protected ApplyPropertyMethodComposite applyComposite;

	protected int resourcesType;

	protected boolean recursiveSelected;

	protected boolean applyToAll;

	protected boolean applyToFiles;

	protected boolean applyToFolders;

	protected IResource[] selectedResources;

	protected boolean strict;

	protected int mask;

	public ResourcePropertyEditPanel(SVNProperty[] data, IResource[] selectedResources, boolean strict) {
		super(data,
				data != null ? SVNUIMessages.PropertyEditPanel_Title_Edit : SVNUIMessages.PropertyEditPanel_Title_Add,
				SVNUIMessages.PropertyEditPanel_Description);
		this.strict = strict;
		this.selectedResources = selectedResources;
		resourcesType = computeResourcesType();
		mask = PredefinedProperty.TYPE_NONE;
		for (IResource resource : this.selectedResources) {
			if (resource.getType() == IResource.FOLDER || resource.getType() == IResource.PROJECT) {
				mask |= PredefinedProperty.TYPE_COMMON;
			} else if (resource.getType() == IResource.FILE) {
				mask |= PredefinedProperty.TYPE_FILE;
			}
		}
		fillVerifiersMap();
	}

	@Override
	protected boolean isPropertyAccepted(PredefinedProperty property) {
		// is there any properties that could be used for both: revisions and resources?
		return (property.type & PredefinedProperty.TYPE_REVISION) == PredefinedProperty.TYPE_NONE
				&& (property.type & mask) != PredefinedProperty.TYPE_NONE;
	}

	@Override
	protected IRepositoryResource getRepostioryResource() {
		return SVNRemoteStorage.instance().asRepositoryResource(selectedResources[0]);
	}

	public boolean isStrict() {
		return strict;
	}

	public boolean isRecursiveSelected() {
		return recursiveSelected;
	}

	public int getApplyMethod() {
		return applyComposite == null ? PropertiesComposite.APPLY_TO_ALL : applyComposite.getApplyMethod();
	}

	public String getFilterMask() {
		return applyComposite == null ? "" : applyComposite.getFilterMask(); //$NON-NLS-1$
	}

	public boolean useMask() {
		return applyComposite == null ? false : applyComposite.useMask();
	}

	@Override
	public void createControlsImpl(Composite parent) {
		super.createControlsImpl(parent);
		if (resourcesType != ResourcePropertyEditPanel.SINGLE_FILE) {
			if (resourcesType == ResourcePropertyEditPanel.MIXED_RESOURCES && !strict) {
				recursiveButton = new Button(parent, SWT.CHECK);
				recursiveButton.setText(SVNUIMessages.PropertyEditPanel_Recursively);

				recursiveButton.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						ResourcePropertyEditPanel.this.refreshControlsEnablement();
						ResourcePropertyEditPanel.this.validateContent();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
			}
			applyComposite = new ApplyPropertyMethodComposite(parent, SWT.NONE, this, resourcesType);
			applyComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		if (resourcesType == ResourcePropertyEditPanel.MIXED_RESOURCES && !strict) {
			refreshControlsEnablement();
		}
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.setPropsDialogContext"; //$NON-NLS-1$
	}

	protected int computeResourcesType() {
		boolean singleResource = selectedResources.length == 1;
		boolean allFiles = true;
		for (IResource element : selectedResources) {
			if (!(element instanceof IFile)) {
				allFiles = false;
				break;
			}
		}
		if (allFiles) {
			return singleResource ? ResourcePropertyEditPanel.SINGLE_FILE : ResourcePropertyEditPanel.MULTIPLE_FILES;
		}
		return ResourcePropertyEditPanel.MIXED_RESOURCES;
	}

	@Override
	protected void saveChangesImpl() {
		super.saveChangesImpl();
		if (resourcesType != ResourcePropertyEditPanel.SINGLE_FILE) {
			if (resourcesType == ResourcePropertyEditPanel.MIXED_RESOURCES && !strict) {
				recursiveSelected = recursiveButton.getSelection();
			}
			if (applyComposite.isEnabled()) {
				applyComposite.saveChanges();
			}
		}
	}

	protected void refreshControlsEnablement() {
		applyComposite.setEnabled(recursiveButton.getSelection());
	}

	@Override
	protected Point getPrefferedSizeImpl() {
		return new Point(590, SWT.DEFAULT);
	}
}
