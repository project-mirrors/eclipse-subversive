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

package org.eclipse.team.svn.ui.repository.model;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.repository.RepositoryTreeViewer;

/**
 * Repository location node representation
 * 
 * @author Alexander Gurov
 */
public class RepositoryLocation extends RepositoryFictiveNode
		implements IParentTreeNode, IDataTreeNode, IResourceTreeNode {
	protected IRepositoryLocation location;

	protected Object[] children;

	protected RepositoryFolder locationRoot;

	public RepositoryLocation(IRepositoryLocation location) {
		this.location = location;
		refresh();
	}

	@Override
	public void setViewer(RepositoryTreeViewer repositoryTree) {
		locationRoot.setViewer(repositoryTree);
	}

	public RepositoryResource getResourceWrapper() {
		return locationRoot;
	}

	@Override
	public IRepositoryResource getRepositoryResource() {
		return location.getRoot();
	}

	@Override
	public void refresh() {
		children = null;
		if (locationRoot == null || !locationRoot.getRepositoryResource().getUrl().equals(location.getUrl())) {
			locationRoot = new RepositoryFolder(null, location.getRoot()) {
				@Override
				protected RefreshOperation getRefreshOperation(RepositoryTreeViewer viewer) {
					return new RefreshOperation(viewer) {
						@Override
						protected void runImpl(IProgressMonitor monitor) throws Exception {
							// TODO rework this using cancellation manager in order to make it thread-safe...
							if (viewer != null && !viewer.getControl().isDisposed()) {
								viewer.refresh(RepositoryLocation.this, null, false);
							}
						}
					};
				}
			};
		} else {
			locationRoot.refresh();
		}
	}

	public IRepositoryLocation getRepositoryLocation() {
		return location;
	}

	@Override
	public Object getData() {
		return location;
	}

	@Override
	public String getLabel(Object o) {
		return location.getLabel();
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	public Object[] getChildren(Object o) {
		if (children == null) {
			ArrayList<Object> list = new ArrayList<>(Arrays.asList(locationRoot.getChildren(o)));
			if (list.size() > 0 && list.get(0) instanceof RepositoryPending) {
				list.add(new RepositoryRevisions(location));
				return list.toArray();
			}
			list.add(new RepositoryRoot(null, location.getRepositoryRoot()));
			list.add(new RepositoryRevisions(location));
			children = list.toArray();
		}
		return children;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return SVNTeamUIPlugin.instance().getImageDescriptor("icons/objects/repository.gif"); //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof RepositoryLocation) {
			return ((RepositoryLocation) obj).location.equals(location);
		}
		return super.equals(obj);
	}

}
