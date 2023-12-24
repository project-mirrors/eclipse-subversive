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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Unaccessible node representation
 * 
 * @author Alexander Gurov
 */
public class RepositoryError extends RepositoryFictiveNode {
	public static final String ERROR_MSG = "RepositoriesView_Model_Error"; //$NON-NLS-1$

	protected IStatus errorStatus;

	public RepositoryError(IStatus errorStatus) {
		this.errorStatus = errorStatus;
	}

	public IStatus getErrorStatus() {
		return errorStatus;
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
		return SVNUIMessages.getString(RepositoryError.ERROR_MSG);
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object o) {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK);
	}

}
