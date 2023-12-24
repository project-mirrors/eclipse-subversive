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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.local;

import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RepositoryResourceSelectionComposite;
import org.eclipse.team.svn.ui.panel.common.AbstractRepositoryResourceSelectionPanel;

/**
 * Replace With URL panel
 * 
 * @author Alexei Goncharov
 */
public class ReplaceWithUrlPanel extends AbstractRepositoryResourceSelectionPanel {

	public ReplaceWithUrlPanel(IRepositoryResource baseResource, long currentRevision) {
		super(baseResource, currentRevision, SVNUIMessages.ReplaceUrlPanel_Title,
				SVNUIMessages.ReplaceUrlPanel_Selection_Description, "replaceUrl", //$NON-NLS-1$
				SVNUIMessages.ReplaceUrlPanel_Selection_Title, SVNUIMessages.ReplaceUrlPanel_Description,
				RepositoryResourceSelectionComposite.TEXT_BASE);
		this.defaultMessage = SVNUIMessages.ReplaceUrlPanel_Message;
	}

}
