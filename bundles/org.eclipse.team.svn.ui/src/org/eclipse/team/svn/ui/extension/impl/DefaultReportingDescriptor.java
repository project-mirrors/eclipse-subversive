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

package org.eclipse.team.svn.ui.extension.impl;

import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.extension.factory.IReportingDescriptor;

/**
 * Default IReportingDescriptor implementation
 *
 * @author Sergiy Logvin
 */

public class DefaultReportingDescriptor implements IReportingDescriptor {
	@Override
	public String getEmailTo() {
		return "subversive-bugs@polarion.org"; //$NON-NLS-1$
	}

	@Override
	public String getEmailFrom() {
		return "subversive-bugs@polarion.org"; //$NON-NLS-1$
	}

	@Override
	public String getHost() {
		return "mail.polarion.cz"; //$NON-NLS-1$
	}

	@Override
	public String getPort() {
		return "25"; //$NON-NLS-1$
	}

	@Override
	public String getProductName() {
		return "Subversive"; //$NON-NLS-1$
	}

	@Override
	public String getProductVersion() {
		return SVNTeamUIPlugin.instance().getVersionString();
	}

	@Override
	public String getTrackerUrl() {
		return "https://bugs.eclipse.org/bugs"; //$NON-NLS-1$
	}

	@Override
	public boolean isTrackerSupportsHTML() {
		return false;
	}

}
