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

import java.util.HashMap;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Operation to get properties for multiple resources
 * 
 * @author Sergiy Logvin
 */
public class GetMultiPropertiesOperation extends AbstractActionOperation
		implements IResourceProvider, IPropertyProvider {
	protected IResource[] resources;

	protected HashMap<IResource, SVNProperty[]> properties;

	protected String propertyName;

	protected int depth;

	protected IStateFilter filter;

	/**
	 * @param resources
	 *            the resources for which the properties are requested
	 * @param propertyName
	 *            the name of a property which value is requested. If null then all the properties will be computed for the resources.
	 */
	public GetMultiPropertiesOperation(IResource[] resources, int depth, IStateFilter filter, String propertyName) {
		super("Operation_GetMultiProperties", SVNMessages.class); //$NON-NLS-1$
		this.resources = resources;
		this.propertyName = propertyName;
		this.filter = filter != null ? filter : IStateFilter.SF_VERSIONED;
		properties = new HashMap<>();
		this.depth = depth;
	}

	@Override
	public IResource[] getResources() {
		Set<IResource> resources = properties.keySet();
		return resources.toArray(new IResource[resources.size()]);
	}

	@Override
	public SVNProperty[] getProperties(IResource resource) {
		return properties.get(resource);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
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
						GetMultiPropertiesOperation.this.processResource(proxy, resource, monitor1);
					}
					return filter.allowsRecursion(local);
				}, depth), monitor, resources.length);
			} finally {
				location.releaseSVNProxy(proxy);
			}
		}
	}

	protected void processResource(final ISVNConnector proxy, final IResource current, IProgressMonitor monitor) {
		ProgressMonitorUtility.setTaskInfo(monitor, this, current.getFullPath().toString());
		this.protectStep(monitor1 -> {
			String wcPath = FileUtility.getWorkingCopyPath(current);
			if (propertyName != null) {
				SVNProperty data = proxy.getProperty(
						new SVNEntryRevisionReference(wcPath, null, SVNRevision.WORKING), propertyName, null,
						new SVNProgressMonitor(GetMultiPropertiesOperation.this, monitor1, null));
				if (data != null) {
					properties.put(current, new SVNProperty[] { data });
				}
			} else {
				SVNProperty[] data = SVNUtility.properties(proxy,
						new SVNEntryRevisionReference(wcPath, null, SVNRevision.WORKING),
						ISVNConnector.Options.NONE,
						new SVNProgressMonitor(GetMultiPropertiesOperation.this, monitor1, null));
				if (data != null && data.length != 0) {
					properties.put(current, data);
				}
			}
		}, monitor, 1);
	}

}
