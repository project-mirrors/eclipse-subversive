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

import org.eclipse.core.runtime.IStatus;

/**
 * Stack trace-based status visitor
 * 
 * @author Alexander Gurov
 */
public class StackTraceVisitor implements ReportPartsFactory.IStatusVisitor {
	@Override
	public boolean visit(IStatus status) {
		String output = ReportPartsFactory.getOutput(status);
		return output.indexOf(".eclipse.team.svn.") != -1 && //$NON-NLS-1$
				output.indexOf("Could not instantiate provider") == -1 && //$NON-NLS-1$
				output.indexOf("Java Model Exception: Java Model Status [") == -1 && //$NON-NLS-1$
				output.indexOf("org.eclipse.team.svn.ui.operation.OpenRemoteFileOperation.openEditor") == -1; //$NON-NLS-1$
	}
}
