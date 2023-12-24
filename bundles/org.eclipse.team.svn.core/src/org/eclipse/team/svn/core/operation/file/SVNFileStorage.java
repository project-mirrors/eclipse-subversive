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

package org.eclipse.team.svn.core.operation.file;

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.AbstractSVNStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVN storage provider based on java.io.File
 * 
 * @author Alexander Gurov
 */
public class SVNFileStorage extends AbstractSVNStorage implements IFileStorage {
	/**
	 * The name of the preferences node in the Subversive Core preferences that contains the known repositories as its children (for
	 * integrations).
	 */
	public static final String PREF_REPOSITORIES_NODE = "externalRepositories"; //$NON-NLS-1$

	/**
	 * The name of the preferences node in the Subversive Core preferences that contains the flag which determines whether we migrated from
	 * Authorization database to Equinox security storage
	 */
	public static final String PREF_MIGRATE_FROM_AUTH_DB_NODE = "externalMigrateFromAuthorizationDatabase"; //$NON-NLS-1$

	/**
	 * The name of file containing the SVN repository locations information (for integration). Deprecated since Subversive 0.7.0 v20080404 -
	 * must not be used. The valid information is stored in preferences.
	 * 
	 * @see SVNFileStorage.PREF_REPOSITORIES_NODE
	 */
	public static final String STATE_INFO_FILE_NAME = ".externalSVNRepositories"; //$NON-NLS-1$

	private static SVNFileStorage instance = new SVNFileStorage();

	public static SVNFileStorage instance() {
		return SVNFileStorage.instance;
	}

	@Override
	public void initialize(Map<String, Object> preferences) throws Exception {
		preferences.put(AbstractSVNStorage.IPREF_STATE_INFO_FILE, SVNFileStorage.STATE_INFO_FILE_NAME);
		preferences.put(AbstractSVNStorage.IPREF_REPO_NODE_NAME, SVNFileStorage.PREF_REPOSITORIES_NODE);
		preferences.put(AbstractSVNStorage.IPREF_AUTH_NODE_NAME, SVNFileStorage.PREF_MIGRATE_FROM_AUTH_DB_NODE);
		super.initialize(preferences);
	}

	@Override
	public IRepositoryResource asRepositoryResource(File file, boolean allowsNull) {
		// check if this resource is placed in working copy
		File wcRoot = file;
		SVNEntryInfo info = null;
		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			while (info == null) {
				if (wcRoot == null) {
					// no WC found
					if (allowsNull) {
						return null;
					}
					throw new RuntimeException(file.getAbsolutePath() + " is not under version control");
				}
				info = SVNUtility.getSVNInfo(wcRoot, proxy);
				if (info == null) {
					wcRoot = wcRoot.getParentFile();
				}
			}
		} finally {
			proxy.dispose();
		}

		String wcUrl = SVNUtility.decodeURL(info.url);
		String rootUrl = SVNUtility.decodeURL(info.reposRootUrl);
		IRepositoryLocation location = findLocation(wcUrl, rootUrl);

		if (wcRoot != file) {
			wcUrl += file.getAbsolutePath().substring(wcRoot.getAbsolutePath().length());
		}

		return file.isFile()
				? (IRepositoryResource) location.asRepositoryFile(wcUrl, allowsNull)
				: location.asRepositoryContainer(wcUrl, allowsNull);
	}

	protected IRepositoryLocation findLocation(String resourceUrl, String rootUrl) {
		IPath url = SVNUtility.createPathForSVNUrl(resourceUrl);
		IRepositoryLocation[] locations = getRepositoryLocations();
		for (IRepositoryLocation location : locations) {
			if (SVNUtility.createPathForSVNUrl(location.getUrl()).isPrefixOf(url)) {
				return location;
			}
		}
		for (IRepositoryLocation location : locations) {
			if (location.getRepositoryRootUrl().equals(rootUrl)) {
				return location;
			}
		}
		IRepositoryLocation location = this.newRepositoryLocation();
		SVNUtility.initializeRepositoryLocation(location, rootUrl);
		addRepositoryLocation(location);
		return location;
	}

	private SVNFileStorage() {
	}

	@Override
	protected IRepositoryLocation wrapLocationIfRequired(IRepositoryLocation location, String url, boolean isFile) {
		return location;
	}

}
