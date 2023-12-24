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

package org.eclipse.team.svn.ui.panel.reporting;

import org.eclipse.swt.graphics.Point;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Preview error report panel
 *
 * @author Sergiy Logvin
 */
public class PreviewErrorReportPanel extends PreviewReportPanel {
	public PreviewErrorReportPanel(String report) {
		super(SVNUIMessages.PreviewErrorReportPanel_Description, report);
	}
	
	public Point getPrefferedSizeImpl() {
		return new Point(750, 700);
	}
	
}
