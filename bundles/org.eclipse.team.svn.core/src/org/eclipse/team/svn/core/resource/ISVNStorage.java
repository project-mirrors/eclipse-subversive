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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.resource;

import java.util.Map;

import org.eclipse.core.runtime.IPath;

/**
 * SVN storage interface. Allows to manage repository locations.
 * 
 * @author Alexander Gurov
 */
public interface ISVNStorage extends IRepositoryLocationFactory {
	int IGNORE_NAME = 0;

	int IGNORE_EXTENSION = 1;

	int IGNORE_PATTERN = 2;

	String PREF_STATE_INFO_LOCATION = "stateInfoLocation";

	String PREF_NO_STORED_AUTHENTICATION = "noStoredAuthentication";

	void initialize(IPath stateInfoLocation) throws Exception;

	void initialize(Map<String, Object> preferences) throws Exception;

	void dispose();

	IRepositoryLocation[] getRepositoryLocations();

	IRepositoryLocation getRepositoryLocation(String id);

	void addRepositoryLocation(IRepositoryLocation location);

	void removeRepositoryLocation(IRepositoryLocation location);

	void reconfigureLocations();

	void saveConfiguration() throws Exception;

	byte[] repositoryResourceAsBytes(IRepositoryResource resource);

	IRepositoryResource repositoryResourceFromBytes(byte[] bytes);
}
