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

import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * This event is sent when the revision property is changed.
 * 
 * @author Alexei Goncharov
 */
public class RevisonPropertyChangeEvent {

	public static final int SET = 0;

	public static final int REMOVED = 1;

	protected int action;

	protected SVNRevision revision;

	protected SVNProperty property;

	protected IRepositoryLocation location;

	public RevisonPropertyChangeEvent(int action, SVNRevision revision, IRepositoryLocation location,
			SVNProperty property) {
		this.revision = revision;
		this.property = property;
		this.action = action;
		this.location = location;
	}

	public int getAction() {
		return action;
	}

	public SVNRevision getRevision() {
		return revision;
	}

	public SVNProperty getProperty() {
		return property;
	}

	public IRepositoryLocation getLocation() {
		return location;
	}
}
