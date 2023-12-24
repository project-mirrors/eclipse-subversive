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
 *    Andrey Loskutov - [scalability] SVN update takes hours if "Synchronize" view is opened
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.resource.events;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.IQueuedElement;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Basic "resources changed event" implementation
 * 
 * @author Alexander Gurov
 */
public class ResourceStatesChangedEvent implements IQueuedElement<ResourceStatesChangedEvent> {
	public static final int CHANGED_NODES = 0;

	public static final int PATH_NODES = 1;

	public final IResource[] resources;

	public final int depth;

	public final int type;

	private IResource[] fullSet;

	public ResourceStatesChangedEvent(IResource[] resources, int depth, int type) {
		// notify in parent to child order
		FileUtility.reorder(this.resources = resources, true);
		this.depth = depth;
		this.type = type;
		if (this.depth == IResource.DEPTH_ZERO) {
			fullSet = this.resources;
		}
	}

	public IResource[] getResourcesRecursivelly() {
		if (fullSet == null) {
			try {
				fullSet = ResourceStatesChangedEvent.collectResources(resources, depth);
				FileUtility.reorder(fullSet, true);
			} catch (Exception e) {
				fullSet = resources;
			}
		}
		return fullSet;
	}

	public boolean contains(IResource resource) {
		if (containsImpl(resource)) {
			return true;
		}
		if (depth != IResource.DEPTH_ZERO) {
			if (containsImpl(resource.getParent())) {
				return true;
			}
			if (depth != IResource.DEPTH_ONE) {
				IPath path = resource.getFullPath();
				for (IResource element : resources) {
					if (element.getFullPath().isPrefixOf(path)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static IResource[] collectResources(IResource[] resources, int depth) throws Exception {
		if (depth == IResource.DEPTH_ZERO) {
			return resources;
		}

		final HashSet<IResource> fullList = new HashSet<>();
		for (IResource element : resources) {
			FileUtility.visitNodes(element, resource -> {
				// Don't descent into ignored folders, but do not check for
				// every *file* because isIgnored() is not for free
				if (FileUtility.isNotSupervised(resource) || (resource instanceof IContainer && SVNUtility.isIgnored(resource))) {
					return false;
				}
				fullList.add(resource);
				return true;
			}, depth);
		}
		return fullList.toArray(new IResource[fullList.size()]);
	}

	protected boolean containsImpl(IResource resource) {
		for (IResource element : resources) {
			if (element.equals(resource)) {
				return true;
			}
		}
		return false;
	}

	public int getSize() {
		return resources.length;
	}

	@Override
	public int hashCode() {
		return Objects.hash(depth, Arrays.hashCode(resources), type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ResourceStatesChangedEvent)) {
			return false;
		}
		ResourceStatesChangedEvent other = (ResourceStatesChangedEvent) obj;
		if ((depth != other.depth) || (type != other.type) || !Arrays.equals(resources, other.resources)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean canSkip() {
		return true;
	}

	@Override
	public boolean canMerge(ResourceStatesChangedEvent e) {
		return depth == e.depth && type == e.type;
	}

	@Override
	public ResourceStatesChangedEvent merge(ResourceStatesChangedEvent event) {
		IResource[] arr = new IResource[resources.length + event.resources.length];
		System.arraycopy(resources, 0, arr, 0, resources.length);
		System.arraycopy(event.resources, 0, arr, resources.length, event.resources.length);
		return new ResourceStatesChangedEvent(arr, depth, type);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResourceStatesChangedEvent [depth=");
		builder.append(depth);
		builder.append(", size=");
		builder.append(resources.length);
		builder.append(", ");
		builder.append(", type=");
		builder.append(type);
		builder.append(", ");
		builder.append("resources=");
		builder.append(Arrays.toString(resources));
		builder.append("]");
		return builder.toString();
	}

}
