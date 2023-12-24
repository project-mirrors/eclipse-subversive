/*******************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    See git history
 *******************************************************************************/

package org.eclipse.team.svn.mylyn;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.tasks.bugs.AbstractSupportHandler;
import org.eclipse.mylyn.tasks.bugs.ISupportRequest;
import org.eclipse.mylyn.tasks.bugs.ISupportResponse;
import org.eclipse.mylyn.tasks.bugs.ITaskContribution;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.ui.debugmail.ReportPartsFactory;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.extension.factory.IReportingDescriptor;

public class SVNTeamSupportHandler extends AbstractSupportHandler {

	@Override
	public void preProcess(ISupportRequest request) {
		super.preProcess(request);
		ITaskContribution contribution = request.getDefaultContribution();
		if (isSubversiveReport(contribution)) {
			appendToDescription(contribution);
		}
	}

	@Override
	public void process(ITaskContribution contribution, IProgressMonitor monitor) {
		super.process(contribution, monitor);
//		if (this.isSubversiveReport(contribution)) {
//			this.appendToDescription(contribution);
//		}
	}

	@Override
	public void postProcess(ISupportResponse response, IProgressMonitor monitor) {
		super.postProcess(response, monitor);
	}

	protected boolean isSubversiveReport(ITaskContribution contribution) {
		return SVNTeamPlugin.NATURE_ID.equals(contribution.getStatus().getPlugin());
	}

	protected void appendToDescription(ITaskContribution contribution) {
		IStatus status = contribution.getStatus();
		String plugin = status.getPlugin();
		if (!SVNTeamPlugin.NATURE_ID.equals(plugin)) {
			return;
		}

		IReportingDescriptor[] providers = ExtensionsManager.getInstance().getReportingDescriptors();
		String report = "";
		for (IReportingDescriptor provider : providers) {
			report += ReportPartsFactory.getProductPart(provider);
			report += ReportPartsFactory.getVersionPart(provider);
		}
		report += ReportPartsFactory.getSVNClientPart();

		if (status.getSeverity() != IStatus.OK) {
			report += ReportPartsFactory.getJVMPropertiesPart();
		}

		report = ReportPartsFactory.removeHTMLTags(report);

		contribution.appendToDescription(report);
	}
}
