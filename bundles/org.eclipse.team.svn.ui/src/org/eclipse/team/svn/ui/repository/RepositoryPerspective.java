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

package org.eclipse.team.svn.ui.repository;

import org.eclipse.team.svn.ui.properties.PropertiesView;
import org.eclipse.team.svn.ui.repository.browser.RepositoryBrowser;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Repository view perspective
 * 
 * @author Alexander Gurov
 */
public class RepositoryPerspective implements IPerspectiveFactory {
	public static final String ID = RepositoryPerspective.class.getName();

	public RepositoryPerspective() {

	}

	@Override
	public void createInitialLayout(IPageLayout layout) {
		// Add new Repository Location wizard
		layout.addNewWizardShortcut("org.eclipse.team.svn.ui.wizard.NewRepositoryLocationWizard"); //$NON-NLS-1$

		// Add "show views". They will be present in "show view" menu
		layout.addShowViewShortcut(RepositoriesView.VIEW_ID);
		layout.addShowViewShortcut(RepositoryBrowser.VIEW_ID);
		layout.addShowViewShortcut(IHistoryView.VIEW_ID);
		layout.addShowViewShortcut(PropertiesView.VIEW_ID);

		// Add  "perspective short cut". They will be present in "open perspective" menu
		layout.addPerspectiveShortcut(RepositoryPerspective.ID);
		layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaPerspective"); //$NON-NLS-1$
		layout.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$
		layout.addPerspectiveShortcut("org.eclipse.team.ui.TeamSynchronizingPerspective"); //$NON-NLS-1$

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.26f, editorArea); //$NON-NLS-1$
		left.addView(RepositoriesView.VIEW_ID);

		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.66f, editorArea); //$NON-NLS-1$
		bottom.addView(RepositoryBrowser.VIEW_ID);
		bottom.addView(IHistoryView.VIEW_ID);
		bottom.addView(PropertiesView.VIEW_ID);
	}

}
