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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.svn.core.operation.HiddenException;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * This interface provides an information about project connection to the repository
 * 
 * @deprecated use SVNTeamProvider directly instead
 * 
 * @author Alexander Gurov
 */
public interface IConnectedProjectInformation {
	
	public IRepositoryLocation getRepositoryLocation() throws HiddenException;
	
	public IRepositoryResource getRepositoryResource() throws HiddenException;
	
	public void switchResource(IRepositoryResource resource) throws CoreException;
	
	public void relocateResource() throws CoreException;
	
}
