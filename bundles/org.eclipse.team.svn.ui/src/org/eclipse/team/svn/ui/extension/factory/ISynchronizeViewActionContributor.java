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
 *    Jiri Walek - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.extension.factory;

import java.util.Collection;

import org.eclipse.team.svn.ui.synchronize.AbstractSynchronizeActionGroup;

/**
 * Synchronize action contributor
 * 
 * @author Jiri Walek
 */
public interface ISynchronizeViewActionContributor {
    /**
     * This method returns synchronize view action contributions for update mode
     * @return collection of AbstractSynchronizeActionGroup
     */
	public Collection<AbstractSynchronizeActionGroup> getUpdateContributions();
    /**
     * This method returns synchronize view action contributions for merge mode
     * @return collection of AbstractSynchronizeActionGroup
     */
	public Collection<AbstractSynchronizeActionGroup> getMergeContributions();
}
