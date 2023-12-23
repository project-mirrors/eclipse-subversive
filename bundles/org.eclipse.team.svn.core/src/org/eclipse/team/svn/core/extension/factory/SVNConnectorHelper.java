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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.extension.factory;


/**
 * Helper class for svn connectors
 * 
 * @author Igor Burilo
 */
public class SVNConnectorHelper {

	public static String getConnectorName(ISVNConnectorFactory connectorFactory) {
		if (connectorFactory.getVersion().compareTo("2.2.1.I20090925-2100") > 0) { //$NON-NLS-1$
			return connectorFactory.getName();
		} else {
			return connectorFactory.getName() + " (" + connectorFactory.getClientVersion().replace('\n', ' ') + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
