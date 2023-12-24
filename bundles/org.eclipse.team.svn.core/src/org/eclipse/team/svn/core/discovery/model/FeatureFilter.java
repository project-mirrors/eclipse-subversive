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

import org.eclipse.team.svn.core.SVNMessages;

/**
 * A means of specifying that a feature must be present in order for the connectorDescriptor to be presented to the user.
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class FeatureFilter {

	protected String featureId;

	protected String version;

	protected ConnectorDescriptor connectorDescriptor;

	public FeatureFilter() {
	}

	/**
	 * The id of the feature to test
	 */
	public String getFeatureId() {
		return featureId;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	/**
	 * A version specifier, specified in the same manner as version dependencies are specified in an OSGi manifest. For example: "[3.0,4.0)"
	 */
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public ConnectorDescriptor getConnectorDescriptor() {
		return connectorDescriptor;
	}

	public void setConnectorDescriptor(ConnectorDescriptor connectorDescriptor) {
		this.connectorDescriptor = connectorDescriptor;
	}

	public void validate() throws ValidationException {
		if (featureId == null || featureId.length() == 0) {
			throw new ValidationException(SVNMessages.FeatureFilter_must_specify_featureFilter_featureId);
		}
		if (version == null || version.length() == 0) {
			throw new ValidationException(SVNMessages.FeatureFilter_must_specify_featureFilter_version);
		}
	}
}
