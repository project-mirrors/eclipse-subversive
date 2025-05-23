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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.RunExternalRepositoryCompareOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamDiffViewerPage;

/**
 * Two way compare for repository resources operation implementation It runs either external or eclipse's compare editor
 * 
 * @author Igor Burilo
 */
public class CompareRepositoryResourcesOperation extends CompositeOperation {

	protected CompareRepositoryResourcesInernalOperation internalCompare;

	public CompareRepositoryResourcesOperation(IRepositoryResourceProvider provider, boolean forceReuse, long options) {
		super("Operation_CompareRepository", SVNUIMessages.class); //$NON-NLS-1$

		final RunExternalRepositoryCompareOperation externalCompare = new RunExternalRepositoryCompareOperation(
				provider, SVNTeamDiffViewerPage.loadDiffViewerSettings());
		this.add(externalCompare);

		internalCompare = new CompareRepositoryResourcesInernalOperation(provider, forceReuse, options) {
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				if (!externalCompare.isExecuted()) {
					super.runImpl(monitor);
				}
			}
		};
		this.add(internalCompare, new IActionOperation[] { externalCompare });
	}

	public CompareRepositoryResourcesOperation(IRepositoryResource prev, IRepositoryResource next, boolean forceReuse,
			long options) {
		this(new IRepositoryResourceProvider.DefaultRepositoryResourceProvider(
				new IRepositoryResource[] { prev, next }), forceReuse, options);
	}

	public CompareRepositoryResourcesOperation(IRepositoryResource prev, IRepositoryResource next) {
		this(prev, next, false, ISVNConnector.Options.NONE);
	}

	public CompareRepositoryResourcesOperation(IRepositoryResourceProvider provider) {
		this(provider, false, ISVNConnector.Options.NONE);
	}

	public void setForceId(String forceId) {
		internalCompare.setForceId(forceId);
	}
}
