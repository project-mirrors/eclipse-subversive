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
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Set resource property operation
 * 
 * @author Sergiy Logvin
 */
public class SetPropertiesOperation extends AbstractWorkingCopyOperation {
	protected SVNProperty[] propertyData;

	protected boolean isRecursive;

	protected IResourcePropertyProvider propertyProvider;

	public SetPropertiesOperation(IResource[] resources, String name, byte[] data, boolean isRecursive) {
		this(resources, new SVNProperty[] { new SVNProperty(name, new String(data)) }, isRecursive);
	}

	public SetPropertiesOperation(IResource[] resources, SVNProperty[] data, boolean isRecursive) {
		super("Operation_SetProperties", SVNMessages.class, resources); //$NON-NLS-1$
		propertyData = data;
		this.isRecursive = isRecursive;
	}

	public SetPropertiesOperation(IResourceProvider resourceProvider, SVNProperty[] data, boolean isRecursive) {
		super("Operation_SetProperties", SVNMessages.class, resourceProvider); //$NON-NLS-1$
		propertyData = data;
		this.isRecursive = isRecursive;
	}

	public SetPropertiesOperation(IResource[] resources, IResourcePropertyProvider propertyProvider,
			boolean isRecursive) {
		super("Operation_SetProperties", SVNMessages.class, resources); //$NON-NLS-1$
		this.propertyProvider = propertyProvider;
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

			this.protectStep(monitor1 -> SetPropertiesOperation.this.processResource(proxy, resource, monitor1), monitor, resources.length);

			location.releaseSVNProxy(proxy);
		}
	}

	protected void processResource(final ISVNConnector proxy, IResource resource, IProgressMonitor monitor) {
		ProgressMonitorUtility.setTaskInfo(monitor, this, resource.getFullPath().toString());
		final String wcPath = FileUtility.getWorkingCopyPath(resource);

		SVNProperty[] properties = getOperableProperties();

		for (int i = 0; i < properties.length && !monitor.isCanceled(); i++) {
			final SVNProperty current = properties[i];
			this.protectStep(monitor1 -> proxy.setPropertyLocal(new String[] { wcPath }, current, SVNDepth.infinityOrEmpty(isRecursive),
					ISVNConnector.Options.NONE, null,
					new SVNProgressMonitor(SetPropertiesOperation.this, monitor1, null)), monitor, properties.length);
		}
	}

	protected SVNProperty[] getOperableProperties() {
		return propertyData == null ? propertyProvider.getProperties() : propertyData;
	}

}
