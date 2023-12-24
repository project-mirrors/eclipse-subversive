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

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRevisionLink;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Repository revision node representation
 * 
 * @author Alexander Gurov
 */
public class RepositoryRevision extends RepositoryFictiveNode
		implements IParentTreeNode, IDataTreeNode, IToolTipProvider {
	protected RepositoryRevisions parent;

	protected RepositoryResource[] wrappers;

	protected IRevisionLink link;

	protected SVNRevision revision;

	public RepositoryRevision(RepositoryRevisions parent, IRevisionLink link) {
		this.parent = parent;
		this.link = link;
		revision = this.link.getRepositoryResource().getSelectedRevision();
		refresh();
	}

	@Override
	public RGB getForeground(Object element) {
		return RepositoryResource.NOT_RELATED_NODES_FOREGROUND;
	}

	@Override
	public RGB getBackground(Object element) {
		return RepositoryResource.NOT_RELATED_NODES_BACKGROUND;
	}

	@Override
	public FontData getFont(Object element) {
		return RepositoryResource.NOT_RELATED_NODES_FONT.getFontData()[0];
	}

	public SVNRevision getRevision() {
		return revision;
	}

	public IRevisionLink getRevisionLink() {
		return link;
	}

	@Override
	public void refresh() {
		wrappers = RepositoryFolder.wrapChildren(null, new IRepositoryResource[] { link.getRepositoryResource() },
				null);
	}

	@Override
	public Object getData() {
		return null;
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	public String getLabel() {
		/*
		 * Show resource url relative to repository root
		 * 
		 * Note how repository root is calculated:
		 * parent.getRepositoryLocation().getRepositoryRootUrl()
		 * this is done instead of
		 * this.link.getRepositoryResource().getRepositoryLocation().getRepositoryRootUrl()
		 * in order externals to different repositories were not shown
		 * as relative to repository root, so they're shown with full url
		 */
		IRepositoryResource resource = link.getRepositoryResource();
		IPath rootPath = SVNUtility.createPathForSVNUrl(parent.getRepositoryLocation().getRepositoryRootUrl());
		IPath resourcePath = SVNUtility.createPathForSVNUrl(resource.getUrl());
		if (rootPath.isPrefixOf(resourcePath)) {
			IPath relativePath = resourcePath.makeRelativeTo(rootPath);
			return "^" + (relativePath.isEmpty() ? "" : "/" + relativePath.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else {
			return resourcePath.toString();
		}
	}

	@Override
	public String getLabel(Object o) {
		return this.getLabel() + " " + revision.toString(); //$NON-NLS-1$
	}

	@Override
	public Object[] getChildren(Object o) {
		return wrappers;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object o) {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof RepositoryRevision) {
			RepositoryRevision other = (RepositoryRevision) obj;
			return revision.equals(other.revision);
		}
		return super.equals(obj);
	}

	@Override
	public String getToolTipMessage(String formatString) {
		return link.getComment();
	}

}
