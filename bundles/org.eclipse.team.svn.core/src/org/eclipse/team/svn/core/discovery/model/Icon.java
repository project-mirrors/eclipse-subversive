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

/**
 * @author David Green
 * @author Igor Burilo
 */
public class Icon {

	protected String image16;

	protected String image32;

	protected String image48;

	protected String image64;

	protected String image128;

	protected ConnectorDescriptor connectorDescriptor;

	protected ConnectorCategory connectorCategory;

	public Icon() {
	}

	public String getImage16() {
		return image16;
	}

	public void setImage16(String image16) {
		this.image16 = image16;
	}

	public String getImage32() {
		return image32;
	}

	public void setImage32(String image32) {
		this.image32 = image32;
	}

	public String getImage48() {
		return image48;
	}

	public void setImage48(String image48) {
		this.image48 = image48;
	}

	public String getImage64() {
		return image64;
	}

	public void setImage64(String image64) {
		this.image64 = image64;
	}

	public String getImage128() {
		return image128;
	}

	public void setImage128(String image128) {
		this.image128 = image128;
	}

	public ConnectorDescriptor getConnectorDescriptor() {
		return connectorDescriptor;
	}

	public void setConnectorDescriptor(ConnectorDescriptor connectorDescriptor) {
		this.connectorDescriptor = connectorDescriptor;
	}

	public ConnectorCategory getConnectorCategory() {
		return connectorCategory;
	}

	public void setConnectorCategory(ConnectorCategory connectorCategory) {
		this.connectorCategory = connectorCategory;
	}

	public void validate() throws ValidationException {
	}
}
