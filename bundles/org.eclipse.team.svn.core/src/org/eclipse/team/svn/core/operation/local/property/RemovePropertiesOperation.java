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

package org.eclipse.team.svn.core.operation.local.property;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Commit operation implementation
 * 
 * @author Sergiy Logvin
 */
public class RemovePropertiesOperation extends AbstractWorkingCopyOperation {
	protected SVNProperty[] data;

	protected boolean isRecursive;

	public RemovePropertiesOperation(IResource[] resources, SVNProperty[] data, boolean isRecursive) {
		super("Operation_RemoveProperties", SVNMessages.class, resources); //$NON-NLS-1$
		this.data = data;
		this.isRecursive = isRecursive;
	}

	public RemovePropertiesOperation(IResourceProvider resourceProvider, SVNProperty[] data, boolean isRecursive) {
		super("Operation_RemoveProperties", SVNMessages.class, resourceProvider); //$NON-NLS-1$
		this.data = data;
		this.isRecursive = isRecursive;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		if (isRecursive) {
			resources = FileUtility.shrinkChildNodes(resources);
		}

		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final IResource resource = resources[i];

			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resource);
			final ISVNConnector proxy = location.acquireSVNProxy();

			this.protectStep(monitor1 -> RemovePropertiesOperation.this.processResource(proxy, resource, monitor1), monitor, resources.length);

			location.releaseSVNProxy(proxy);
		}
	}

	protected void processResource(final ISVNConnector proxy, IResource resource, IProgressMonitor monitor) {
		ProgressMonitorUtility.setTaskInfo(monitor, this, resource.getFullPath().toString());
		final String wcPath = FileUtility.getWorkingCopyPath(resource);

		for (int i = 0; i < data.length && !monitor.isCanceled(); i++) {
			final SVNProperty current = data[i];
			this.protectStep(monitor1 -> proxy.setPropertyLocal(new String[] { wcPath }, new SVNProperty(current.name),
					isRecursive ? SVNDepth.INFINITY : SVNDepth.EMPTY, ISVNConnector.Options.NONE, null,
					new SVNProgressMonitor(RemovePropertiesOperation.this, monitor1, null)), monitor, data.length);
		}
	}

}
