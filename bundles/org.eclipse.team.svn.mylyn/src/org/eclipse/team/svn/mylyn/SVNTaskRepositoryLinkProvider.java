/*******************************************************************************
 * Copyright (c) 2008, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.mylyn;

import org.eclipse.core.resources.IResource;
import org.eclipse.mylyn.tasks.core.IRepositoryManager;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractTaskRepositoryLinkProvider;

/**
 * Task repository link provider
 * 
 * @author Alexander Gurov
 */
public class SVNTaskRepositoryLinkProvider extends AbstractTaskRepositoryLinkProvider {

	public TaskRepository getTaskRepository(IResource resource, IRepositoryManager repositoryManager) {
		String url = SVNLinkedTaskInfoAdapterFactory.getBugtraqModel(resource).getUrl();
		if (url != null) {
			for (TaskRepository repository : repositoryManager.getAllRepositories()) {
				String tUrl = repository.getRepositoryUrl();
				if (url.startsWith(tUrl)) {
					return repository;
				}
			}
		}
		return null;
	}

}
