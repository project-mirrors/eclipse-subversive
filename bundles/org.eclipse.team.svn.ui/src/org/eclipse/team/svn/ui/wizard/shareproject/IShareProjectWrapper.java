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
 *    Alexander Gurov - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.wizard.shareproject;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.IResourceProvider;

/**
 * Conventional interface in order to provide flexible share project API's
 * 
 * @author Alexander Gurov
 */
public interface IShareProjectWrapper extends IActionOperation, IResourceProvider {
	public IProject []getProjects();
}
