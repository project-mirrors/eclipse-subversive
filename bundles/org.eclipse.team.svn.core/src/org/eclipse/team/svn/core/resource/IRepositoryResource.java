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

package org.eclipse.team.svn.core.resource;

import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNLock;
import org.eclipse.team.svn.core.connector.SVNRevision;

/**
 * Abstract repository resource
 * 
 * @author Alexander Gurov
 */
public interface IRepositoryResource extends IRepositoryBase, IRepositoryResourceFactory {
	
	public static class Information {
		public final SVNLock lock;
		public final long fileSize;
		public final String lastAuthor;
		public final long lastChangedDate;
		public final boolean hasProperties;
		
		public Information(SVNLock lock, long fileSize, String lastAuthor, long lastChangedDate, boolean hasProperties) {
			this.lock = lock;
			this.fileSize = fileSize;
			this.lastAuthor = lastAuthor;
			this.hasProperties = hasProperties;
			this.lastChangedDate = lastChangedDate;
		}
	}

	public SVNRevision getSelectedRevision();
	
	public void setSelectedRevision(SVNRevision revision);
	
	public SVNRevision getPegRevision();
	
	public void setPegRevision(SVNRevision pegRevision);
	
	public boolean isInfoCached();
	
	public void refresh();

	public boolean exists() throws SVNConnectorException;
	
	public String getName();
	
	public String getUrl();
	
	public long getRevision() throws SVNConnectorException;
	
	public IRepositoryResource getParent();
	
	public IRepositoryResource getRoot();
	
	public IRepositoryLocation getRepositoryLocation();
	
	public Information getInfo();
	
}
