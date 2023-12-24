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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.mapping;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.core.subscribers.ChangeSet;

public class SVNChangeSetResourceMapping extends ResourceMapping {

	protected ChangeSet changeSet;

	public SVNChangeSetResourceMapping(ChangeSet changeSet) {
		this.changeSet = changeSet;
	}

	@Override
	public Object getModelObject() {
		return changeSet;
	}

	@Override
	public String getModelProviderId() {
		return SVNChangeSetModelProvider.ID;
	}

	@Override
	public IProject[] getProjects() {
		HashSet<IProject> projects = new HashSet<>();
		IResource[] resources = changeSet.getResources();
		for (IResource element : resources) {
			projects.add(element.getProject());
		}
		return projects.toArray(new IProject[projects.size()]);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor)
			throws CoreException {
		IResource[] resources = changeSet.getResources();
		if (resources.length == 0) {
			return new ResourceTraversal[0];
		}
		return new ResourceTraversal[] { new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE) };
	}
}
