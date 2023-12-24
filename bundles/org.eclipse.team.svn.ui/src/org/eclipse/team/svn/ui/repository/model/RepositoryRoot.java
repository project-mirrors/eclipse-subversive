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
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Repository root node representation
 * 
 * @author Alexander Gurov
 */
public class RepositoryRoot extends RepositoryFolder {
	protected static String ROOT_NAME;

	public RepositoryRoot(RepositoryResource parent, IRepositoryResource resource) {
		super(parent, resource);
		RepositoryRoot.ROOT_NAME = SVNUIMessages.RepositoriesView_Model_Root;
		relatesToLocation = Boolean.FALSE;
	}

	@Override
	public String getLabel() {
		return label == null && ((IRepositoryRoot) resource).getKind() == IRepositoryRoot.KIND_ROOT
				? RepositoryRoot.ROOT_NAME
				: super.getLabel();
	}

	@Override
	protected ImageDescriptor getImageDescriptorImpl() {
		return isExternals()
				? super.getImageDescriptorImpl()
				: ((IRepositoryRoot) resource).getKind() == IRepositoryRoot.KIND_ROOT
						? SVNTeamUIPlugin.instance().getImageDescriptor("icons/objects/repository-root.gif") //$NON-NLS-1$
						: SVNTeamUIPlugin.instance().getImageDescriptor("icons/objects/root.gif"); //$NON-NLS-1$
	}

}
