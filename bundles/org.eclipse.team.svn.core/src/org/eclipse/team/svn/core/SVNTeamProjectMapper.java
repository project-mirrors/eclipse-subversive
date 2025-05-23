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

package org.eclipse.team.svn.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Project-to-SVNTeamProvider mapper
 * 
 * @author Alexander Gurov
 */
public class SVNTeamProjectMapper {
	public static void map(final IProject project, IRepositoryResource resource) throws Exception {
		SVNTeamProvider.map(project, resource);
		// initially mark team private members
		ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) monitor -> FileUtility.findAndMarkSVNInternals(project, true), null, IWorkspace.AVOID_UPDATE, new NullProgressMonitor());
	}

}
