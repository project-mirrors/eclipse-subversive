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

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.compare.PropertyCompareInput;
import org.eclipse.team.svn.ui.compare.ThreeWayPropertyCompareInput;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Compare properties action helper for Synchronize View
 * 
 * @author Igor Burilo
 */
public class ComparePropertiesActionHelper extends AbstractActionHelper {

	public ComparePropertiesActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	@Override
	public IActionOperation getOperation() {
		IResource resource = getSelectedResource();
		ILocalResource baseResource = SVNRemoteStorage.instance().asLocalResource(resource);
		IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(resource);
		SVNEntryRevisionReference baseReference = new SVNEntryRevisionReference(
				FileUtility.getWorkingCopyPath(resource), null, SVNRevision.BASE);
		SVNEntryRevisionReference remoteReference = baseReference;
		AbstractSVNSyncInfo info = getSelectedSVNSyncInfo();
		if (info != null) {
			ILocalResource change = getSelectedSVNSyncInfo().getRemoteChangeResource();
			if (change instanceof IResourceChange) {
				remote = ((IResourceChange) change).getOriginator();
				remoteReference = new SVNEntryRevisionReference(remote.getUrl(), remote.getPegRevision(),
						SVNRevision.fromNumber(((IResourceChange) change).getRevision()));
			}
			PropertyCompareInput input = new ThreeWayPropertyCompareInput(new CompareConfiguration(), resource,
					remoteReference, baseReference, remote.getRepositoryLocation(), baseResource.getRevision());
			CompareUI.openCompareEditor(input);
		}
		return null;
	}

}
