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

package org.eclipse.team.svn.core.operation;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * This interface allows us to redefine property editor behaviour depending on property source
 * 
 * @author Alexander Gurov
 */
public interface IResourcePropertyProvider extends IActionOperation {
	public IResource getLocal();

	public IRepositoryResource getRemote();

	public SVNProperty[] getProperties();

	public boolean isEditAllowed();

	public void refresh();
}
