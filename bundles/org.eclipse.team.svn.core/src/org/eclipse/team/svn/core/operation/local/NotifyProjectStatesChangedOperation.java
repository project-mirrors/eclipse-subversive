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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.events.ProjectStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;

/**
 * Fire project state changed event
 * 
 * @author Alexander Gurov
 */
public class NotifyProjectStatesChangedOperation extends AbstractActionOperation {
	protected IProject[] projects;

	protected int eventType;

	public NotifyProjectStatesChangedOperation(IProject[] projects, int eventType) {
		super("Operation_NotifyProjectChange", SVNMessages.class); //$NON-NLS-1$
		this.projects = projects;
		this.eventType = eventType;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		SVNRemoteStorage.instance().fireResourceStatesChangedEvent(new ProjectStatesChangedEvent(projects, eventType));
	}

}
