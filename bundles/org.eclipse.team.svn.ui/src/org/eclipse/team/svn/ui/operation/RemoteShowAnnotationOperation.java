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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.remote.AbstractRepositoryOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.annotate.BuiltInAnnotate;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.common.ShowAnnotationPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IWorkbenchPage;

/**
 * The operation shows annotation for repository resource
 * 
 * @author Alexander Gurov
 */
public class RemoteShowAnnotationOperation extends AbstractRepositoryOperation {
	public RemoteShowAnnotationOperation(IRepositoryResource resource) {
		super("Operation_ShowAnnotationRemote", SVNUIMessages.class, new IRepositoryResource[] { resource }); //$NON-NLS-1$
	}

	public RemoteShowAnnotationOperation(IRepositoryResourceProvider provider) {
		super("Operation_ShowAnnotationRemote", SVNUIMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		UIMonitorUtility.getDisplay().syncExec(() -> {
			ShowAnnotationPanel panel = new ShowAnnotationPanel(
					RemoteShowAnnotationOperation.this.operableData()[0]);
			DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getShell(), panel);
			if (dialog.open() == 0) {
				IWorkbenchPage page = UIMonitorUtility.getActivePage();
				if (page != null) {
					new BuiltInAnnotate().open(page, RemoteShowAnnotationOperation.this.operableData()[0], null,
							panel.getRevisions());
				}
			}
		});
	}

}
