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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.property.IRevisionPropertiesProvider;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * Get revision properties operation implementation
 * 
 * @author Alexei Goncharov
 */
public class GetRevisionPropertiesOperation extends AbstractActionOperation implements IRevisionPropertiesProvider {
	protected IRepositoryLocation location;

	protected SVNRevision revision;

	protected SVNProperty[] revProperties;

	/**
	 * Creates an instance of GetRevisionPropertiesOperation depending on the repository location.
	 * 
	 * @param location
	 *            - repository location to get properties for
	 * @param revision
	 *            - revision to get properties for
	 * 
	 * @author Alexei Goncharov
	 */
	public GetRevisionPropertiesOperation(IRepositoryLocation location, SVNRevision revision) {
		super("Operation_GetRevisionProperties", SVNMessages.class); //$NON-NLS-1$
		this.revision = revision;
		this.location = location;
	}

	@Override
	public SVNProperty[] getRevisionProperties() {
		if (revProperties == null) {
			return new SVNProperty[0];
		}
		return revProperties;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		SVNEntryRevisionReference reference = new SVNEntryRevisionReference(location.getUrl(), revision, revision);
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			revProperties = proxy.listRevisionProperties(reference, new SVNProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}
}
