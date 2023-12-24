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
public class Overview {

	protected String summary;

	protected String url;

	protected String screenshot;

	protected ConnectorDescriptor connectorDescriptor;

	protected ConnectorCategory connectorCategory;

	public Overview() {
	}

	/**
	 * A description providing detailed information about the item. Newlines can be used to format the text into multiple paragraphs if
	 * necessary. Text must fit into an area 320x240, otherwise it will be truncated in the UI. More lengthy descriptions can be provided on
	 * a web page if required, see @url.
	 */
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * An URL that points to a web page with more information relevant to the connector or category.
	 */
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * 320x240 PNG, JPEG or GIF
	 */
	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
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
