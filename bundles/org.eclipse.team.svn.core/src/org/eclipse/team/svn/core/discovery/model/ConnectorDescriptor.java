/*******************************************************************************
 * Copyright (c) 2009, 2023 Tasktop Technologies, Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tasktop Technologies - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/
package org.eclipse.team.svn.core.discovery.model;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.team.svn.core.SVNMessages;

/**
 * A description of a connector, including kind, description, licensing and brand.
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class ConnectorDescriptor {

	protected java.util.List<ConnectorDescriptorKind> kind = new java.util.ArrayList<>();

	protected String name;

	protected String provider;

	protected String license;

	protected String description;

	protected String siteUrl;

	protected List<String> installableUnits = new ArrayList<>();

	protected String categoryId;

	protected String platformFilter;

	protected String groupId;

	protected java.util.List<FeatureFilter> featureFilter = new java.util.ArrayList<>();

	protected Icon icon;

	protected Overview overview;

	public ConnectorDescriptor() {
	}

	/**
	 * must be one of 'document', 'task', 'vcs'
	 */
	public List<ConnectorDescriptorKind> getKind() {
		return kind;
	}

	public void setKind(List<ConnectorDescriptorKind> kind) {
		this.kind = kind;
	}

	/**
	 * the name of the connector including the name of the organization that produces the repository if appropriate, for example 'Mozilla
	 * Bugzilla'.
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The name of the organization that supplies the connector.
	 */
	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	/**
	 * The short name of the license, for example 'EPL 1.0', 'GPL 2.0', or 'Commercial'.
	 */
	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	/**
	 * A description of the connector. Plug-ins should provide a description, especially if the description is not self-evident from
	 * the @name and
	 * 
	 * @organization.
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * The URL of the update site containing the connector.
	 */
	public String getSiteUrl() {
		return siteUrl;
	}

	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}

	/**
	 * The id of the feature that installs this connector
	 */
	public List<String> getInstallableUnits() {
		return installableUnits;
	}

	public void setInstallableUnits(List<String> id) {
		installableUnits = id;
	}

	/**
	 * the id of the connectorCategory in which this connector belongs
	 */
	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	/**
	 * E.g., "(& (osgi.os=macosx) (osgi.ws=carbon))"
	 */
	public String getPlatformFilter() {
		return platformFilter;
	}

	public void setPlatformFilter(String platformFilter) {
		this.platformFilter = platformFilter;
	}

	/**
	 * The id of the connectorCategory group. See group/@id for more details.
	 */
	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public java.util.List<FeatureFilter> getFeatureFilter() {
		return featureFilter;
	}

	public void setFeatureFilter(java.util.List<FeatureFilter> featureFilter) {
		this.featureFilter = featureFilter;
	}

	public Icon getIcon() {
		return icon;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	public Overview getOverview() {
		return overview;
	}

	public void setOverview(Overview overview) {
		this.overview = overview;
	}

	public void validate() throws ValidationException {
		if (kind == null || kind.isEmpty()) {
			throw new ValidationException(SVNMessages.ConnectorDescriptor_must_specify_connectorDescriptor_kind);
		}
		if (name == null || name.length() == 0) {
			throw new ValidationException(SVNMessages.ConnectorDescriptor_must_specify_connectorDescriptor_name);
		}
		if (provider == null || provider.length() == 0) {
			throw new ValidationException(SVNMessages.ConnectorDescriptor_must_specify_connectorDescriptor_provider);
		}
		if (license == null || license.length() == 0) {
			throw new ValidationException(SVNMessages.ConnectorDescriptor_must_specify_connectorDescriptor_license);
		}
		if (siteUrl == null || siteUrl.length() == 0) {
			throw new ValidationException(SVNMessages.ConnectorDescriptor_must_specify_connectorDescriptor_siteUrl);
		}
		try {
			new java.net.URL(siteUrl);
		} catch (MalformedURLException e) {
			throw new ValidationException(SVNMessages.ConnectorDescriptor_invalid_connectorDescriptor_siteUrl);
		}
		if (installableUnits == null || installableUnits.isEmpty()) {
			throw new ValidationException(SVNMessages.ConnectorDescriptor_must_specify_connectorDescriptor_id);
		}
		if (categoryId == null || categoryId.length() == 0) {
			throw new ValidationException(SVNMessages.ConnectorDescriptor_must_specify_connectorDescriptor_categoryId);
		}
		for (FeatureFilter featureFilterItem : featureFilter) {
			featureFilterItem.validate();
		}
		if (icon != null) {
			icon.validate();
		}
		if (overview != null) {
			overview.validate();
		}
	}
}
