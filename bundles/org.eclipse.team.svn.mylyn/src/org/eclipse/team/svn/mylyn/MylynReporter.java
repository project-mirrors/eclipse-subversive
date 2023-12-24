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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.utility.StringId;
import org.eclipse.team.svn.ui.debugmail.ReportPartsFactory;
import org.eclipse.team.svn.ui.extension.factory.IReporter;
import org.eclipse.team.svn.ui.extension.factory.IReporterFactory.ReportType;
import org.eclipse.team.svn.ui.extension.factory.IReportingDescriptor;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Allows to post a product bug or tip directly to Eclipse.org Bugzilla
 * 
 * @author Alexander Gurov
 */
public class MylynReporter extends AbstractActionOperation implements IReporter {
	protected TaskRepository repository;

	protected AbstractTaskDataHandler taskDataHandler;

	protected IReportingDescriptor settings;

	protected ReportType type;

	protected IStatus problemStatus;

	protected String summary;

	protected String reportId;

	protected String userComment;

	public MylynReporter(TaskRepository repository, AbstractTaskDataHandler taskDataHandler,
			IReportingDescriptor settings, ReportType type) {
		super(MylynMessages.getErrorString("Operation_OpenReportEditor"), MylynMessages.class); //$NON-NLS-1$

		this.settings = settings;
		this.type = type;
		this.repository = repository;
		this.taskDataHandler = taskDataHandler;
		reportId = StringId.generateRandom("ID", 5);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		String kind = repository.getConnectorKind();
		TaskAttributeMapper attributeFactory = taskDataHandler.getAttributeMapper(repository);
		final TaskData taskData = new TaskData(attributeFactory, kind, repository.getRepositoryUrl(), ""); // ID must be empty (but not null) for new task

		boolean isInitializedSuccessfully = taskDataHandler.initializeTaskData(repository, taskData, new TaskMapping() {
			@Override
			public String getSummary() {
				return MylynReporter.this.buildSubject();
			}

			@Override
			public String getTaskKind() {
				return type == ReportType.BUG ? "normal" : "enhancement";
			}

			@Override
			public String getDescription() {
				return MylynReporter.this.buildReport();
			}

			@Override
			public String getProduct() {
				return settings.getProductName();
			}
		}, monitor);

		if (!isInitializedSuccessfully) {
			throw new CoreException(new RepositoryStatus(IStatus.ERROR, SVNMylynIntegrationPlugin.ID,
					RepositoryStatus.ERROR_REPOSITORY, "The selected repository does not support creating new tasks."));
		}

		//does not work for Bugzilla connector
		taskData.getRoot().getMappedAttribute(TaskAttribute.SUMMARY).setValue(buildSubject());
		taskData.getRoot().getMappedAttribute(TaskAttribute.DESCRIPTION).setValue(buildReport());

		// has no public key
		taskData.getRoot()
				.getAttribute(BugzillaAttribute.BUG_SEVERITY.getKey())
				.setValue(type == ReportType.BUG ? "normal" : "enhancement");

		// open task editor
		UIMonitorUtility.getDisplay().syncExec(() -> {
			try {
				TasksUiInternal.createAndOpenNewTask(taskData);
			} catch (CoreException e) {
				MylynReporter.this.reportStatus(IStatus.ERROR, null, e);
			}
		});
	}

	@Override
	public String buildReport() {
		String report = ReportPartsFactory.getVersionPart(settings);
		if (userComment != null) {
			report += ReportPartsFactory.getUserCommentPart(userComment);
		}
		report += ReportPartsFactory.getSVNClientPart();

		if (type == ReportType.BUG) {
			report += ReportPartsFactory.getJVMPropertiesPart();
			if (problemStatus != null) {
				report += ReportPartsFactory.getStatusPart(problemStatus);
			}
		}

		if (!settings.isTrackerSupportsHTML()) {
			report = ReportPartsFactory.removeHTMLTags(report);
		}
		return report;
	}

	@Override
	public String buildSubject() {
		String subject = summary != null ? summary : reportId;
		return subject;
	}

	@Override
	public IReportingDescriptor getReportingDescriptor() {
		return settings;
	}

	@Override
	public boolean isCustomEditorSupported() {
		return true;
	}

	@Override
	public void setProblemStatus(IStatus problemStatus) {
		this.problemStatus = problemStatus;
	}

	@Override
	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Override
	public void setUserComment(String userComment) {
		this.userComment = userComment;
	}

	@Override
	public void setUserEMail(String userEMail) {
	}

	@Override
	public void setUserName(String userName) {
	}

}
