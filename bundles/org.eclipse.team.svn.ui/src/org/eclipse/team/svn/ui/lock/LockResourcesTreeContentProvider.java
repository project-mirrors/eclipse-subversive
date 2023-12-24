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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.lock;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for lock resources
 * 
 * @author Igor Burilo
 */
public class LockResourcesTreeContentProvider implements ITreeContentProvider {

	protected LockResource root;

	public void initialize(LockResource root) {
		this.root = root;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		List<LockResource> res = new ArrayList<>();
		LockResource node = (LockResource) parentElement;
		LockResource[] children = node.getChildren();
		for (LockResource child : children) {
			if (!child.isFile()) {
				res.add(child);
			}
		}
		return res.toArray(new LockResource[0]);
	}

	@Override
	public boolean hasChildren(Object element) {
		LockResource node = (LockResource) element;
		LockResource[] children = node.getChildren();
		for (LockResource child : children) {
			if (!child.isFile()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object getParent(Object element) {
		return ((LockResource) element).getParent();
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return new Object[] { root };
	}

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

}
