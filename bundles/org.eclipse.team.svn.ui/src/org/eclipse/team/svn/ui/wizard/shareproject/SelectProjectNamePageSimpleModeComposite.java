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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RepositoryResourceOnlySelectionComposite;
import org.eclipse.team.svn.ui.extension.impl.ISelectProjectNamePageData;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * @author Igor Burilo
 *
 */
public class SelectProjectNamePageSimpleModeComposite extends Composite implements ISelectProjectNamePageData {

	protected RepositoryResourceOnlySelectionComposite resourceSelectionComposite;

	protected IValidationManager validationManager;

	protected IRepositoryLocation location;

	protected boolean multiProject;

	protected List<Control> controls = new ArrayList<>();

	public SelectProjectNamePageSimpleModeComposite(Composite parent, int style, IValidationManager validationManager) {
		super(parent, style);
		this.validationManager = validationManager;
		createControls();
	}

	@Override
	public void setProjectsAndLocation(IProject[] projects, IRepositoryLocation location, boolean multiProject) {
		this.location = location;
		this.multiProject = multiProject;

		/*
		 * Set picker URL as repository location plus project name
		 */
		IRepositoryResource baseResource = location.asRepositoryContainer(location.getUrl(), false);
		resourceSelectionComposite.setBaseResource(baseResource);
		resourceSelectionComposite.setMatchToBaseResource(true);

		IProject pr = projects[0];
		String url = baseResource.getUrl();
		if (!this.multiProject) {
			url += "/" + pr.getName(); //$NON-NLS-1$
		}
		resourceSelectionComposite.setUrl(url);
	}

	protected void createControls() {
		GridLayout layout = new GridLayout();
		setLayout(layout);
		GridData gridData = new GridData();
		setLayoutData(gridData);

		IRepositoryResource baseResource = null;

		resourceSelectionComposite = new RepositoryResourceOnlySelectionComposite(
				this, SWT.NONE, validationManager, "selectProjectNamePage", //$NON-NLS-1$
				baseResource, SVNUIMessages.SelectProjectNamePage_Select_Title,
				SVNUIMessages.SelectProjectNamePage_Select_Description);

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 550;
		resourceSelectionComposite.setLayoutData(data);
		controls.add(resourceSelectionComposite);
	}

	@Override
	public ShareProjectOperation.IFolderNameMapper getSelectedNames() {
		return project -> {
			String folderName = null;

			String toTrim = location.getUrl();
			String selectedUrl = resourceSelectionComposite.getSelectedResource().getUrl();
			selectedUrl = SVNUtility.normalizeURL(selectedUrl);
			if (selectedUrl.startsWith(toTrim)) {
				folderName = selectedUrl.equals(toTrim) ? "" : selectedUrl.substring(toTrim.length() + 1); //$NON-NLS-1$
				if (multiProject) {
					folderName += "/" + project.getName(); //$NON-NLS-1$
				}
			} else {
				throw new RuntimeException("Inconsistent repository location and selected repository url. "
						+ "Selected url: " + selectedUrl + ", repository location: " + toTrim);
			}

			return folderName;
		};
	}

	@Override
	public String getRootProjectName() {
		return null;
	}

	@Override
	public void save() {
		resourceSelectionComposite.saveHistory();
	}

	@Override
	public int getLayoutType() {
		return ShareProjectOperation.LAYOUT_DEFAULT;
	}

	@Override
	public boolean isManagementFoldersEnabled() {
		return false;
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
				control.setEnabled(enabled);
			}
		}
	}

}
