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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.action.IResourceSelector;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * As there are several hierarchies of action, we need to extract common code for them, e.g. we have commit action in
 * AbstractSynchronizeModelAction and AbstractSynchronizeLogicalModelAction hierarchies.
 * 
 * Helpers contain common action logic which can be reused by actions.
 * 
 * Note that actions not necessary should use action helpers
 * 
 * @author Igor Burilo
 */
public abstract class AbstractActionHelper {

	protected IAction action;

	protected ISynchronizePageConfiguration configuration;

	public AbstractActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		this.action = action;
		this.configuration = configuration;
	}

	public FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter();
	}

	public IResourceSelector getSyncInfoSelector() {
		IResourceSelector selector = null;
		if (action instanceof AbstractSynchronizeLogicalModelAction) {
			selector = ((AbstractSynchronizeLogicalModelAction) action).getSyncInfoSelector();
		} else if (action instanceof AbstractSynchronizeModelAction) {
			selector = ((AbstractSynchronizeModelAction) action).getSyncInfoSelector();
		}
		return selector;
	}

//	public IResourceSelector getTreeNodeSelector() {
//		IResourceSelector selector = null;
//		if (this.action instanceof AbstractSynchronizeLogicalModelAction) {
//			selector =  ((AbstractSynchronizeLogicalModelAction) this.action).getSyncInfoSelector();
//		} else if (this.action instanceof AbstractSynchronizeModelAction) {
//			selector =  ((AbstractSynchronizeModelAction) this.action).getTreeNodeSelector();
//		}
//		return selector;
//	}

	public AbstractSVNSyncInfo[] getSVNSyncInfos() {
		AbstractSVNSyncInfo[] syncInfos = null;
		if (action instanceof AbstractSynchronizeLogicalModelAction) {
			syncInfos = ((AbstractSynchronizeLogicalModelAction) action).getSVNSyncInfos();
		} else if (action instanceof AbstractSynchronizeModelAction) {
			syncInfos = ((AbstractSynchronizeModelAction) action).getSVNSyncInfos();
		}
		return syncInfos == null ? new AbstractSVNSyncInfo[0] : syncInfos;
	}

	public IResource getSelectedResource() {
		IResource resource = null;
		if (action instanceof AbstractSynchronizeLogicalModelAction) {
			resource = ((AbstractSynchronizeLogicalModelAction) action).getSelectedResource();
		} else if (action instanceof AbstractSynchronizeModelAction) {
			resource = ((AbstractSynchronizeModelAction) action).getSelectedResource();
		}
		return resource;
	}

	public IResource[] getAllSelectedResources() {
		IResource[] resources = null;
		if (action instanceof AbstractSynchronizeLogicalModelAction) {
			resources = ((AbstractSynchronizeLogicalModelAction) action).getAllSelectedResources();
		} else if (action instanceof AbstractSynchronizeModelAction) {
			resources = ((AbstractSynchronizeModelAction) action).getAllSelectedResources();
		}
		return resources;
	}

	public AbstractSVNSyncInfo getSelectedSVNSyncInfo() {
		AbstractSVNSyncInfo syncInfo = null;
		if (action instanceof AbstractSynchronizeLogicalModelAction) {
			syncInfo = ((AbstractSynchronizeLogicalModelAction) action).getSelectedSVNSyncInfo();
		} else if (action instanceof AbstractSynchronizeModelAction) {
			syncInfo = ((AbstractSynchronizeModelAction) action).getSelectedSVNSyncInfo();
		}
		return syncInfo;
	}

	public abstract IActionOperation getOperation();

}
