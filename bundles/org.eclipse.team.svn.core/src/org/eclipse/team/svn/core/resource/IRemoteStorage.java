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
	public void addResourceStatesListener(Class eventClass, IResourceStatesListener listener);

	public void removeResourceStatesListener(Class eventClass, IResourceStatesListener listener);

	public void fireResourceStatesChangedEvent(ResourceStatesChangedEvent event);

	public IResourceChange asResourceChange(IChangeStateProvider changeState, boolean update);

	public ILocalResource asLocalResource(IResource resource);

	public ILocalResource asLocalResourceAccessible(IResource resource);

	public ILocalResource asLocalResourceDirty(IResource resource);

	public void refreshLocalResources(IResource[] resources, int depth);

	public IRepositoryResource asRepositoryResource(IRepositoryLocation location, String url, boolean isFile);

	public IRepositoryResource asRepositoryResource(IResource resource);

	public ILocalResource asLocalResource(IProject project, String url, int kind);

	public IRepositoryLocation getRepositoryLocation(IResource resource);

	public byte[] resourceChangeAsBytes(IResourceChange resource);

	public IResourceChange resourceChangeFromBytes(byte[] bytes);

}
