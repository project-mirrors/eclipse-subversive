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
 *    Andrey Loskutov - [scalability] SVN update takes hours if "Synchronize" view is opened
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.resource.events;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * Project state changed event implementation
 * 
 * @author Alexander Gurov
 */
public class ProjectStatesChangedEvent extends ResourceStatesChangedEvent {
	public static final int ST_POST_SHARED = 0;

	public static final int ST_POST_DISCONNECTED = 1;

	public static final int ST_PRE_SHARED = 2;

	public static final int ST_PRE_DISCONNECTED = 3;

	// POST_CLOSE/POST_DELETE events are not provided by Eclipse Platform
	public static final int ST_PRE_CLOSED = 4;

	public static final int ST_PRE_DELETED = 5;

	public final int newState;

	public ProjectStatesChangedEvent(IProject[] projects, int newState) {
		super(projects, IResource.DEPTH_ZERO, ResourceStatesChangedEvent.CHANGED_NODES);
		this.newState = newState;
	}

	@Override
	public boolean canMerge(ResourceStatesChangedEvent e) {
		if (e instanceof ProjectStatesChangedEvent) {
			return super.canMerge(e) && newState == ((ProjectStatesChangedEvent) e).newState;
		}
		return false;
	}

	@Override
	public ProjectStatesChangedEvent merge(ResourceStatesChangedEvent event) {
		IProject[] arr = new IProject[resources.length + event.resources.length];
		System.arraycopy(resources, 0, arr, 0, resources.length);
		System.arraycopy(event.resources, 0, arr, resources.length, event.resources.length);
		return new ProjectStatesChangedEvent(arr, newState);
	}

	@Override
	public int hashCode() {
		return Objects.hash(newState, Arrays.hashCode(resources));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ProjectStatesChangedEvent)) {
			return false;
		}
		ProjectStatesChangedEvent other = (ProjectStatesChangedEvent) obj;
		if ((newState != other.newState) || !Arrays.equals(resources, other.resources)) {
			return false;
		}
		return true;
	}
}
