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

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.wizard.createpatch.PatchOptionsPage;
import org.eclipse.team.svn.ui.wizard.createpatch.SelectPatchFilePage;

/**
 * Create patch file wizard
 * 
 * @author Alexander Gurov
 */
public class CreatePatchWizard extends AbstractSVNWizard {
	public static final int WRITE_TO_CLIPBOARD = 0;

	public static final int WRITE_TO_EXTERNAL_FILE = 1;

	public static final int WRITE_TO_WORKSPACE_FILE = 2;

	protected String targetName;

	protected boolean showIgnoreAncestry;

	protected SelectPatchFilePage selectFile;

	protected PatchOptionsPage options;

	protected IResource[] roots;

	public CreatePatchWizard(String targetName) {
		this(targetName, null);
	}

	public CreatePatchWizard(String targetName, IResource[] roots) {
		this(targetName, roots, false);
	}

	public CreatePatchWizard(String targetName, IResource[] roots, boolean showIgnoreAncestry) {
		setWindowTitle(SVNUIMessages.CreatePatchWizard_Title);
		this.targetName = targetName;
		this.roots = roots;
		this.showIgnoreAncestry = showIgnoreAncestry;
	}

	public String getCharset() {
		return selectFile.getCharset();
	}

	public int getRootPoint() {
		return options.getRootPoint();
	}

	public IResource getTargetFolder() {
		if (selectFile.getFile() != null) {
			return selectFile.getFile().getParent();
		}
		return ResourcesPlugin.getWorkspace()
				.getRoot()
				.getContainerForLocation(new Path(getFileName()).removeLastSegments(1));
	}

	public String getFileName() {
		return selectFile.getFileName();
	}

	public int getWriteMode() {
		return selectFile.getWriteMode();
	}

	public boolean isIgnoreDeleted() {
		return options.isIgnoreDeleted();
	}

	public boolean isProcessBinary() {
		return options.isProcessBinary();
	}

	public boolean isProcessUnversioned() {
		return options.isProcessUnversioned();
	}

	public boolean isRecursive() {
		return options.isRecursive() & selectFile.isRecursive();
	}

	public long getDiffOptions() {
		return (isIgnoreAncestry() ? ISVNConnector.Options.IGNORE_ANCESTRY : ISVNConnector.Options.NONE)
				| (isIgnoreDeleted() ? ISVNConnector.Options.SKIP_DELETED : ISVNConnector.Options.NONE)
				| (isProcessBinary() ? ISVNConnector.Options.FORCE : ISVNConnector.Options.NONE);
	}

	public long getDiffOutputOptions() {
		return options.getDiffOutputOptions();
	}

	public IResource[] getSelection() {
		return selectFile.isRecursive() ? roots : selectFile.getSelection();
	}

	public boolean isIgnoreAncestry() {
		return options.isIgnoreAncestry();
	}

	@Override
	public void addPages() {
		addPage(selectFile = new SelectPatchFilePage(targetName, roots));
		addPage(options = new PatchOptionsPage(roots != null, showIgnoreAncestry));
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (roots != null) {
			HashSet<IProject> projects = new HashSet<>();
			for (IResource resource : getSelection()) {
				projects.add(resource.getProject());
			}
			options.setMultiSelect(projects.size() > 1);
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
