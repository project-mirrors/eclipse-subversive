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
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local.property;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Operation to set the properties on multiple resources
 * 
 * @author Sergiy Logvin
 */
public class SetMultiPropertiesOperation extends AbstractWorkingCopyOperation {
	protected IPropertyProvider propertyProvider;

	protected int depth;

	protected IStateFilter filter;

	public SetMultiPropertiesOperation(IResourceProvider resourceProvider, IPropertyProvider propertyProvider,
			IStateFilter filter, int depth) {
		super("Operation_SetMultiProperties", SVNMessages.class, resourceProvider); //$NON-NLS-1$
		this.propertyProvider = propertyProvider;
		this.depth = depth;
		this.filter = filter != null ? filter : IStateFilter.SF_VERSIONED;
	}

	public SetMultiPropertiesOperation(IResource[] resources, IPropertyProvider propertyProvider, IStateFilter filter,
			int depth) {
		super("Operation_SetMultiProperties", SVNMessages.class, resources); //$NON-NLS-1$
		this.propertyProvider = propertyProvider;
		this.depth = depth;
		this.filter = filter != null ? filter : IStateFilter.SF_VERSIONED;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final IResource current = resources[i];

			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(current);
			final ISVNConnector proxy = location.acquireSVNProxy();
			try {
				this.protectStep(monitor1 -> FileUtility.visitNodes(current, resource -> {
					if (monitor1.isCanceled() || FileUtility.isNotSupervised(resource)) {
						return false;
					}
					ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(resource);
					if (filter.accept(local)) {
						SVNProperty[] properties = propertyProvider.getProperties(resource);
						if (properties != null) {
							SetMultiPropertiesOperation.this.processResource(proxy, resource, properties,
									monitor1);
						}
					}
					return filter.allowsRecursion(local);
				}, depth), monitor, resources.length);
			} finally {
				location.releaseSVNProxy(proxy);
			}
		}

	}

	protected void processResource(final ISVNConnector proxy, IResource current, SVNProperty[] properties,
			IProgressMonitor monitor) {
		ProgressMonitorUtility.setTaskInfo(monitor, this, current.getFullPath().toString());
		final String wcPath = FileUtility.getWorkingCopyPath(current);
		for (int i = 0; i < properties.length && !monitor.isCanceled(); i++) {
			final SVNProperty property = properties[i];
			this.protectStep(monitor1 -> proxy.setPropertyLocal(new String[] { wcPath }, property, SVNDepth.EMPTY,
					ISVNConnector.Options.NONE, null,
					new SVNProgressMonitor(SetMultiPropertiesOperation.this, monitor1, null)), monitor, properties.length);
		}
	}

}
