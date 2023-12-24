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
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.operation.CompareResourcesOperation;

/**
 * Compare menu "compare with base revision" action implementation
 * 
 * @author Alexander Gurov
 */
public class CompareWithWorkingCopyAction extends AbstractWorkingCopyAction {

	public CompareWithWorkingCopyAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource resource = this.getSelectedResources()[0];
		ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(resource);
		IRepositoryResource remote = local.isCopied()
				? SVNUtility.getCopiedFrom(resource)
				: SVNRemoteStorage.instance().asRepositoryResource(resource);
		remote.setSelectedRevision(SVNRevision.BASE);
		runScheduled(new CompareResourcesOperation(local, remote));
	}

	@Override
	public boolean isEnabled() {
		return this.getSelectedResources().length == 1
				&& checkForResourcesPresence(CompareWithWorkingCopyAction.COMPARE_FILTER);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

	public static final IStateFilter COMPARE_FILTER = new IStateFilter.AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_EXCLUDE_DELETED.accept(resource, state, mask)
					| (mask & ILocalResource.IS_COPIED) != 0;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_EXCLUDE_DELETED.accept(resource, state, mask);
		}
	};

}
