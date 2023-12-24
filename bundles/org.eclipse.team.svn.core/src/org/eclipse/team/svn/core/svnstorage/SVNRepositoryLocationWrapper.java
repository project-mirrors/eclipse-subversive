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

package org.eclipse.team.svn.core.svnstorage;

import java.util.Collection;

import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.resource.IRevisionLink;
import org.eclipse.team.svn.core.resource.SSHSettings;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.resource.events.IRepositoryLocationStateListener;

/**
 * Allows to redefine the original location URL
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryLocationWrapper implements IRepositoryLocation {
	protected IRepositoryLocation location;

	protected String url;

	protected String repositoryRootUrl;

	protected String repositoryUUID;

	public SVNRepositoryLocationWrapper(IRepositoryLocation location, String mappedUrl) {
		this.location = location;
		url = mappedUrl;
	}

	@Override
	public void addStateListener(IRepositoryLocationStateListener listener) {
		location.addStateListener(listener);
	}

	@Override
	public void removeStateListener(IRepositoryLocationStateListener listener) {
		location.removeStateListener(listener);
	}

	@Override
	public String asReference(LocationReferenceTypeEnum locationReferenceType) {
		return location.asReference(locationReferenceType);
	}

	@Override
	public void fillLocationFromReference(String[] referenceParts) {
		location.fillLocationFromReference(referenceParts);
	}

	@Override
	public String getUrlAsIs() {
		return url;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public IRepositoryContainer asRepositoryContainer(String url, boolean allowsNull) {
		return SVNRepositoryLocation.asRepositoryContainer(this, url, allowsNull);
	}

	@Override
	public IRepositoryFile asRepositoryFile(String url, boolean allowsNull) {
		return SVNRepositoryLocation.asRepositoryFile(this, url, allowsNull);
	}

	@Override
	public IRepositoryRoot getRepositoryRoot() {
		return new SVNRepositoryRoot(this);
	}

	@Override
	public IRepositoryRoot getRoot() {
		return new SVNRepositoryLocationRoot(this);
	}

	@Override
	public String getRepositoryRootUrl() {
		if (repositoryRootUrl == null) {
			fetchRepoInfo();
		}
		return repositoryRootUrl == null ? getUrl() : repositoryRootUrl;
	}

	@Override
	public String getRepositoryUUID() {
		if (repositoryUUID == null) {
			fetchRepoInfo();
		}
		return repositoryUUID;
	}

	@Override
	public ISVNConnector acquireSVNProxy() {
		return location.acquireSVNProxy();
	}

	@Override
	public void addRealm(String realm, IRepositoryLocation location) {
		this.location.addRealm(realm, location);
	}

	@Override
	public void addRevisionLink(IRevisionLink link) {
		location.addRevisionLink(link);
	}

	@Override
	public void dispose() {
		location.dispose();
	}

	@Override
	public String getBranchesLocation() {
		return location.getBranchesLocation();
	}

	@Override
	public String getId() {
		return location.getId();
	}

	@Override
	public String getLabel() {
		return location.getLabel();
	}

	@Override
	public IRepositoryLocation getLocationForRealm(String realm) {
		return location.getLocationForRealm(realm);
	}

	@Override
	public String getPassword() {
		return location.getPassword();
	}

	@Override
	public Collection<IRepositoryLocation> getRealmLocations() {
		return location.getRealmLocations();
	}

	@Override
	public Collection<String> getRealms() {
		return location.getRealms();
	}

	@Override
	public IRevisionLink[] getRevisionLinks() {
		return location.getRevisionLinks();
	}

	@Override
	public SSHSettings getSSHSettings() {
		return location.getSSHSettings();
	}

	@Override
	public SSLSettings getSSLSettings() {
		return location.getSSLSettings();
	}

	@Override
	public String getTagsLocation() {
		return location.getTagsLocation();
	}

	@Override
	public String getTrunkLocation() {
		return location.getTrunkLocation();
	}

	@Override
	public String getUserInputBranches() {
		return location.getUserInputBranches();
	}

	@Override
	public String getUserInputTags() {
		return location.getUserInputTags();
	}

	@Override
	public String getUserInputTrunk() {
		return location.getUserInputTrunk();
	}

	@Override
	public String getUsername() {
		return location.getUsername();
	}

	@Override
	public boolean isPasswordSaved() {
		return location.isPasswordSaved();
	}

	@Override
	public boolean isStructureEnabled() {
		return location.isStructureEnabled();
	}

	@Override
	public void reconfigure() {
		location.reconfigure();
	}

	@Override
	public void releaseSVNProxy(ISVNConnector proxy) {
		location.releaseSVNProxy(proxy);
	}

	@Override
	public void removeRealm(String realm) {
		location.removeRealm(realm);
	}

	@Override
	public void removeRevisionLink(IRevisionLink link) {
		location.removeRevisionLink(link);
	}

	@Override
	public void setBranchesLocation(String location) {
		this.location.setBranchesLocation(location);
	}

	@Override
	public void setLabel(String label) {
		location.setLabel(label);
	}

	@Override
	public void setPassword(String password) {
		location.setPassword(password);
	}

	@Override
	public void setPasswordSaved(boolean saved) {
		location.setPasswordSaved(saved);
	}

	@Override
	public void setStructureEnabled(boolean enabled) {
		location.setStructureEnabled(enabled);
	}

	@Override
	public void setTagsLocation(String location) {
		this.location.setTagsLocation(location);
	}

	@Override
	public void setTrunkLocation(String location) {
		this.location.setTrunkLocation(location);
	}

	@Override
	public void setUsername(String username) {
		location.setUsername(username);
	}

	@Override
	public String getName() {
		return location.getName();
	}

	@Override
	public boolean isAuthorNameEnabled() {
		return location.isAuthorNameEnabled();
	}

	@Override
	public String getAuthorName() {
		return location.getAuthorName();
	}

	@Override
	public void setAuthorNameEnabled(boolean isEnabled) {
		location.setAuthorNameEnabled(isEnabled);
	}

	@Override
	public void setAuthorName(String name) {
		location.setAuthorName(name);
	}

	@Override
	public Object getAdapter(Class adapter) {
		return location.getAdapter(adapter);
	}

	protected void fetchRepoInfo() {
		String[] values = SVNRepositoryLocation.fetchRepoInfo(this, true);
		repositoryRootUrl = values[0];
		repositoryUUID = values[1];
	}

	@Override
	public boolean isPasswordSavedForRealm(String realm) {
		return location.isPasswordSavedForRealm(realm);
	}

	@Override
	public boolean equals(Object obj) {
		return location.equals(obj);
	}
}
