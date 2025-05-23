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

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * This operation allows us to get properties of the concrete revision of the versioned resource
 * 
 * @author Alexander Gurov
 */
public class GetRemotePropertiesOperation extends AbstractRepositoryOperation implements IResourcePropertyProvider {
	protected SVNProperty[] properties;

	public GetRemotePropertiesOperation(IRepositoryResource resource) {
		super("Operation_GetRevisionProperties", SVNMessages.class, new IRepositoryResource[] { resource }); //$NON-NLS-1$
	}

	public GetRemotePropertiesOperation(IRepositoryResourceProvider provider) {
		super("Operation_GetRevisionProperties", SVNMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	public SVNProperty[] getProperties() {
		return properties;
	}

	@Override
	public boolean isEditAllowed() {
		return false;
	}

	@Override
	public void refresh() {

	}

	@Override
	public IResource getLocal() {
		return null;
	}

	@Override
	public IRepositoryResource getRemote() {
		return operableData()[0];
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource resource = operableData()[0];
		properties = null;
		IRepositoryLocation location = resource.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn proplist " + url + "@" + resource.getPegRevision() + " --revprop -r " + resource.getSelectedRevision() + " --username \"" + location.getUsername() + "\"\n");
			properties = SVNUtility.properties(proxy, SVNUtility.getEntryRevisionReference(resource),
					ISVNConnector.Options.NONE, new SVNProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { operableData()[0].getUrl() });
	}

}
