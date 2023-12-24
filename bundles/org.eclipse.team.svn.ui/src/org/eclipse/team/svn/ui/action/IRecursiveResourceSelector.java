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

package org.eclipse.team.svn.ui.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.IStateFilter;

/**
 * Conventional interface that allow us to implement generic algorythms with resource selection
 * 
 * @author Alexander Gurov
 */
public interface IRecursiveResourceSelector {
    public IResource []getSelectedResourcesRecursive(IStateFilter filter, int depth);
    public IResource []getSelectedResourcesRecursive(IStateFilter filter);
}
