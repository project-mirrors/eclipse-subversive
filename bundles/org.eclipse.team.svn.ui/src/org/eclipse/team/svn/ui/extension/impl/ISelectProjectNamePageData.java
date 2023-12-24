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

package org.eclipse.team.svn.ui.extension.impl;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * @author Igor Burilo
 *
 */
public interface ISelectProjectNamePageData {

	void setProjectsAndLocation(IProject []projects, IRepositoryLocation location, boolean multiProject);
	
	ShareProjectOperation.IFolderNameMapper getSelectedNames();
	
	String getRootProjectName();
	
	int getLayoutType();
	
	boolean isManagementFoldersEnabled();
	
	void validateContent();
	
	IRepositoryLocation getRepositoryLocation();
	
	void save();
}
