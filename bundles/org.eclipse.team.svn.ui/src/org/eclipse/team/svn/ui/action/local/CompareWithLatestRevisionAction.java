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

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.history.ISVNHistoryView;
import org.eclipse.team.svn.ui.operation.CompareResourcesOperation;
import org.eclipse.team.svn.ui.operation.CorrectRevisionOperation;
import org.eclipse.team.svn.ui.operation.ShowHistoryViewOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Compare menu "compare with latest revision" action implementation
 * 
 * @author Alexander Gurov
 */
public class CompareWithLatestRevisionAction extends AbstractWorkingCopyAction {

	public CompareWithLatestRevisionAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource resource = this.getSelectedResources()[0];

		ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(resource);
		IRepositoryResource remote = SVNUtility.getCopiedFrom(local);
		if (remote == null) {
			remote = SVNRemoteStorage.instance().asRepositoryResource(resource);
		}
		remote.setSelectedRevision(SVNRevision.HEAD);

		CompareResourcesOperation mainOp = new CompareResourcesOperation(local, remote);
		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
		op.add(new CorrectRevisionOperation(null, remote, local.getRevision(), resource));
		op.add(mainOp);
		if (SVNTeamPreferences.getHistoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.HISTORY_CONNECT_TO_COMPARE_WITH_NAME)) {
			op.add(new ShowHistoryViewOperation(resource, remote, ISVNHistoryView.COMPARE_MODE,
					ISVNHistoryView.COMPARE_MODE), new IActionOperation[] { mainOp });
		}
		runScheduled(op);
	}

	@Override
	public boolean isEnabled() {
		boolean isCompareFoldersAllowed = CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x;
		return this.getSelectedResources().length == 1
				&& (isCompareFoldersAllowed || this.getSelectedResources()[0].getType() == IResource.FILE)
				&& checkForResourcesPresence(CompareWithWorkingCopyAction.COMPARE_FILTER);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

}
