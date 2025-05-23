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

package org.eclipse.team.svn.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.team.svn.core.resource.events.ProjectStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;

/**
 * Project close listener. Clean local and remote status cache, notifys Sunchronize View.
 * 
 * @author Alexander Gurov
 */
public class ProjectCloseListener implements IResourceChangeListener {
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IProject[] projects = { (IProject) event.getResource() };
		SVNRemoteStorage.instance()
				.fireResourceStatesChangedEvent(new ProjectStatesChangedEvent(projects,
						event.getType() == IResourceChangeEvent.PRE_CLOSE
								? ProjectStatesChangedEvent.ST_PRE_CLOSED
								: ProjectStatesChangedEvent.ST_PRE_DELETED));
		/*
		 * Don't clear remote status cache because PersistentRemoteStatusCache impl. depends on
		 * resources existence, i.e. it can can't flush bytes for deleted or closed project.
		 * Note: if you delete a project then all its sync info cache will be deleted
		 */
//		if (RepositoryProvider.getProvider(projects[0], SVNTeamPlugin.NATURE_ID) != null) {
//			CompositeOperation op = new CompositeOperation(SVNUIMessages.Operation_PreCloseDeleteClean);
//			op.add(new ClearUpdateStatusesOperation(projects));
//			op.add(new ClearMergeStatusesOperation(projects));
//			UIMonitorUtility.doTaskScheduledDefault(op);
//		}
	}

}
