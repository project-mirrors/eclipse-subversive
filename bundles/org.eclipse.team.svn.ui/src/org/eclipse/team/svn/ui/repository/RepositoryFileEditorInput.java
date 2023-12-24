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

package org.eclipse.team.svn.ui.repository;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.history.ResourceContentStorage;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.repository.model.IResourceTreeNode;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Remote file editor input implementation
 * 
 * @author Alexander Gurov
 */
public class RepositoryFileEditorInput extends PlatformObject
		implements IWorkbenchAdapter, IRepositoryEditorInput, IResourceTreeNode {
	private static final Object[] NO_CHILDREN = {};

	protected ResourceContentStorage storage;

	protected RepositoryFile resource;

	public RepositoryFileEditorInput(IRepositoryFile resource) {
		this.resource = new RepositoryFile(null, resource);
		storage = new ResourceContentStorage(resource);
	}

	@Override
	public Object[] getChildren(Object o) {
		return RepositoryFileEditorInput.NO_CHILDREN;
	}

	@Override
	public void setViewer(RepositoryTreeViewer repositoryTree) {

	}

	@Override
	public IRepositoryResource getRepositoryResource() {
		return resource.getRepositoryResource();
	}

	@Override
	public Object getData() {
		return getRepositoryResource();
	}

	@Override
	public void refresh() {
		resource.refresh();
	}

	@Override
	public Object getParent(Object o) {
		return resource.getParent(o);
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return resource.getImageDescriptor(object);
	}

	@Override
	public String getLabel(Object o) {
		return resource.getLabel(o);
	}

	@Override
	public void fetchContents(IProgressMonitor monitor) {
		storage.fetchContents(monitor);
	}

	@Override
	public IStorage getStorage() {
		return storage;
	}

	@Override
	public boolean exists() {
		try {
			return resource.getRepositoryResource().exists();
		} catch (Exception ex) {
			LoggedOperation.reportError(this.getClass().getName(), ex);
			return false;
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return this.getImageDescriptor(resource);
	}

	@Override
	public String getName() {
		return BaseMessages.format(SVNUIMessages.RepositoryFileViewer_Name,
				new String[] { resource.getRepositoryResource().getName(),
						String.valueOf(resource.getRepositoryResource().getSelectedRevision()) });
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getName();
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return this;
		}
		if (adapter == IRepositoryFile.class) {
			return resource.getRepositoryResource();
		}
		return super.getAdapter(adapter);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof RepositoryFileEditorInput) {
			return resource.equals(((RepositoryFileEditorInput) obj).resource);
		}
		return false;
	}

	@Override
	public IPath getPath() {
		return storage.getTemporaryPath();
	}

}
