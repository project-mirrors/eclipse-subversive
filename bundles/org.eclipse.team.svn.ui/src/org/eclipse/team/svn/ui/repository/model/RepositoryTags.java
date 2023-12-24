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
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;

/**
 * TAG's node representation
 * 
 * @author Alexander Gurov
 */
public class RepositoryTags extends RepositoryFolder {
	public RepositoryTags(RepositoryResource parent, IRepositoryResource resource) {
		super(parent, resource);
	}

	protected ImageDescriptor getImageDescriptorImpl() {
		return this.isExternals()
				? super.getImageDescriptorImpl()
				: SVNTeamUIPlugin.instance().getImageDescriptor("icons/objects/tags.gif"); //$NON-NLS-1$
	}

	public RGB getForeground(Object element) {
		return RepositoryResource.STRUCTURE_DEFINED_NODES_FOREGROUND;
	}

	public RGB getBackground(Object element) {
		return RepositoryResource.STRUCTURE_DEFINED_NODES_BACKGROUND;
	}

	public FontData getFont(Object element) {
		return RepositoryResource.STRUCTURE_DEFINED_NODES_FONT.getFontData()[0];
	}

}
