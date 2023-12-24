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

package org.eclipse.team.svn.ui.discovery;

import java.util.List;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.svn.core.discovery.model.ConnectorDescriptor;

/**
 * Interface for install jobs
 * 
 * @author Igor Burilo
 */
public interface IConnectorsInstallJob extends IRunnableWithProgress {

	void setInstallableConnectors(List<ConnectorDescriptor> installableConnectors);
}
