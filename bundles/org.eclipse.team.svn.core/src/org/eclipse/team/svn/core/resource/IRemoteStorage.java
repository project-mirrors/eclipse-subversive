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

package org.eclipse.team.svn.core.resource;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;

/**
 * Remote storage interace
 * 
 * @author Alexander Gurov
 */
public interface IRemoteStorage extends ISVNStorage {
	void addResourceStatesListener(Class eventClass, IResourceStatesListener listener);

	void removeResourceStatesListener(Class eventClass, IResourceStatesListener listener);

	void fireResourceStatesChangedEvent(ResourceStatesChangedEvent event);

	IResourceChange asResourceChange(IChangeStateProvider changeState, boolean update);

	ILocalResource asLocalResource(IResource resource);

	ILocalResource asLocalResourceAccessible(IResource resource);

	ILocalResource asLocalResourceDirty(IResource resource);

	void refreshLocalResources(IResource[] resources, int depth);

	IRepositoryResource asRepositoryResource(IRepositoryLocation location, String url, boolean isFile);

	IRepositoryResource asRepositoryResource(IResource resource);

	ILocalResource asLocalResource(IProject project, String url, int kind);

	IRepositoryLocation getRepositoryLocation(IResource resource);

	byte[] resourceChangeAsBytes(IResourceChange resource);

	IResourceChange resourceChangeFromBytes(byte[] bytes);

}
