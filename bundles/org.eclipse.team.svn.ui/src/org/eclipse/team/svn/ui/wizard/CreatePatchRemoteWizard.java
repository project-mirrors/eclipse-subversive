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

package org.eclipse.team.svn.ui.wizard;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.wizard.createpatch.SelectRepositoryResourcePage;

/**
 * Create patch for repository resource
 * 
 * @author Alexander Gurov
 */
public class CreatePatchRemoteWizard extends CreatePatchWizard {
	protected SelectRepositoryResourcePage urlSelectionPage;

	protected IRepositoryResource targetResource;

	public CreatePatchRemoteWizard(IRepositoryResource targetResource) {
		this(targetResource, false);
	}

	public CreatePatchRemoteWizard(IRepositoryResource targetResource, boolean showIgnoreAncestry) {
		super(targetResource.getName(), null, showIgnoreAncestry);
		this.targetResource = targetResource;
	}

	public IRepositoryResource getSelectedResource() {
		return urlSelectionPage.getSelectedResource();
	}

	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage instanceof SelectRepositoryResourcePage) {
			return false;
		}
		return super.canFinish();
	}

	@Override
	public void addPages() {
		addPage(urlSelectionPage = new SelectRepositoryResourcePage(targetResource));
		super.addPages();
	}

}
