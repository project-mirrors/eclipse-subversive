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

package org.eclipse.team.svn.ui.extension.factory;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.ui.wizard.shareproject.SelectProjectNamePage;

/**
 * Share project wizard factory
 * 
 * @author Alexander Gurov
 */
public interface IShareProjectFactory {
	/**
	 * The method provides ShareProjectWizard page with some extended options in compare to default Subversive implementation
	 * 
	 * @param project
	 *            the project which will be shared
	 * @return wizard page
	 */
	SelectProjectNamePage getProjectLayoutPage();

	/**
	 * Allows to override default Subversive behavior while sharing the project
	 * 
	 * @param projects
	 *            the projects which will be shared
	 * @param location
	 *            the repository location which will be used in order to share the project
	 * @param page
	 *            advanced share project configuration page
	 * @return share project operation implementation which overrides default Subversive behavior
	 */
	ShareProjectOperation getShareProjectOperation(IProject[] projects, IRepositoryLocation location,
			SelectProjectNamePage page, String commitMessage);

	/**
	 * Force disablement of the finish button on the "Commit Comment" page depending on project
	 * 
	 * @param project
	 *            the project which will be shared
	 * @return true if should be disallowed
	 */
	boolean disallowFinishOnCommitComment(IProject[] projects);

	/**
	 * Force disablement of the finish button on the "Already Connected" page depending on project
	 * 
	 * @param project
	 *            the project which will be shared
	 * @return true if should be disallowed
	 */
	boolean disallowFinishOnAlreadyConnected(IProject[] projects);

	/**
	 * Force disablement of the finish button on the "Add Repository Location" page depending on project
	 * 
	 * @param project
	 *            the project which will be shared
	 * @return true if should be disallowed
	 */
	boolean disallowFinishOnAddRepositoryLocation(IProject[] projects);

	/**
	 * Force disablement of the finish button on the "Select Repository Location" page depending on project
	 * 
	 * @param project
	 *            the project which will be shared
	 * @return true if should be disallowed
	 */
	boolean disallowFinishOnSelectRepositoryLocation(IProject[] projects);
}
