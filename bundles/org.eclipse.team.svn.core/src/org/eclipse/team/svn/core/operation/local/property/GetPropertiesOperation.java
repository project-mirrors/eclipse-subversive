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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Commit operation implementation
 * 
 * @author Sergiy Logvin
 */
public class GetPropertiesOperation extends AbstractActionOperation implements IResourcePropertyProvider {
	protected SVNProperty[] properties;

	protected IResource resource;

	protected SVNRevision revision;

	public GetPropertiesOperation(IResource resource) {
		this(resource,
				IStateFilter.SF_DELETED.accept(SVNRemoteStorage.instance().asLocalResource(resource))
						? SVNRevision.BASE
						: SVNRevision.WORKING);
	}

	public GetPropertiesOperation(IResource resource, SVNRevision revision) {
		super("Operation_GetProperties", SVNMessages.class); //$NON-NLS-1$
		this.resource = resource;
		this.revision = revision;
	}

	@Override
	public SVNProperty[] getProperties() {
		return properties;
	}

	@Override
	public boolean isEditAllowed() {
		return true;
	}

	@Override
	public void refresh() {
		run(new NullProgressMonitor());
	}

	@Override
	public IResource getLocal() {
		return resource;
	}

	@Override
	public IRepositoryResource getRemote() {
		return SVNRemoteStorage.instance().asRepositoryResource(resource);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		properties = null;
		IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resource);
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn proplist \"" + local.getWorkingCopyPath() + "\"\n");
			properties = SVNUtility.properties(proxy,
					new SVNEntryRevisionReference(FileUtility.getWorkingCopyPath(resource), null, revision),
					ISVNConnector.Options.NONE, new SVNProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { resource.getName() });
	}

}
