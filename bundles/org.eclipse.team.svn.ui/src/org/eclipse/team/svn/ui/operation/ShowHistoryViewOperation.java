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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.history.ISVNHistoryView;
import org.eclipse.team.svn.ui.history.SVNHistoryPage;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

/**
 * Show history view operation
 * 
 * @author Alexander Gurov
 */
public class ShowHistoryViewOperation extends AbstractActionOperation {
	protected IRepositoryResource remote;

	protected IRepositoryResourceProvider provider;

	protected IResource compareWith;

	protected IResource local;

	protected int options;

	protected int mask;

	public ShowHistoryViewOperation(IResource local, int mask, int options) {
		super("Operation_ShowHistory", SVNUIMessages.class); //$NON-NLS-1$
		this.mask = mask;
		this.options = options;
		this.local = local;
	}

	public ShowHistoryViewOperation(IRepositoryResource remote, int mask, int options) {
		super("Operation_ShowHistory", SVNUIMessages.class); //$NON-NLS-1$
		this.mask = mask;
		this.options = options;
		this.remote = remote;
	}

	public ShowHistoryViewOperation(IRepositoryResourceProvider provider, int mask, int options) {
		super("Operation_ShowHistory", SVNUIMessages.class); //$NON-NLS-1$
		this.mask = mask;
		this.options = options;
		this.provider = provider;
	}

	public ShowHistoryViewOperation(IResource compareWith, IRepositoryResource remote, int mask, int options) {
		super("Operation_ShowHistory", SVNUIMessages.class); //$NON-NLS-1$
		this.mask = mask | ISVNHistoryView.COMPARE_MODE;
		this.options = options | ISVNHistoryView.COMPARE_MODE;
		this.remote = remote;
		this.compareWith = compareWith;
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (provider != null) {
			remote = provider.getRepositoryResources()[0];
		}
		UIMonitorUtility.getDisplay().syncExec(() -> {
			IWorkbenchPage page = UIMonitorUtility.getActivePage();

			if (page != null) {
				try {
					IHistoryView historyView = (IHistoryView) page.showView(IHistoryView.VIEW_ID);
					if (historyView != null) {
						IHistoryPage tPage = historyView.showHistoryFor(local != null ? (Object) local : remote);
						if (tPage != null && tPage instanceof SVNHistoryPage) {
							SVNHistoryPage hPage = (SVNHistoryPage) tPage;
							hPage.setOptions(mask, options);
							hPage.setCompareWith(compareWith);
							if (local != null && !local.equals(hPage.getResource())) {
								// view is disconnected
								hPage.showHistory(local);
							}
						}
					}
				} catch (PartInitException ex) {
					ShowHistoryViewOperation.this.reportStatus(IStatus.ERROR, null, ex);
				}
			}
		});
	}

}
