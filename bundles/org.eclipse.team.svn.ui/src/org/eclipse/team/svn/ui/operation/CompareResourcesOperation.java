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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation;
import org.eclipse.team.svn.core.operation.local.UDiffGenerateOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamDiffViewerPage;

/**
 * Compare WORKING and BASE revisions of a local resources operation
 * 
 * It runs either external or eclipse's compare editor
 * 
 * @author Igor Burilo
 */
public class CompareResourcesOperation extends CompositeOperation {

	protected ILocalResource local;

	protected IRepositoryResource remote;

	protected CompareResourcesInternalOperation internalCompareOp;

	public CompareResourcesOperation(ILocalResource local, IRepositoryResource remote) {
		this(local, remote, false, false);
	}

	public CompareResourcesOperation(ILocalResource local, IRepositoryResource remote, boolean forceReuse) {
		this(local, remote, forceReuse, false);
	}

	public CompareResourcesOperation(ILocalResource local, IRepositoryResource remote, boolean forceReuse,
			boolean showInDialog) {
		this(local, remote, forceReuse, showInDialog, ISVNConnector.Options.NONE);
	}

	public CompareResourcesOperation(ILocalResource local, IRepositoryResource remote, boolean forceReuse,
			boolean showInDialog, long options) {
		super("Operation_CompareLocal", SVNUIMessages.class); //$NON-NLS-1$
		this.local = local;
		this.remote = remote;

		final RunExternalCompareOperation externalCompareOp = new RunExternalCompareOperation(local, remote,
				SVNTeamDiffViewerPage.loadDiffViewerSettings());
		this.add(externalCompareOp);

		internalCompareOp = new CompareResourcesInternalOperation(local, remote, forceReuse, showInDialog, options) {
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				if (!externalCompareOp.isExecuted()) {
					super.runImpl(monitor);
				}
			}
		};
		this.add(internalCompareOp, new IActionOperation[] { externalCompareOp });
	}

	public void setDiffFile(String diffFile) {
		if (diffFile != null) {
			this.add(new UDiffGenerateOperation(local, remote, diffFile), new IActionOperation[] { internalCompareOp });
		}
	}

	public void setForceId(String forceId) {
		internalCompareOp.setForceId(forceId);
	}

	/**
	 * If there are no repository changes (incoming or conflicting), then we compare with base revision (don't touch repository)
	 *
	 * @param resource
	 * @return
	 */
	public static SVNRevision getRemoteResourceRevisionForCompare(IResource resource) {
		SVNRevision revision = SVNRevision.HEAD;
		try {
			AbstractSVNSyncInfo syncInfo = (AbstractSVNSyncInfo) UpdateSubscriber.instance().getSyncInfo(resource);
			if (syncInfo != null) {
				int kind = SyncInfo.getDirection(syncInfo.getKind());
				if (kind != SyncInfo.INCOMING && kind != SyncInfo.CONFLICTING) {
					revision = SVNRevision.BASE;
				}
			}
		} catch (TeamException te) {
			LoggedOperation.reportError(CompareResourcesOperation.class.toString(), te);
		}
		return revision;
	}
}
