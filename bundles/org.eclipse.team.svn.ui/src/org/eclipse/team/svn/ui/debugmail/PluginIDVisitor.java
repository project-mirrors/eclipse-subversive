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
import org.eclipse.team.svn.core.SVNTeamPlugin;

/**
 * Plugin ID-based status visitor
 * 
 * @author Alexander Gurov
 */
public class PluginIDVisitor implements ReportPartsFactory.IStatusVisitor {
	@Override
	public boolean visit(IStatus status) {
		return status.getPlugin().equals(SVNTeamPlugin.NATURE_ID);
	}
}
