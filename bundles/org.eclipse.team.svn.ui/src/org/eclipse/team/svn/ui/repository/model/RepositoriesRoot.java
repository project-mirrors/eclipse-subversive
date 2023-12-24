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

import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * All repositories node representation
 * 
 * @author Alexander Gurov
 */
public class RepositoriesRoot extends RepositoryFictiveNode implements IParentTreeNode, IDataTreeNode {
	protected RepositoryLocation[] children;

	protected boolean softRefresh;

	@Override
	public Object getData() {
		return null;
	}

	@Override
	public void refresh() {
		children = null;
	}

	public void softRefresh() {
		softRefresh = true;
	}

	@Override
	public String getLabel(Object o) {
		return null;
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	public Object[] getChildren(Object o) {
		if (children == null || softRefresh) {
			HashMap<IRepositoryLocation, RepositoryLocation> oldLocations = new HashMap<>();
			if (children != null) {
				for (RepositoryLocation child : children) {
					oldLocations.put(child.getRepositoryLocation(), child);
				}
			}

			IRepositoryLocation[] locations = SVNRemoteStorage.instance().getRepositoryLocations();
			Arrays.sort(locations, (first, second) -> {
				IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
				if (SVNTeamPreferences.getBehaviourBoolean(store,
						SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_NAME)) {
					return first.getLabel().compareToIgnoreCase(second.getLabel());
				}
				return first.getLabel().compareTo(second.getLabel());
			});
			children = new RepositoryLocation[locations.length];
			for (int i = 0; i < locations.length; i++) {
				children[i] = oldLocations.get(locations[i]);
				if (children[i] == null) {
					children[i] = new RepositoryLocation(locations[i]);
				}
			}
		}
		return children;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

}
