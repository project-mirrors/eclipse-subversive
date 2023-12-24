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
 *    Alexey Mikoyan - Initial implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.repository.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Used to show current working directory
 *
 * @author Alexey Mikoyan
 *
 */
public class RepositoryFictiveWorkingDirectory extends RepositoryFictiveNode {
	public static final String WORKING_DIR_LABEL = ".."; //$NON-NLS-1$

	protected Object associatedDirectory;

	public RepositoryFictiveWorkingDirectory(Object associatedDirectory) {
		this.associatedDirectory = associatedDirectory;
	}

	public Object[] getChildren(Object o) {
		return null;
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}

	public String getLabel(Object o) {
		return RepositoryFictiveWorkingDirectory.WORKING_DIR_LABEL;
	}

	public Object getAssociatedDirectory() {
		return this.associatedDirectory;
	}

}
