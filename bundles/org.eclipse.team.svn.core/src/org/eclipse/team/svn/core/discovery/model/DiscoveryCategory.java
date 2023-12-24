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

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class DiscoveryCategory extends ConnectorCategory {
	private AbstractDiscoverySource source;

	private List<DiscoveryConnector> connectors = new ArrayList<DiscoveryConnector>();

	public List<DiscoveryConnector> getConnectors() {
		return connectors;
	}

	public AbstractDiscoverySource getSource() {
		return source;
	}

	public void setSource(AbstractDiscoverySource source) {
		this.source = source;
	}
}
