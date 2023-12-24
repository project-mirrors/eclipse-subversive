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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.event;

import org.eclipse.core.resources.IResource;

/**
 * Resources selection changed event
 * 
 * @author Sergiy Logvin
 */
public class ResourceSelectionChangedEvent {
	public final IResource[] resources;

	public ResourceSelectionChangedEvent(IResource[] resources) {
		this.resources = resources;
	}

}
