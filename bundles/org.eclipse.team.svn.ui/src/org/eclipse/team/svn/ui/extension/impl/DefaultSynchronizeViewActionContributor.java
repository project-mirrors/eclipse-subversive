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

package org.eclipse.team.svn.ui.extension.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.team.svn.ui.extension.factory.ISynchronizeViewActionContributor;
import org.eclipse.team.svn.ui.extension.impl.synchronize.MergeActionGroup;
import org.eclipse.team.svn.ui.extension.impl.synchronize.OptionsActionGroup;
import org.eclipse.team.svn.ui.extension.impl.synchronize.UpdateActionGroup;

/**
 * Default implementation of the ISynchronizeViewActionContributor
 * 
 * @author Alexander Gurov
 */
public class DefaultSynchronizeViewActionContributor implements ISynchronizeViewActionContributor {

	public DefaultSynchronizeViewActionContributor() {

	}

	public Collection getUpdateContributions() {
		ArrayList actionGroups = new ArrayList();
		actionGroups.add(new UpdateActionGroup());
		actionGroups.add(new OptionsActionGroup());
		return actionGroups;
	}

	public Collection getMergeContributions() {
		ArrayList actionGroups = new ArrayList();
		actionGroups.add(new MergeActionGroup());
		return actionGroups;
	}

	
}
