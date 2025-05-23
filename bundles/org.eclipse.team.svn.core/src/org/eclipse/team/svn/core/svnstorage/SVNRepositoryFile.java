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

package org.eclipse.team.svn.core.svnstorage;

import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntry.Fields;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVN based representation of IRepositoryFile
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryFile extends SVNRepositoryResource implements IRepositoryFile {
	private static final long serialVersionUID = 6042328067024796901L;

	public SVNRepositoryFile(IRepositoryLocation location, String url, SVNRevision selectedRevision) {
		super(location, url, selectedRevision);
	}

	@Override
	protected void getRevisionImpl(ISVNConnector proxy) throws SVNConnectorException {
		SVNEntry[] entries = SVNUtility.list(proxy, SVNUtility.getEntryRevisionReference(this), SVNDepth.EMPTY,
				Fields.ALL, ISVNConnector.Options.FETCH_LOCKS, new SVNNullProgressMonitor());
		if (entries != null && entries.length > 0 && entries[0].revision != SVNRevision.INVALID_REVISION_NUMBER) {//FIXME -1 for SVN Kit 1.2.0 if resource is not exists
			lastRevision = SVNRevision.fromNumber(entries[0].revision);
			setInfo(new IRepositoryResource.Information(entries[0].lock, entries[0].size, entries[0].author,
					entries[0].date, entries[0].hasProperties));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof IRepositoryFile)) {
			return false;
		}
		return super.equals(obj);
	}

}
