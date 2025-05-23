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

package org.eclipse.team.svn.core;

import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;

/**
 * IProject attachement type
 * 
 * @author Alexander Gurov
 */
public class SVNTeamProviderType extends RepositoryProviderType {

	public SVNTeamProviderType() {
	}

	@Override
	public ProjectSetCapability getProjectSetCapability() {
		return new SVNTeamProjectSetCapability();
	}

	@Override
	public Subscriber getSubscriber() {
		return UpdateSubscriber.instance();
	}

}
