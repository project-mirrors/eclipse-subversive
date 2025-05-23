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
 *    Alexei Goncharov (Polarion Software) - Closing project with file in editor and reopening project generates NPE (bug 246147)
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.filtermatchers.AbstractFileInfoMatcher;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVN folder matcher, though it is not used by Subversive, it still could be used by some external plug-ins. Usage sample:
 * this.getProject().createFilter(IResourceFilterDescription.INHERITABLE | IResourceFilterDescription.FOLDERS, new
 * FileInfoMatcherDescription("org.eclipse.team.svn.core.svnmeta", null), IResource.DEPTH_INFINITE, new NullProgressMonitor());
 * 
 * @author Alexander Gurov
 */
public class SVNFileInfoMatcher extends AbstractFileInfoMatcher {

	@Override
	public boolean matches(IContainer parent, IFileInfo fileInfo) throws CoreException {
		return fileInfo.getName().equals(SVNUtility.getSVNFolderName());
	}

	@Override
	public void initialize(IProject project, Object arguments) throws CoreException {
	}

}
