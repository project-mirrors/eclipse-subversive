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

package org.eclipse.team.svn.core.resource;

import java.util.Collection;

import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.resource.events.IRepositoryLocationStateListener;

/**
 * Repository location
 * 
 * @author Alexander Gurov
 */
public interface IRepositoryLocation extends IRepositoryBase, IRepositoryResourceFactory {

	/**
	 * Detect what information should be present while serializing location:
	 * 
	 * ALL - save all data. For example, it's used for storing repository locations between Eclipse sessions.
	 * 
	 * WITHOUT_REVISION_COMMENTS - it's used for storing all info except revision link comments. For example, it's used for storing
	 * locations in project set files, as comment size can be very big.
	 * 
	 * ONLY_REQUIRED_DATA - it's stored only necessary data required for restoring location. For example, it's used by setting location
	 * property for project; we make location size as small as possible because project's property size is limited by Eclipse.
	 */
	public enum LocationReferenceTypeEnum {
		ALL, WITHOUT_REVISION_COMMENTS, ONLY_REQUIRED_DATA
	}

	void addStateListener(IRepositoryLocationStateListener listener);

	void removeStateListener(IRepositoryLocationStateListener listener);

	String getId();

	void setLabel(String label);

	String getLabel();

	/*
	 * As in some cases there can be limitations on reference string size or some
	 * information is not needed, then we provide a parameter which says what
	 * information should be saved.
	 * For more details, see LocationReferenceTypeEnum
	 */
	String asReference(LocationReferenceTypeEnum locationReferenceType);

	void fillLocationFromReference(String[] referenceParts);

	String getUrlAsIs();

	void setUrl(String url);

	String getRepositoryRootUrl();

	String getRepositoryUUID();

	IRepositoryRoot getRepositoryRoot();

	IRepositoryRoot getRoot();

	void setStructureEnabled(boolean enabled);

	boolean isStructureEnabled();

	void setTrunkLocation(String location);

	String getTrunkLocation();

	String getUserInputTrunk();

	String getBranchesLocation();

	void setBranchesLocation(String location);

	String getUserInputBranches();

	String getTagsLocation();

	void setTagsLocation(String location);

	String getUserInputTags();

	IRevisionLink[] getRevisionLinks();

	void addRevisionLink(IRevisionLink link);

	void removeRevisionLink(IRevisionLink link);

	String getUsername();

	void setUsername(String username);

	String getPassword();

	void setPassword(String password);

	boolean isPasswordSavedForRealm(String realm);

	boolean isPasswordSaved();

	void setPasswordSaved(boolean saved);

	ISVNConnector acquireSVNProxy();

	void releaseSVNProxy(ISVNConnector proxy);

	void reconfigure();

	void dispose();

	SSLSettings getSSLSettings();

	SSHSettings getSSHSettings();

	Collection<String> getRealms();

	void addRealm(String realm, IRepositoryLocation location);

	void removeRealm(String realm);

	Collection<IRepositoryLocation> getRealmLocations();

	IRepositoryLocation getLocationForRealm(String realm);

	boolean isAuthorNameEnabled();

	String getAuthorName();

	void setAuthorNameEnabled(boolean isEnabled);

	void setAuthorName(String name);
}
