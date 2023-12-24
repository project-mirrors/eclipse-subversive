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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.reporting;

import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Preview any product quality report panel
 * 
 * @author Alexander Gurov
 */
public class PreviewReportPanel extends PreviewPanel {
	public PreviewReportPanel(String description, String report) {
		super(SVNUIMessages.PreviewReportPanel_Title, description, SVNUIMessages.PreviewReportPanel_Message, report);
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.previewReportDialogContext"; //$NON-NLS-1$
	}
}
