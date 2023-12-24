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

import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;

/**
 * Implementation of IRepositoryRoot
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryRoot extends SVNRepositoryRootBase {
	private static final long serialVersionUID = 8314649604979930219L;

	public SVNRepositoryRoot(IRepositoryLocation location) {
		// do not ask root URL if not required !
		super(location, null, SVNRevision.HEAD);
	}

	@Override
	public String getUrl() {
		return location.getRepositoryRootUrl();
	}

	@Override
	public IRepositoryResource getParent() {
		return null;
	}

	@Override
	public IRepositoryResource getRoot() {
		return this;
	}

	@Override
	public int getKind() {
		return IRepositoryRoot.KIND_ROOT;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof IRepositoryRoot)) {
			return false;
		}
		// do not ask root URL if not required !
		if (getKind() == ((IRepositoryRoot) obj).getKind()
				&& getRepositoryLocation() == ((IRepositoryRoot) obj).getRepositoryLocation()) {
			return true;
		}
		return super.equals(obj);
	}

}
