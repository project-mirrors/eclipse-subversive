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
import org.eclipse.team.svn.core.resource.IRepositoryRoot;

/**
 * Implementation of IRepositoryRoot
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryLocationRoot extends SVNRepositoryRootBase {
	private static final long serialVersionUID = 2511682499975444957L;

	public SVNRepositoryLocationRoot(IRepositoryLocation location) {
		super(location, location.getUrl(), SVNRevision.HEAD);
	}

	public int getKind() {
		return IRepositoryRoot.KIND_LOCATION_ROOT;
	}

}
