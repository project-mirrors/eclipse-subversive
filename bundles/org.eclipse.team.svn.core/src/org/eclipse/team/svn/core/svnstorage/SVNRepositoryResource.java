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
import org.eclipse.team.svn.core.connector.SVNConnectorCancelException;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVN based representation of IRepositoryResource
 * 
 * @author Alexander Gurov
 */
public abstract class SVNRepositoryResource extends SVNRepositoryBase implements IRepositoryResource, Serializable {
	private static final long serialVersionUID = 8854704746872311777L;

	private transient SVNRevision selectedRevision; // should be managed using setters and getters regarding to "transient" modifier

	private transient SVNRevision pegRevision; // revision where we found this item

	protected transient SVNRevision.Number lastRevision;

	protected transient IRepositoryLocation location;

	protected transient IRepositoryRoot root;

	protected transient IRepositoryResource.Information info;

	// serialization conventional constructor
	protected SVNRepositoryResource() {
	}

	public SVNRepositoryResource(IRepositoryLocation location, String url, SVNRevision selectedRevision) {
		super(url);
		this.location = location;
		this.selectedRevision = selectedRevision;
	}

	public void setInfo(IRepositoryResource.Information info) {
		this.info = info;
	}

	@Override
	public Information getInfo() {
		return info;
	}

	@Override
	public SVNRevision getPegRevision() {
		return pegRevision == null ? SVNRevision.HEAD : pegRevision;
	}

	@Override
	public void setPegRevision(SVNRevision pegRevision) {
		this.pegRevision = pegRevision;
	}

	@Override
	public SVNRevision getSelectedRevision() {
		if (selectedRevision == null) {
			selectedRevision = SVNRevision.HEAD;
		}
		return selectedRevision;
	}

	@Override
	public void setSelectedRevision(SVNRevision revision) {
		selectedRevision = revision;
	}

	@Override
	public boolean isInfoCached() {
		return lastRevision != null;
	}

	@Override
	public synchronized void refresh() {
		lastRevision = null;
	}

	public void setRevision(long revisionNumber) {
		lastRevision = SVNRevision.fromNumber(revisionNumber);
	}

	@Override
	public synchronized long getRevision() throws SVNConnectorException {
		if (lastRevision == null) {
			lastRevision = SVNRevision.INVALID_REVISION;
			ISVNConnector proxy = getRepositoryLocation().acquireSVNProxy();
			try {
				getRevisionImpl(proxy);
			} finally {
				getRepositoryLocation().releaseSVNProxy(proxy);
			}
		}
		return lastRevision.getNumber();
	}

	@Override
	public boolean exists() throws SVNConnectorException {
		try {
			return getRevision() != SVNRevision.INVALID_REVISION_NUMBER;
		} catch (SVNConnectorException ex) {
			//FIXME uncomment this when the WI is resolved ("Unknown node kind" exception instead of "Path not found" (PLC-1008))
//			if (ex instanceof ClientExceptionEx) {
//				if (((ClientExceptionEx)ex).getErrorMessage().getErrorCode().equals(SVNErrorCode.RA_DAV_PATH_NOT_FOUND)) {
//					return false;
//				}
//			}
//			throw ex;
			if (ex instanceof SVNConnectorCancelException) {
				throw ex;
			}
			return false;
		}
	}

	@Override
	public IRepositoryResource getParent() {
		String parentUrl = SVNUtility.normalizeURL(getUrl());
		int idx = parentUrl.lastIndexOf('/');
		if (idx == -1) {
			throw new IllegalArgumentException(parentUrl);
		}
		return asRepositoryContainer(parentUrl.substring(0, idx), true);
	}

	@Override
	public IRepositoryResource getRoot() {
		if (root == null) {
			IRepositoryResource parent = this;
			while (!(parent instanceof IRepositoryRoot)) {
				parent = parent.getParent();
			}
			root = (IRepositoryRoot) parent;
		}
		return root;
	}

	@Override
	public IRepositoryLocation getRepositoryLocation() {
		return location;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof IRepositoryResource)) {
			return false;
		}
		IRepositoryResource other = (IRepositoryResource) obj;
		return super.equals(obj) && getSelectedRevision().equals(other.getSelectedRevision())
				&& getPegRevision().equals(other.getPegRevision());
	}

	@Override
	public IRepositoryContainer asRepositoryContainer(String url, boolean allowsNull) {
		IRepositoryContainer retVal = getRepositoryLocation()
				.asRepositoryContainer(url.indexOf('/') != -1 ? url : getUrl() + "/" + url, allowsNull); //$NON-NLS-1$
		if (retVal == null) {
			return null;
		}
		retVal.setPegRevision(getPegRevision());
		retVal.setSelectedRevision(getSelectedRevision());
		return retVal;
	}

	@Override
	public IRepositoryFile asRepositoryFile(String url, boolean allowsNull) {
		IRepositoryFile retVal = getRepositoryLocation()
				.asRepositoryFile(url.indexOf('/') != -1 ? url : getUrl() + "/" + url, allowsNull); //$NON-NLS-1$
		if (retVal == null) {
			return null;
		}
		retVal.setPegRevision(getPegRevision());
		retVal.setSelectedRevision(getSelectedRevision());
		return retVal;
	}

	protected abstract void getRevisionImpl(ISVNConnector proxy) throws SVNConnectorException;

}
