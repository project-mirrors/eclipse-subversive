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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.remote.AbstractRepositoryOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.repository.RepositoriesView;
import org.eclipse.team.svn.ui.repository.RepositoryTreeViewer;
import org.eclipse.team.svn.ui.repository.model.IDataTreeNode;

/**
 * Refresh repository tree operation
 * 
 * @author Alexander Gurov
 */
public class RefreshRemoteResourcesOperation extends AbstractRepositoryOperation {

	public RefreshRemoteResourcesOperation(IRepositoryResource[] resources) {
		super("Operation_RefreshRemote", SVNUIMessages.class, resources); //$NON-NLS-1$
	}

	public RefreshRemoteResourcesOperation(IRepositoryResourceProvider provider) {
		super("Operation_RefreshRemote", SVNUIMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource[] resources = operableData();

		for (final IRepositoryResource current : resources) {
			this.protectStep(monitor1 -> {
				if (current instanceof IRepositoryRoot
						&& ((IRepositoryRoot) current).getKind() == IRepositoryRoot.KIND_LOCATION_ROOT) {
					RepositoriesView.refresh(current.getRepositoryLocation(), new RefreshVisitor());
				} else {
					RepositoriesView.refresh(current, new RefreshVisitor());
				}
			}, monitor, resources.length);
		}
	}

	protected class RefreshVisitor implements RepositoryTreeViewer.IRefreshVisitor {
		@Override
		public void visit(Object element) {
			if (element instanceof IDataTreeNode) {
				((IDataTreeNode) element).refresh();
			}
		}
	}

}
