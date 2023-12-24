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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action.logicalmodel;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ExternalProgramParameters;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.DefaultExternalProgramParametersProvider;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.DetectExternalCompareOperationHelper;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.ExternalCompareOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.action.local.CompareWithWorkingCopyAction;
import org.eclipse.team.svn.ui.operation.CompareResourcesOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamDiffViewerPage;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Open in external compare editor logical model action implementation for Synchronize view
 * 
 * @author Igor Burilo
 */
public class OpenInExternalCompareEditorModelAction extends AbstractSynchronizeLogicalModelAction {

	public OpenInExternalCompareEditorModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected IActionOperation getOperation() {
		IActionOperation op = null;

		IResource resource = getSelectedResource();
		DiffViewerSettings diffSettings = SVNTeamDiffViewerPage.loadDiffViewerSettings();
		DetectExternalCompareOperationHelper detectCompareHelper = new DetectExternalCompareOperationHelper(resource,
				diffSettings, true);
		detectCompareHelper.execute(new NullProgressMonitor());
		ExternalProgramParameters externalProgramParams = detectCompareHelper.getExternalProgramParameters();

		ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
		IRepositoryResource remote = local.isCopied()
				? SVNUtility.getCopiedFrom(resource)
				: SVNRemoteStorage.instance().asRepositoryResource(resource);
		if (externalProgramParams != null) {
			op = new ExternalCompareOperation(local, remote,
					new DefaultExternalProgramParametersProvider(externalProgramParams));
		} else {
			op = new CompareResourcesOperation(local, remote, false, true);
		}
		return op;
	}

	@Override
	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		return selection.size() == 1 && super.isEnabledForSelection(selection);
	}

	@Override
	public FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter() {
			@Override
			public boolean select(SyncInfo info) {
				ILocalResource local = ((AbstractSVNSyncInfo) info).getLocalResource();
				return local.getResource().getType() == IResource.FILE
						&& CompareWithWorkingCopyAction.COMPARE_FILTER.accept(local);
			}
		};
	}
}
