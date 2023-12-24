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

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRevisionLink;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Repository revisions node representation
 * 
 * @author Alexander Gurov
 */
public class RepositoryRevisions extends RepositoryFictiveNode implements IParentTreeNode, IDataTreeNode {
	protected static String REVISIONS_NAME;

	protected IRepositoryLocation location;

	protected RepositoryRevision[] children;

	public RepositoryRevisions(IRepositoryLocation location) {
		this.location = location;
		RepositoryRevisions.REVISIONS_NAME = SVNUIMessages.RepositoriesView_Model_Revisions;
	}

	public IRepositoryLocation getRepositoryLocation() {
		return location;
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

	@Override
	public void refresh() {
		children = null;
	}

	@Override
	public Object getData() {
		return null;
	}

	@Override
	public boolean hasChildren() {
		return location.getRevisionLinks().length > 0;
	}

	@Override
	public String getLabel(Object o) {
		return RepositoryRevisions.REVISIONS_NAME;
	}

	public IRevisionLink[] getLinks(SVNRevision revision) {
		IRevisionLink[] links = location.getRevisionLinks();
		ArrayList<IRevisionLink> retVal = new ArrayList<>();
		for (IRevisionLink link : links) {
			if (link.getRepositoryResource().getSelectedRevision().equals(revision)) {
				retVal.add(link);
			}
		}
		return retVal.toArray(new IRevisionLink[retVal.size()]);
	}

	@Override
	public Object[] getChildren(Object o) {
		if (children == null) {
			IRevisionLink[] links = location.getRevisionLinks();
			children = new RepositoryRevision[links.length];
			for (int i = 0; i < links.length; i++) {
				children[i] = new RepositoryRevision(this, links[i]);
			}
			Arrays.sort(children, (o1, o2) -> {
				IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
				if (SVNTeamPreferences.getBehaviourBoolean(store,
						SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_NAME)) {
					return o1.getLabel().compareToIgnoreCase(o2.getLabel());
				}
				return o1.getLabel().compareTo(o2.getLabel());
			});
		}
		return children;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object o) {
		return SVNTeamUIPlugin.instance().getImageDescriptor("icons/objects/revisions.gif"); //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof RepositoryRevisions) {
			return ((RepositoryRevisions) obj).location.equals(location);
		}
		return super.equals(obj);
	}

	protected class RevisionWrapper {
		public SVNRevision revision;

		public RevisionWrapper(SVNRevision revision) {
			this.revision = revision;
		}

		@Override
		public int hashCode() {
			return revision.getKind().id;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof RevisionWrapper) {
				return revision.equals(((RevisionWrapper) obj).revision);
			}
			return false;
		}
	}
}
