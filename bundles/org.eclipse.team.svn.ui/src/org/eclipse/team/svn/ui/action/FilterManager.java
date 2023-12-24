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
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Manager for the resource state filters
 * 
 * @author Sergiy Logvin
 */
public class FilterManager implements IPropertyChangeListener, IResourceStatesListener {
	private static FilterManager instance = null;

	protected Map filters2condition;

	protected Map recursiveFilters2condition;

	protected MapChecker flatChecker;

	protected MapChecker recursiveChecker;

	protected boolean dirty;

	public static synchronized FilterManager instance() {
		if (FilterManager.instance == null) {
			FilterManager.instance = new FilterManager();
		}
		return FilterManager.instance;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty()
				.equals(SVNTeamPreferences.fullDecorationName(SVNTeamPreferences.DECORATION_PRECISE_ENABLEMENTS_NAME))
				|| event.getProperty()
						.equals(SVNTeamPreferences.fullCoreName(SVNTeamPreferences.CORE_SVNCONNECTOR_NAME))) {
			clear();
		}
	}

	@Override
	public void resourcesStateChanged(ResourceStatesChangedEvent event) {
		clear();
	}

	public void clear() {
		dirty = true;
	}

	public boolean checkForResourcesPresenceRecursive(IResource[] selectedResources, IStateFilter stateFilter) {
		return checkForResourcesPresence(selectedResources, stateFilter, true);
	}

	public boolean checkForResourcesPresence(IResource[] selectedResources, IStateFilter stateFilter,
			boolean recursive) {
		boolean computeDeep = SVNTeamPreferences.getDecorationBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.DECORATION_PRECISE_ENABLEMENTS_NAME);
		selectedResources = connectedToSVN(selectedResources);
		if (dirty) {
			dirty = false;
			if (filters2condition.size() > 0) {
				flatChecker.clearFilters();
				FileUtility.checkForResourcesPresence(selectedResources, flatChecker, IResource.DEPTH_ZERO);
				flatChecker.checkDisallowed();
			}
			if (computeDeep) {
				if (recursiveFilters2condition.size() > 0) {
					recursiveChecker.clearFilters();
					FileUtility.checkForResourcesPresence(selectedResources, recursiveChecker,
							IResource.DEPTH_INFINITE);
					recursiveChecker.checkDisallowed();
				}
			} else {
				recursiveChecker.setAllTo(Boolean.TRUE);
			}
		}
		Map filtersMap = recursive ? recursiveFilters2condition : filters2condition;
		Boolean retVal = (Boolean) filtersMap.get(stateFilter);
		if (retVal == null) {
			if (!computeDeep && recursive) {
				filtersMap.put(stateFilter, retVal = Boolean.TRUE);
			} else {
				boolean containsResources = FileUtility.checkForResourcesPresence(selectedResources, stateFilter,
						recursive ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO);
				filtersMap.put(stateFilter, retVal = containsResources);
			}
		}
		return retVal;
	}

	protected IResource[] connectedToSVN(IResource[] selectedResources) {
		ArrayList<IResource> retVal = new ArrayList<>(selectedResources.length);
		for (IResource element : selectedResources) {
			if (FileUtility.isConnected(element)) {
				retVal.add(element);
			}
		}
		return retVal.toArray(new IResource[retVal.size()]);
	}

	private FilterManager() {
		filters2condition = new HashMap();
		recursiveFilters2condition = new HashMap();

		flatChecker = new MapChecker(filters2condition);
		recursiveChecker = new MapChecker(recursiveFilters2condition);
		SVNTeamUIPlugin.instance().getPreferenceStore().addPropertyChangeListener(this);
		SVNRemoteStorage.instance().addResourceStatesListener(ResourceStatesChangedEvent.class, this);
	}

	protected static class MapChecker extends IStateFilter.AbstractStateFilter {
		protected Map filterMap;

		public MapChecker(Map filterMap) {
			this.filterMap = filterMap;
		}

		public void clearFilters() {
			for (Iterator it = filterMap.keySet().iterator(); it.hasNext();) {
				filterMap.put(it.next(), null);
			}
		}

		public void setAllTo(Boolean value) {
			for (Iterator it = filterMap.keySet().iterator(); it.hasNext();) {
				filterMap.put(it.next(), value);
			}
		}

		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			boolean retVal = true;
			for (Iterator it = filterMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				if (entry.getValue() == null) {
					IStateFilter filter = (IStateFilter) entry.getKey();
					boolean value = local == null ? filter.accept(resource, state, mask) : filter.accept(local);
					retVal &= value;
					if (value) {
						filterMap.put(filter, Boolean.TRUE);
					}
				}
			}
			return retVal;
		}

		public void checkDisallowed() {
			for (Iterator it = filterMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				if (entry.getValue() == null) {
					filterMap.put(entry.getKey(), Boolean.FALSE);
				}
			}
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}

	}

}
