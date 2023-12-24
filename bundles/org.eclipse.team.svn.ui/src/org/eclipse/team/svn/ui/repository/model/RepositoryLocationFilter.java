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

package org.eclipse.team.svn.ui.repository.model;

import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * Repository locations filter default implementation
 *
 * @author Sergiy Logvin
 */
public class RepositoryLocationFilter implements IRepositoryContentFilter {
	protected String url;

	public RepositoryLocationFilter(String locationUrl) {
		this.url = locationUrl;
	}

	public boolean accept(Object obj) {
		if (obj instanceof RepositoryLocation) {
			IRepositoryLocation location = ((RepositoryLocation) obj).getRepositoryLocation();
			return location.getUrl().equals(this.url);
		}
		return true;
	}

}
