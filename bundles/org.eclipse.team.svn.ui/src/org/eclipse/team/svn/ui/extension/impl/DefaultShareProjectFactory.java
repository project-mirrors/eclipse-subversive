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

package org.eclipse.team.svn.ui.extension.impl;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.ui.extension.factory.IShareProjectFactory;
import org.eclipse.team.svn.ui.wizard.shareproject.SelectProjectNamePage;

/**
 * Default implementation
 * 
 * @author Alexander Gurov
 */
public class DefaultShareProjectFactory implements IShareProjectFactory {

	public DefaultShareProjectFactory() {

	}

	public SelectProjectNamePage getProjectLayoutPage() {
		return new SelectProjectNamePage();
	}

	public ShareProjectOperation getShareProjectOperation(IProject[] projects, IRepositoryLocation location,
			SelectProjectNamePage page, String commitComment) {
		return new ShareProjectOperation(projects, location, page.getSelectedNames(), page.getRootProjectName(),
				page.getLayoutType(), page.isManagementFoldersEnabled(), commitComment);
	}

	public boolean disallowFinishOnAddRepositoryLocation(IProject[] projects) {
		return false;
	}

	public boolean disallowFinishOnCommitComment(IProject[] projects) {
		return false;
	}

	public boolean disallowFinishOnAlreadyConnected(IProject[] projects) {
		return false;
	}

	public boolean disallowFinishOnSelectRepositoryLocation(IProject[] projects) {
		return false;
	}

}
