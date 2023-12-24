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

package org.eclipse.team.svn.ui.debugmail;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;

/**
 * Problem listener implementation
 * 
 * @author Alexander Gurov
 */
public class ProblemListener implements ILogListener {
	protected static PluginIDVisitor idVisitor = new PluginIDVisitor();

	protected static StackTraceVisitor stackVisitor = new StackTraceVisitor();

	public ProblemListener() {
		super();
	}

	public void logging(IStatus status, String plugin) {
		// our problems should be handled in the UILoggedOperation in order to suppress two sequential dialogs 
		ReportPartsFactory.IStatusVisitor visitor = ReportPartsFactory.checkStatus(status, ProblemListener.idVisitor)
				? (ReportPartsFactory.IStatusVisitor) null
				: ProblemListener.stackVisitor;
		if (visitor != null && ReportPartsFactory.checkStatus(status, visitor)) {
			this.sendReport(status, plugin);
		}
	}

	protected void sendReport(IStatus status, String plugin) {
		UILoggedOperation.showError(plugin, "", status); //$NON-NLS-1$
	}

}
