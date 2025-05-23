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

package org.eclipse.team.svn.core.operation.file.property;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.file.AbstractFileOperation;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Get resource properties operation
 * 
 * @author Alexander Gurov
 */
public class GetPropertiesOperation extends AbstractFileOperation {
	protected SVNProperty[] properties;

	protected SVNRevision revision;

	public GetPropertiesOperation(File file) {
		this(file, SVNRevision.WORKING);
	}

	public GetPropertiesOperation(File file, SVNRevision revision) {
		super("Operation_GetPropertiesFile", SVNMessages.class, new File[] { file }); //$NON-NLS-1$
		this.revision = revision;
	}

	public SVNProperty[] getProperties() {
		return properties;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File file = operableData()[0];
		IRepositoryResource remote = SVNFileStorage.instance().asRepositoryResource(file, false);
		IRepositoryLocation location = remote.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			properties = SVNUtility.properties(proxy,
					new SVNEntryRevisionReference(file.getAbsolutePath(), null, revision), ISVNConnector.Options.NONE,
					new SVNProgressMonitor(this, monitor, null));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

}
