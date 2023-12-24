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

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.team.svn.core.resource.IRepositoryBase;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVN implementation of IRepositoryBase
 * 
 * @author Alexander Gurov
 */
public abstract class SVNRepositoryBase extends PlatformObject implements IRepositoryBase, Serializable {
	private static final long serialVersionUID = -4325317873020232839L;

	protected String url;

	// serialization conventional constructor
	protected SVNRepositoryBase() {
	}

	public SVNRepositoryBase(String url) {
		this.url = url;
	}

	@Override
	public String getName() {
		return SVNUtility.createPathForSVNUrl(getUrl()).lastSegment();
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return getUrl();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof IRepositoryBase) {
			return getUrl().equals(((IRepositoryBase) obj).getUrl());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getUrl().hashCode();
	}

}
