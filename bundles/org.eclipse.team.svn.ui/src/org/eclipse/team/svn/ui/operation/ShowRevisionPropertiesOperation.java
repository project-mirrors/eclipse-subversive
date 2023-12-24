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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.properties.RevPropertiesView;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Show revision properties operation
 * 
 * @author Alexei Goncharov
 */
public class ShowRevisionPropertiesOperation extends AbstractActionOperation {

	protected IRepositoryLocation location;

	protected SVNRevision revision;

	protected IWorkbenchPage page;

	public ShowRevisionPropertiesOperation(IWorkbenchPage page, IRepositoryLocation location, SVNRevision revision) {
		super("Operation_ShowRevProperties", SVNUIMessages.class); //$NON-NLS-1$
		this.page = page;
		this.location = location;
		this.revision = revision;
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		UIMonitorUtility.getDisplay().syncExec(() -> ShowRevisionPropertiesOperation.this.protectStep(monitor1 -> {
			RevPropertiesView view = (RevPropertiesView) page.showView(RevPropertiesView.VIEW_ID);
			view.setLocationAndRevision(location, revision);
		}, monitor, 1));
	}

}
