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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.operation.local.AbstractMergeSet;
import org.eclipse.team.svn.core.synchronize.MergeScopeHelper;
import org.eclipse.team.ui.synchronize.AbstractSynchronizeScope;

/**
 * Merge resources scope. Non-persistent.
 * 
 * @author Alexander Gurov
 */
public class MergeScope extends AbstractSynchronizeScope {

	protected MergeScopeHelper scopeHelper;

	public MergeScope(AbstractMergeSet info) {
		scopeHelper = new MergeScopeHelper(info);
	}

	public MergeScope() {
		scopeHelper = new MergeScopeHelper();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeScope#getName()
	 */
	@Override
	public String getName() {
		return scopeHelper.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeScope#getRoots()
	 */
	@Override
	public IResource[] getRoots() {
		return scopeHelper.getRoots();
	}

	public void setMergeSet(AbstractMergeSet info) {
		scopeHelper.setMergeSet(info);
		fireRootsChanges();
	}

	public MergeScopeHelper getMergeScopeHelper() {
		return scopeHelper;
	}
}
