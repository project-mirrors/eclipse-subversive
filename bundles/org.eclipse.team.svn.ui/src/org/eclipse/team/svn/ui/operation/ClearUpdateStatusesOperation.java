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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * UI operation that can clear remote statuses storage in the synchronize view
 * 
 * @author Alexander Gurov
 */
public class ClearUpdateStatusesOperation extends AbstractWorkingCopyOperation {
	public ClearUpdateStatusesOperation(IResource[] resources) {
		super("Operation_ClearUpdateStatusesCache", SVNUIMessages.class, resources); //$NON-NLS-1$
	}

	public ClearUpdateStatusesOperation(IResourceProvider provider) {
		super("Operation_ClearUpdateStatusesCache", SVNUIMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		UpdateSubscriber.instance().clearRemoteStatuses(operableData());
	}

}
