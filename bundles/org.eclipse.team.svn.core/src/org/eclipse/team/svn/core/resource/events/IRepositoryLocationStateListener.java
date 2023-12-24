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
public interface IRepositoryLocationStateListener extends ISSHSettingsStateListener, ISSLSettingsStateListener {
	String LABEL = "label";

	String URL = "url";

	String STRUCTURE_ENABLED = "structureEnabled";

	String TRUNK_LOCATION = "trunkLocation";

	String BRANCHES_LOCATION = "branchesLocation";

	String TAGS_LOCATION = "tagsLocation";

	String USERNAME = "username";

	String PASSWORD = "password";

	String PASSWORD_SAVED = "passwordSaved";

	String AUTHOR_NAME = "authorName";

	String AUTHOR_NAME_ENABLED = "authorNameEnabled";

	void changed(IRepositoryLocation where, String field, Object oldValue, Object newValue);

	void realmAdded(IRepositoryLocation where, String realm, IRepositoryLocation location);

	void realmRemoved(IRepositoryLocation where, String realm);

	void revisionLinkAdded(IRepositoryLocation where, IRevisionLink link);

	void revisionLinkRemoved(IRepositoryLocation where, IRevisionLink link);

	void proxyAcquired(IRepositoryLocation where, ISVNConnector proxy);

	void proxyDisposed(IRepositoryLocation where, ISVNConnector proxy);
}
