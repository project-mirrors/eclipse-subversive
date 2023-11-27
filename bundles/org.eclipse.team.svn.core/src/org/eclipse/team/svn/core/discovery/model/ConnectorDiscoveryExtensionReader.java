/*******************************************************************************
 * Copyright (c) 2009 Task top Technologies, Polarion Software and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.svn.core.discovery.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.team.svn.core.SVNMessages;

/**
 * Connector Discovery extension point reader, for extension points of type
 * <tt>org.eclipse.team.svn.core.connectorDiscovery</tt>
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class ConnectorDiscoveryExtensionReader {

	public static final String EXTENSION_POINT_ID = "org.eclipse.team.svn.core.connectorDiscovery"; //$NON-NLS-1$

	public static final String CONNECTOR_DESCRIPTOR = "connectorDescriptor"; //$NON-NLS-1$

	public static final String CONNECTOR_CATEGORY = "connectorCategory"; //$NON-NLS-1$

	public static final String ICON = "icon"; //$NON-NLS-1$

	public static final String OVERVIEW = "overview"; //$NON-NLS-1$

	public static final String FEATURE_FILTER = "featureFilter"; //$NON-NLS-1$

	public static final String GROUP = "group"; //$NON-NLS-1$

	public ConnectorDescriptor readConnectorDescriptor(IConfigurationElement element) throws ValidationException {
		return readConnectorDescriptor(element, ConnectorDescriptor.class);
	}

	public <T extends ConnectorDescriptor> T readConnectorDescriptor(IConfigurationElement element, Class<T> clazz)
			throws ValidationException {
		T connectorDescriptor;
		try {
			connectorDescriptor = clazz.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		try {
			String kinds = element.getAttribute("kind"); //$NON-NLS-1$
			if (kinds != null) {
				String[] akinds = kinds.split("\\s*,\\s*"); //$NON-NLS-1$
				for (String kind : akinds) {
					connectorDescriptor.getKind().add(ConnectorDescriptorKind.fromValue(kind));
				}
			}
		} catch (IllegalArgumentException e) {
			throw new ValidationException(SVNMessages.ConnectorDiscoveryExtensionReader_unexpected_value_kind);
		}		
		
		String ids = element.getAttribute("id"); //$NON-NLS-1$
		if (ids != null) {
			String[] aids = ids.split("\\s*,\\s*"); //$NON-NLS-1$
			for (String id : aids) {
				connectorDescriptor.getInstallableUnits().add(id);
			}
		}		
		
		connectorDescriptor.setName(element.getAttribute("name")); //$NON-NLS-1$
		connectorDescriptor.setProvider(element.getAttribute("provider")); //$NON-NLS-1$
		connectorDescriptor.setLicense(element.getAttribute("license")); //$NON-NLS-1$
		connectorDescriptor.setDescription(element.getAttribute("description")); //$NON-NLS-1$
		connectorDescriptor.setSiteUrl(element.getAttribute("siteUrl")); //$NON-NLS-1$		
		connectorDescriptor.setCategoryId(element.getAttribute("categoryId")); //$NON-NLS-1$
		connectorDescriptor.setPlatformFilter(element.getAttribute("platformFilter")); //$NON-NLS-1$
		connectorDescriptor.setGroupId(element.getAttribute("groupId")); //$NON-NLS-1$

		for (IConfigurationElement child : element.getChildren("featureFilter")) { //$NON-NLS-1$
			FeatureFilter featureFilterItem = readFeatureFilter(child);
			featureFilterItem.setConnectorDescriptor(connectorDescriptor);
			connectorDescriptor.getFeatureFilter().add(featureFilterItem);
		}
		for (IConfigurationElement child : element.getChildren("icon")) { //$NON-NLS-1$
			Icon iconItem = readIcon(child);
			iconItem.setConnectorDescriptor(connectorDescriptor);
			if (connectorDescriptor.getIcon() != null) {
				throw new ValidationException(SVNMessages.ConnectorDiscoveryExtensionReader_unexpected_element_icon);
			}
			connectorDescriptor.setIcon(iconItem);
		}
		for (IConfigurationElement child : element.getChildren("overview")) { //$NON-NLS-1$
			Overview overviewItem = readOverview(child);
			overviewItem.setConnectorDescriptor(connectorDescriptor);
			if (connectorDescriptor.getOverview() != null) {
				throw new ValidationException(SVNMessages.ConnectorDiscoveryExtensionReader_unexpected_element_overview);
			}
			connectorDescriptor.setOverview(overviewItem);
		}

		connectorDescriptor.validate();

		return connectorDescriptor;
	}

	public ConnectorCategory readConnectorCategory(IConfigurationElement element) throws ValidationException {
		return readConnectorCategory(element, ConnectorCategory.class);
	}

	public <T extends ConnectorCategory> T readConnectorCategory(IConfigurationElement element, Class<T> clazz)
			throws ValidationException {
		T connectorCategory;
		try {
			connectorCategory = clazz.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		connectorCategory.setId(element.getAttribute("id")); //$NON-NLS-1$
		connectorCategory.setName(element.getAttribute("name")); //$NON-NLS-1$
		connectorCategory.setDescription(element.getAttribute("description")); //$NON-NLS-1$
		connectorCategory.setRelevance(element.getAttribute("relevance")); //$NON-NLS-1$

		for (IConfigurationElement child : element.getChildren("icon")) { //$NON-NLS-1$
			Icon iconItem = readIcon(child);
			iconItem.setConnectorCategory(connectorCategory);
			if (connectorCategory.getIcon() != null) {
				throw new ValidationException(SVNMessages.ConnectorDiscoveryExtensionReader_unexpected_element_icon);
			}
			connectorCategory.setIcon(iconItem);
		}
		for (IConfigurationElement child : element.getChildren("overview")) { //$NON-NLS-1$
			Overview overviewItem = readOverview(child);
			overviewItem.setConnectorCategory(connectorCategory);
			if (connectorCategory.getOverview() != null) {
				throw new ValidationException(SVNMessages.ConnectorDiscoveryExtensionReader_unexpected_element_overview);
			}
			connectorCategory.setOverview(overviewItem);
		}
		for (IConfigurationElement child : element.getChildren("group")) { //$NON-NLS-1$
			Group groupItem = readGroup(child);
			groupItem.setConnectorCategory(connectorCategory);
			connectorCategory.getGroup().add(groupItem);
		}

		connectorCategory.validate();

		return connectorCategory;
	}

	public Icon readIcon(IConfigurationElement element) throws ValidationException {
		Icon icon = new Icon();

		icon.setImage16(element.getAttribute("image16")); //$NON-NLS-1$
		icon.setImage32(element.getAttribute("image32")); //$NON-NLS-1$
		icon.setImage48(element.getAttribute("image48")); //$NON-NLS-1$
		icon.setImage64(element.getAttribute("image64")); //$NON-NLS-1$
		icon.setImage128(element.getAttribute("image128")); //$NON-NLS-1$

		icon.validate();

		return icon;
	}

	public Overview readOverview(IConfigurationElement element) throws ValidationException {
		Overview overview = new Overview();

		overview.setSummary(element.getAttribute("summary")); //$NON-NLS-1$
		overview.setUrl(element.getAttribute("url")); //$NON-NLS-1$
		overview.setScreenshot(element.getAttribute("screenshot")); //$NON-NLS-1$

		overview.validate();

		return overview;
	}

	public FeatureFilter readFeatureFilter(IConfigurationElement element) throws ValidationException {
		FeatureFilter featureFilter = new FeatureFilter();

		featureFilter.setFeatureId(element.getAttribute("featureId")); //$NON-NLS-1$
		featureFilter.setVersion(element.getAttribute("version")); //$NON-NLS-1$

		featureFilter.validate();

		return featureFilter;
	}

	public Group readGroup(IConfigurationElement element) throws ValidationException {
		Group group = new Group();

		group.setId(element.getAttribute("id")); //$NON-NLS-1$

		group.validate();

		return group;
	}

}
