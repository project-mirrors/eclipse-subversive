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

package org.eclipse.team.svn.core.resource.events;

import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRevisionLink;

/**
 * Repository location change listener
 * 
 * @author Alexander Gurov
 */
public class RepositoryLocationStateAdapter implements IRepositoryLocationStateListener {
	@Override
	public void sshChanged(IRepositoryLocation where, String field, Object oldValue, Object newValue) {

	}

	@Override
	public void sslChanged(IRepositoryLocation where, String field, Object oldValue, Object newValue) {

	}

	@Override
	public void changed(IRepositoryLocation where, String field, Object oldValue, Object newValue) {

	}

	@Override
	public void realmAdded(IRepositoryLocation where, String realm, IRepositoryLocation location) {

	}

	@Override
	public void realmRemoved(IRepositoryLocation where, String realm) {

	}

	@Override
	public void revisionLinkAdded(IRepositoryLocation where, IRevisionLink link) {

	}

	@Override
	public void revisionLinkRemoved(IRepositoryLocation where, IRevisionLink link) {

	}

	@Override
	public void proxyAcquired(IRepositoryLocation where, ISVNConnector proxy) {

	}

	@Override
	public void proxyDisposed(IRepositoryLocation where, ISVNConnector proxy) {

	}
}
