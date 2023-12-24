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

package org.eclipse.team.svn.core.svnstorage;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.team.svn.core.resource.IResourceProvider;

public class ResourcesParentsProvider implements IResourceProvider {

	protected IResource[] resources;

	protected IResourceProvider provider;

	public ResourcesParentsProvider(IResource[] resources) {
		this.resources = resources;
	}

	public ResourcesParentsProvider(IResourceProvider provider) {
		this.provider = provider;
	}

	@Override
	public IResource[] getResources() {
		IResource[] resources = operableData();
		if (resources == null) {
			return new IResource[1];
		}
		HashSet resourcesWithParents = new HashSet(Arrays.asList(resources));
		for (IResource element : resources) {
			IResource parent = element.getParent();
			if (parent != null && !(parent instanceof IWorkspaceRoot)) {
				resourcesWithParents.add(parent);
			}
		}
		return (IResource[]) resourcesWithParents.toArray(new IResource[resourcesWithParents.size()]);
	}

	protected IResource[] operableData() {
		return resources == null ? provider.getResources() : resources;
	}

}
