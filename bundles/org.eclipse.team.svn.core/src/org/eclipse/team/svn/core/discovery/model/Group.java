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
 * groups provide a way to anchor connectors in a grouping with other like entries.
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class Group {

	protected String id;

	protected ConnectorCategory connectorCategory;

	public Group() {
	}

	/**
	 * An identifier that identifies the group. Must be unique for a particular connectorCategory.
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ConnectorCategory getConnectorCategory() {
		return connectorCategory;
	}

	public void setConnectorCategory(ConnectorCategory connectorCategory) {
		this.connectorCategory = connectorCategory;
	}

	public void validate() throws ValidationException {
		if (id == null || id.length() == 0) {
			throw new ValidationException(SVNMessages.Group_must_specify_group_id);
		}
	}
}
