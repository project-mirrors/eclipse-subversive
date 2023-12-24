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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.repository.browser;

import org.eclipse.team.svn.ui.repository.model.IParentTreeNode;
import org.eclipse.team.svn.ui.repository.model.RepositoryError;
import org.eclipse.team.svn.ui.repository.model.RepositoryFictiveWorkingDirectory;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.repository.model.RepositoryPending;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Repository browser content provider
 *
 * @author Sergiy Logvin
 */
public class RepositoryBrowserContentProvider extends BaseWorkbenchContentProvider {
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

		if (adapter instanceof RepositoryFolder) {
			RepositoryFolder folder = (RepositoryFolder) adapter;
			Object[] children = folder.peekChildren(parentElement);
			if (folder.getParent() == null) {
				return children;
			}
			if (children.length != 0 && children[0] != null
					&& (children[0] instanceof RepositoryError || children[0] instanceof RepositoryPending)) {
				return children;
			}
			Object[] childrenWithParent = new Object[children.length + 1];
			childrenWithParent[0] = new RepositoryFictiveWorkingDirectory(parentElement);
			System.arraycopy(children, 0, childrenWithParent, 1, children.length);
			return childrenWithParent;
		}
		return super.getChildren(parentElement);
	}

}
