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
import org.eclipse.team.svn.core.resource.IRepositoryFolder;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * SVN based representation of IRepositoryFolder
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryFolder extends SVNRepositoryContainer implements IRepositoryFolder {
	private static final long serialVersionUID = -8790962415969490733L;

	public SVNRepositoryFolder(IRepositoryLocation location, String url, SVNRevision selectedRevision) {
		super(location, url, selectedRevision);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof IRepositoryFolder)) {
			return false;
		}
		return super.equals(obj);
	}

}
