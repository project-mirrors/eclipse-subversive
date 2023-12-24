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

import org.eclipse.mylyn.team.ui.AbstractTaskReference;

/**
 * ILinkedTaskInfo implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNLinkedTaskInfo extends AbstractTaskReference {
	protected String repositoryUrl;

	protected String taskId;

	protected String taskFullUrl;

	protected String comment;

	public SVNLinkedTaskInfo(String repositoryUrl, String taskId, String taskFullUrl, String comment) {
		this.repositoryUrl = repositoryUrl;
		this.taskId = taskId;
		this.taskFullUrl = taskFullUrl;
		this.comment = comment;
	}

	@Override
	public String getText() {
		return comment;
	}

	@Override
	public String getRepositoryUrl() {
		return repositoryUrl;
	}

	@Override
	public String getTaskUrl() {
		return taskFullUrl;
	}

	@Override
	public String getTaskId() {
		return taskId;
	}

}
