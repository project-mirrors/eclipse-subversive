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

package org.eclipse.team.svn.core.utility;

import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * Utility for comparing repository locations by UUID
 *
 * @author Sergiy Logvin
 */
public class RepositoryLocationUtility {

	protected IRepositoryLocation location;

	public RepositoryLocationUtility(IRepositoryLocation location) {
		this.location = location;
	}

	public IRepositoryLocation getRepositoryLocation() {
		return location;
	}

	public String getRepositoryUUID() {
		String uuid = location.getRepositoryUUID();
		return uuid == null ? location.getId() : uuid;
	}

	@Override
	public int hashCode() {
		int h = 17;
		String username = location.getUsername();
		String password = location.getPassword();
		h += 31 * getRepositoryUUID().hashCode();
		h += 31 * (username != null ? username.hashCode() : 0);
		h += 31 * (password != null ? password.hashCode() : 0);

		return h;
	}

	@Override
	public boolean equals(Object arg0) {
		RepositoryLocationUtility location2 = (RepositoryLocationUtility) arg0;

		return getRepositoryUUID().equals(location2.getRepositoryUUID())
				&& (location.getUsername() != null
						&& location.getUsername().equals(location2.getRepositoryLocation().getUsername())
						|| location.getUsername() == location2.getRepositoryLocation().getUsername())
				&& (location.getPassword() != null
						&& location.getPassword().equals(location2.getRepositoryLocation().getPassword())
						|| location.getPassword() == location2.getRepositoryLocation().getPassword());
	}

}
