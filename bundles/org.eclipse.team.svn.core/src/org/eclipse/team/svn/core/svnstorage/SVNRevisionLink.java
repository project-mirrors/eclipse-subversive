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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.svnstorage;

import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRevisionLink;

/**
 * SVN implementation of IRevisionLink
 * 
 * @author Igor Burilo
 */
public class SVNRevisionLink implements IRevisionLink {

	protected IRepositoryResource repositoryResource;

	protected String comment;

	public SVNRevisionLink(IRepositoryResource repositoryResource) {
		this.repositoryResource = repositoryResource;
	}

	@Override
	public String getComment() {
		return comment == null ? "" : comment; //$NON-NLS-1$
	}

	@Override
	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public IRepositoryResource getRepositoryResource() {
		return repositoryResource;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof IRevisionLink)) {
			return false;
		}
		IRevisionLink link = (IRevisionLink) obj;
		return repositoryResource.equals(link.getRepositoryResource()) && getComment().equals(link.getComment());
	}

	@Override
	public int hashCode() {
		return repositoryResource.hashCode();
	}

	@Override
	public String toString() {
		return repositoryResource.toString();
	}
}
