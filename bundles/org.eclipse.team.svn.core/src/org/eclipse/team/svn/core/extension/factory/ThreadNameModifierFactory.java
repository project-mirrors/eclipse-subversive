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

package org.eclipse.team.svn.core.extension.factory;

import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNManager;

/**
 * Wraps real factories in order to produce wrapped SVN connectors.
 * 
 * @author Alexander Gurov
 */
public class ThreadNameModifierFactory implements ISVNConnectorFactory {
	protected ISVNConnectorFactory factory;

	public ThreadNameModifierFactory(ISVNConnectorFactory factory) {
		this.factory = factory;
	}

	@Override
	public String getClientVersion() {
		return factory.getClientVersion();
	}

	@Override
	public String getCompatibilityVersion() {
		return factory.getCompatibilityVersion();
	}

	@Override
	public String getId() {
		return factory.getId();
	}

	@Override
	public String getName() {
		return factory.getName();
	}

	@Override
	public String getVersion() {
		return factory.getVersion();
	}

	@Override
	public int getSupportedFeatures() {
		return factory.getSupportedFeatures();
	}

	@Override
	public int getSVNAPIVersion() {
		return factory.getSVNAPIVersion();
	}

	@Override
	public ISVNConnector createConnector() {
		return new ThreadNameModifier(factory.createConnector());
	}

	@Override
	public ISVNManager createManager() {
		return factory.createManager();
	}

}
