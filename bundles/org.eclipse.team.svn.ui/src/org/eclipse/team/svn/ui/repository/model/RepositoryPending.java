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

package org.eclipse.team.svn.ui.repository.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Referesh in progress node implementation
 * 
 * @author Alexander Gurov
 */
public class RepositoryPending extends RepositoryFictiveNode {
	public static final String PENDING = "RepositoriesView_Model_Pending"; //$NON-NLS-1$

	protected RepositoryResource parent;

	public RepositoryPending(RepositoryResource parent) {
		this.parent = parent;
	}

	@Override
	public RGB getForeground(Object element) {
		return parent.getForeground(element);
	}

	public boolean hasChildren() {
		return false;
	}

	@Override
	public Object[] getChildren(Object o) {
		return null;
	}

	@Override
	public String getLabel(Object o) {
		return SVNUIMessages.getString(RepositoryPending.PENDING);
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/repositories/browser_pending.gif"); //$NON-NLS-1$
	}

}
