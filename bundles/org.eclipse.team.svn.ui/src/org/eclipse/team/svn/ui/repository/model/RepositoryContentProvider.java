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

import org.eclipse.team.svn.ui.repository.RepositoryTreeViewer;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * Repository content provider
 * 
 * @author Alexander Gurov
 */
public class RepositoryContentProvider extends WorkbenchContentProvider {
	protected RepositoryTreeViewer repositoryTree;

	protected IRepositoryContentFilter filter;

	public RepositoryContentProvider(RepositoryTreeViewer repositoryTree) {
		this.repositoryTree = repositoryTree;
	}

	public IRepositoryContentFilter getFilter() {
		return filter;
	}

	public void setFilter(IRepositoryContentFilter filter) {
		this.filter = filter;
	}

	@Override
	public boolean hasChildren(Object element) {
		IWorkbenchAdapter adapter = getAdapter(element);
		if (adapter instanceof IParentTreeNode) {
			return ((IParentTreeNode) adapter).hasChildren();
		}
		return false;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		IWorkbenchAdapter adapter = getAdapter(parentElement);
		if (adapter instanceof IParentTreeNode) {
			if (adapter instanceof IResourceTreeNode) {
				((IResourceTreeNode) adapter).setViewer(repositoryTree);
			}
			ArrayList<Object> filtered = new ArrayList<>();
			Object[] children = adapter.getChildren(parentElement);
			if (children != null) {
				for (Object child : children) {
					if (filter == null || filter.accept(child)) {
						if (child instanceof IResourceTreeNode) {
							((IResourceTreeNode) child).setViewer(repositoryTree);
						}
						filtered.add(child);
					}
				}
			}
			return filtered.toArray();
		}
		return new Object[0];
	}

}
