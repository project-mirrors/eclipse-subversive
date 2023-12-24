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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.remote.GetLogMessagesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Corrects Log Message revision
 * 
 * @author Alexander Gurov
 */
public class CorrectRevisionOperation extends AbstractActionOperation {
	protected IRepositoryResource[] repositoryResources;

	protected long[] knownRevisions;

	protected GetLogMessagesOperation[] msgsOps;

	protected IResource[] resources;

	protected boolean hasWarning;

	protected boolean isCancel;

	public CorrectRevisionOperation(GetLogMessagesOperation msgsOp, IRepositoryResource repositoryResource,
			long knownRevision, IResource resource) {
		this(msgsOp == null ? null : new GetLogMessagesOperation[] { msgsOp },
				new IRepositoryResource[] { repositoryResource }, new long[] { knownRevision },
				new IResource[] { resource });
	}

	public CorrectRevisionOperation(GetLogMessagesOperation[] msgsOps, IRepositoryResource[] repositoryResources,
			long[] knownRevisions, IResource[] resources) {
		super("Operation_CorrectRevision", SVNUIMessages.class); //$NON-NLS-1$
		this.repositoryResources = repositoryResources;
		this.knownRevisions = knownRevisions;
		this.msgsOps = msgsOps;
		this.resources = resources;
	}

	@Override
	public int getOperationWeight() {
		if (msgsOps == null) {
			return 0;
		}
		return super.getOperationWeight();
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		for (int i = 0; i < repositoryResources.length; i++) {
			if (!repositoryResources[i].exists() && resources != null && resources[i] != null
					&& resources[i].getType() != IResource.PROJECT) {
				// calculate peg revision for the repository resource
				ILocalResource parent = SVNRemoteStorage.instance().asLocalResourceAccessible(resources[i].getParent());
				ILocalResource self = SVNRemoteStorage.instance().asLocalResourceAccessible(resources[i]);
				boolean switchedStateEquals = (parent.getChangeMask()
						& ILocalResource.IS_SWITCHED) == (self.getChangeMask() & ILocalResource.IS_SWITCHED);
				if (switchedStateEquals) {
					long parentRevision = parent.getRevision();
					long selfRevision = self.getRevision();
					long revision = parentRevision > selfRevision ? parentRevision : selfRevision;
					if (revision != SVNRevision.INVALID_REVISION_NUMBER) {
						repositoryResources[i].setPegRevision(SVNRevision.fromNumber(revision));
					}
				} else {
					repositoryResources[i].setPegRevision(SVNRevision.fromNumber(self.getRevision()));
				}
			}
			if (!repositoryResources[i].exists() && knownRevisions[i] != SVNRevision.INVALID_REVISION_NUMBER) {
				hasWarning = true;
				SVNRevision rev = SVNRevision.fromNumber(knownRevisions[i]);
				repositoryResources[i].setSelectedRevision(rev);
				repositoryResources[i].setPegRevision(rev);
				if (msgsOps != null) {
					msgsOps[i].setStartRevision(rev);
				}
			}
		}
		if (hasWarning) {
			UIMonitorUtility.getDisplay().syncExec(() -> {
				boolean one = repositoryResources.length == 1;
				MessageDialog dlg = new MessageDialog(
						UIMonitorUtility.getShell(),
						CorrectRevisionOperation.this.getOperationResource(one ? "Title_Single" : "Title_Multi"), //$NON-NLS-1$ //$NON-NLS-2$
						null,
						CorrectRevisionOperation.this
								.getOperationResource(one ? "Message_Single" : "Message_Multi"), //$NON-NLS-1$ //$NON-NLS-2$
						MessageDialog.WARNING,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
				if (dlg.open() != 0) {
					monitor.setCanceled(true);
					isCancel = true;
				}
			});
		}
	}

	public boolean hasWarning() {
		return hasWarning;
	}

	public boolean isCancel() {
		return isCancel;
	}

}
