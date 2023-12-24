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

import java.io.Serializable;

import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntry.Fields;
import org.eclipse.team.svn.core.connector.SVNEntry.Kind;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVN based representation of IRepositoryContainer
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryContainer extends SVNRepositoryResource implements IRepositoryContainer, Serializable {
	private static final long serialVersionUID = -6380931196819185798L;

	protected transient IRepositoryResource[] children;

	// serialization conventional constructor
	protected SVNRepositoryContainer() {
	}

	public SVNRepositoryContainer(IRepositoryLocation location, String url, SVNRevision selectedRevision) {
		super(location, url, selectedRevision);
	}

	@Override
	public boolean isChildrenCached() {
		return children != null;
	}

	@Override
	public synchronized void refresh() {
		super.refresh();
		children = null;
	}

	@Override
	public void setSelectedRevision(SVNRevision revision) {
		//If we change selected revision, we need to reset children cache
		if (isChildrenCached() && revision != null && !getSelectedRevision().equals(revision)) {
			children = null;
		}
		super.setSelectedRevision(revision);
	}

	@Override
	public IRepositoryResource[] getChildren() throws SVNConnectorException {
		IRepositoryResource[] retVal = children;

		// synchronize only assignment in order to avoid deadlock with this Sync and UI Sync locked from callback
		//	in result we can perform excessive work but it is acceptable in that case
		if (retVal == null) {
			String thisUrl = getUrl();
			SVNEntry[] children = null;

			ISVNConnector proxy = getRepositoryLocation().acquireSVNProxy();
			try {
				children = SVNUtility.list(proxy, SVNUtility.getEntryRevisionReference(this), SVNDepth.IMMEDIATES,
						Fields.ALL, ISVNConnector.Options.FETCH_LOCKS, new SVNNullProgressMonitor());
			} finally {
				getRepositoryLocation().releaseSVNProxy(proxy);
			}

			synchronized (this) {
				retVal = new IRepositoryResource[children.length];

				for (int i = 0; i < children.length; i++) {
					if (children[i].revision == SVNRevision.INVALID_REVISION_NUMBER) {
						//FIXME -1 for SVN Kit 1.2.0 if resource is not exists
						throw new SVNConnectorException("-1 for SVN Kit 1.2.0 if resource is not exists");
					}
					String childUrl = thisUrl + "/" + children[i].path;
					SVNRepositoryResource resource = children[i].nodeKind == Kind.DIR
							? (SVNRepositoryResource) asRepositoryContainer(childUrl, false)
							: (SVNRepositoryResource) asRepositoryFile(childUrl, false);
					resource.setRevision(children[i].revision);
					resource.setInfo(new IRepositoryResource.Information(children[i].lock, children[i].size,
							children[i].author, children[i].date, children[i].hasProperties));
					retVal[i] = resource;
				}

				this.children = retVal;
			}
		}

		return retVal;
	}

	@Override
	protected void getRevisionImpl(ISVNConnector proxy) throws SVNConnectorException {
		SVNEntryRevisionReference reference = SVNUtility.getEntryRevisionReference(this);
		SVNEntryInfo[] infos = SVNUtility.info(proxy, reference, SVNDepth.EMPTY, new SVNNullProgressMonitor());
		if (infos != null && infos.length > 0 && infos[0].lastChangedRevision != SVNRevision.INVALID_REVISION_NUMBER) {
			lastRevision = SVNRevision.fromNumber(infos[0].lastChangedRevision);
			SVNProperty[] data = SVNUtility.properties(proxy, reference, ISVNConnector.Options.NONE,
					new SVNNullProgressMonitor());
			setInfo(new IRepositoryResource.Information(infos[0].lock, 0, infos[0].lastChangedAuthor,
					infos[0].lastChangedDate, data != null && data.length > 0));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof IRepositoryContainer)) {
			return false;
		}
		return super.equals(obj);
	}

}
