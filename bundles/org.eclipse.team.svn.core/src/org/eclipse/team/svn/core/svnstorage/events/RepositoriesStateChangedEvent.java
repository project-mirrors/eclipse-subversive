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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.svnstorage.events;

import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * Event is sent when the repository location is added or removed.
 * 
 * @author Alexei Goncharov
 */
public class RepositoriesStateChangedEvent {

	public static final int ADDED = 0;

	public static final int REMOVED = 1;

	protected int eventType;

	protected IRepositoryLocation location;

	public RepositoriesStateChangedEvent(IRepositoryLocation location, int action) {
		this.location = location;
		this.eventType = action;
	}

	public int getAction() {
		return this.eventType;
	}

	public IRepositoryLocation getLocation() {
		return this.location;
	}

}
