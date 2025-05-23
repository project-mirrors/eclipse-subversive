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
 *    Igor Burilo - Bug 245509: Improve extract log
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Extract To action for Synchronize View (both incoming and outgoing - the conflicting resources are ignored)
 * 
 * @author Alexei Goncharov
 */
public class ExtractToAction extends AbstractSynchronizeModelAction {

	protected ExtractToActionHelper actionHelper;

	public ExtractToAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		actionHelper = new ExtractToActionHelper(this, configuration);
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return actionHelper.getSyncInfoFilter();
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		AbstractSVNSyncInfo[] infos = getSVNSyncInfos();
		for (AbstractSVNSyncInfo element : infos) {
			if (SyncInfo.getDirection(element.getKind()) == SyncInfo.CONFLICTING) {
				return false;
			}
		}
		return infos.length > 0;
	}

	@Override
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return actionHelper.getOperation();
	}

}
