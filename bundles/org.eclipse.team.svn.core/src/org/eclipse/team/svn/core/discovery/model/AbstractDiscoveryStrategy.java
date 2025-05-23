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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An abstraction of a strategy for discovering connectors and categories. Strategy design pattern. Note that strategies are not reusable
 * and must be disposed.
 * 
 * @author David Green
 * @author Igor Burilo
 */
public abstract class AbstractDiscoveryStrategy {

	protected List<DiscoveryCategory> categories;

	protected List<DiscoveryConnector> connectors;

	/**
	 * Perform discovery and add discovered items to {@link #getCategories() categories} and {@link #getConnectors()}.
	 * 
	 * @param monitor
	 *            the monitor
	 */
	public abstract void performDiscovery(IProgressMonitor monitor) throws CoreException;

	public List<DiscoveryCategory> getCategories() {
		return categories;
	}

	public void setCategories(List<DiscoveryCategory> categories) {
		this.categories = categories;
	}

	public List<DiscoveryConnector> getConnectors() {
		return connectors;
	}

	public void setConnectors(List<DiscoveryConnector> connectors) {
		this.connectors = connectors;
	}

	public void dispose() {
	}
}
