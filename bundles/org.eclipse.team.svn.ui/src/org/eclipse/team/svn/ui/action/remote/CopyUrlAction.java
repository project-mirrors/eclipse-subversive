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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;

/**
 * Copy repository resource URL action
 *
 * @author Sergiy Logvin
 */
public class CopyUrlAction extends AbstractRepositoryTeamAction {

	protected String url;

	public CopyUrlAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IActionOperation op = new AbstractActionOperation("Operation_CopyURL", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				Clipboard clipboard = new Clipboard(CopyUrlAction.this.getShell().getDisplay());
				try {
					clipboard.setContents(
							new Object[] { url }, new Transfer[] { TextTransfer.getInstance() });
				} finally {
					clipboard.dispose();
				}
			}
		};
		runBusy(op);
	}

	@Override
	public boolean isEnabled() {
		IRepositoryResource[] resources = getSelectedRepositoryResources();
		IRepositoryLocation[] locations = getSelectedRepositoryLocations();
		if (resources.length == 0 && locations.length == 1) {
			url = locations[0].getUrl();
			return true;
		} else if (resources.length == 1) {
			url = resources[0].getUrl();
			return true;
		}

		return false;
	}

}
