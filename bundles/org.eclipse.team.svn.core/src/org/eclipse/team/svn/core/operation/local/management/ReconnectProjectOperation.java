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

package org.eclipse.team.svn.core.operation.local.management;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamProjectMapper;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Reconnect project that has SVN metainformation
 * 
 * @author Alexander Gurov
 */
public class ReconnectProjectOperation extends AbstractWorkingCopyOperation {
	protected IRepositoryLocation location;

	public ReconnectProjectOperation(IProject[] projects, IRepositoryLocation location) {
		super("Operation_Reconnect", SVNMessages.class, projects); //$NON-NLS-1$
		this.location = location;
	}

	public ReconnectProjectOperation(IResourceProvider provider, IRepositoryLocation location) {
		super("Operation_Reconnect", SVNMessages.class, provider); //$NON-NLS-1$
		this.location = location;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		// reconnect always requires root as scheduling rule
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final IProject project = (IProject) resources[i];
			this.protectStep(monitor1 -> {
				SVNChangeStatus st = SVNUtility.getSVNInfoForNotConnected(project);
				if (st == null) {
					throw new UnreportableException(SVNMessages.getErrorString("Error_NonSVNPath")); //$NON-NLS-1$
				}
				IRepositoryContainer remote = location.asRepositoryContainer(SVNUtility.decodeURL(st.url), false);
				SVNTeamProjectMapper.map(project, remote);
			}, monitor, resources.length);
		}
	}

}
