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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Path;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNLogPath;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.history.ISVNHistoryViewInfo;
import org.eclipse.team.svn.ui.history.model.ILogNode;

/**
 * Root history category. Contains all history entries.
 * 
 * @author Alexander Gurov
 */
public class RootHistoryCategory extends HistoryCategory {
	public static String[] NO_REMOTE;

	public static String[] NO_LOCAL;

	public static String[] NO_REVS;

	public static String[] PENDING;

	protected Object[] allHistory;

	protected SVNLocalFileRevision[] localHistory;

	protected SVNLogEntry[] remoteHistory;

	protected HistoryCategory[] categoriesBoth;

	protected HistoryCategory[] categoriesRemote;

	protected HistoryCategory[] categoriesLocal;

	protected Map<Object, SVNChangedPathData[]> pathData;

	protected Set<String> relatedPathsPrefixes;

	protected Set<String> relatedParents;

	protected ISVNHistoryViewInfo info;

	public RootHistoryCategory(ISVNHistoryViewInfo info) {
		super(HistoryCategory.CATEGORY_ROOT, null);
		if (RootHistoryCategory.NO_REMOTE == null) {
			RootHistoryCategory.NO_REMOTE = new String[] { SVNUIMessages.LogMessagesComposite_NoRemote };
			RootHistoryCategory.NO_LOCAL = new String[] { SVNUIMessages.LogMessagesComposite_NoLocal };
			RootHistoryCategory.NO_REVS = new String[] { SVNUIMessages.LogMessagesComposite_NoRevs };
			RootHistoryCategory.PENDING = new String[] { SVNUIMessages.RepositoriesView_Model_Pending };
		}
		this.info = info;
		pathData = new HashMap<>();
	}

	public SVNLogEntry[] getRemoteHistory() {
		return remoteHistory;
	}

	public SVNLocalFileRevision[] getLocalHistory() {
		return localHistory;
	}

	@Override
	public Object[] getEntries() {
		switch (info.getMode()) {
			case ISVNHistoryViewInfo.MODE_LOCAL: {
				return getLocalHistoryInternal();
			}
			case ISVNHistoryViewInfo.MODE_REMOTE: {
				return getRemoteHistoryInternal();
			}
		}
		return getAllHistoryInternal();
	}

	public Collection<String> getRelatedPathPrefixes() {
		return relatedPathsPrefixes;
	}

	public Collection<String> getRelatedParents() {
		return relatedParents;
	}

	public SVNChangedPathData[] getPathData(ILogNode key) {
		return pathData.get(key == null ? null : key.getEntity());
	}

	public void refreshModel() {
		synchronized (info) {
			localHistory = info.getLocalHistory();
			remoteHistory = info.getRemoteHistory();
			if (localHistory == null) {
				allHistory = remoteHistory;
			} else if (remoteHistory == null) {
				allHistory = localHistory;
			} else {
				allHistory = new Object[localHistory.length + remoteHistory.length];
				System.arraycopy(localHistory, 0, allHistory, 0, localHistory.length);
				System.arraycopy(remoteHistory, 0, allHistory, localHistory.length, remoteHistory.length);
			}
			collectRelatedNodes();
			collectCategoriesAndMapData();
		}
	}

	protected void collectRelatedNodes() {
		relatedPathsPrefixes = null;
		relatedParents = null;

		if (remoteHistory != null) {
			SVNLogPath[] changes = null;
			// msg.changedPaths can be null or empty if user has no rights. So, find first accessible entry.
			for (SVNLogEntry msg : remoteHistory) {
				if (msg.changedPaths != null && msg.changedPaths.length > 0) {
					changes = msg.changedPaths;
					break;
				}
			}

			if (changes != null) {
				String baseUrl = info.getRepositoryResource().getUrl();
				String changePath = changes[0].path;
				int idx = -1;
				// find root trim point for the URL specified
				while (changePath.length() > 0 && (idx = baseUrl.indexOf(changePath)) == -1) {
					changePath = new Path(changePath).removeLastSegments(1).toString();
				}
				if (idx != -1 && idx < baseUrl.length()) {
					// cut root URL from related path
					String relatedPathsPrefix = baseUrl.substring(idx + 1);

					relatedPathsPrefixes = new HashSet<>();
					relatedParents = new HashSet<>();

					// collect copiedFrom entries
					for (SVNLogEntry msg : remoteHistory) {
						relatedPathsPrefixes.add(relatedPathsPrefix);
						if (msg.changedPaths != null && msg.changedPaths.length > 0) {
							relatedPathsPrefix = getNextPrefix(msg, relatedPathsPrefix);
						}
					}
				}
			}
		}
	}

	protected String getNextPrefix(SVNLogEntry message, String current) {
		String checked = "/" + current; //$NON-NLS-1$
		SVNLogPath[] changes = message.changedPaths;

		for (SVNLogPath change : changes) {
			if (change.copiedFromPath != null && checked.startsWith(change.path)) {
				String rest = checked.substring(change.path.length());
				String relatedParent = change.copiedFromPath.substring(1);
				relatedParents.add(relatedParent);
				relatedParents.add(change.path.substring(1));
				return relatedParent + rest;
			}
		}

		return current;
	}

	protected void collectCategoriesAndMapData() {
		pathData.clear();

		ArrayList<HistoryCategory> categoriesAll = new ArrayList<>();
		ArrayList<HistoryCategory> categoriesLocal = new ArrayList<>();
		ArrayList<HistoryCategory> categoriesRemote = new ArrayList<>();

		ArrayList<Object> todayEntriesAll = new ArrayList<>();
		ArrayList<Object> yesterdayEntriesAll = new ArrayList<>();
		ArrayList<Object> weekEntriesAll = new ArrayList<>();
		ArrayList<Object> monthEntriesAll = new ArrayList<>();
		ArrayList<Object> earlierEntriesAll = new ArrayList<>();

		ArrayList<SVNLocalFileRevision> todayEntriesLocal = new ArrayList<>();
		ArrayList<SVNLocalFileRevision> yesterdayEntriesLocal = new ArrayList<>();
		ArrayList<SVNLocalFileRevision> weekEntriesLocal = new ArrayList<>();
		ArrayList<SVNLocalFileRevision> monthEntriesLocal = new ArrayList<>();
		ArrayList<SVNLocalFileRevision> earlierEntriesLocal = new ArrayList<>();

		ArrayList<SVNLogEntry> todayEntriesRemote = new ArrayList<>();
		ArrayList<SVNLogEntry> yesterdayEntriesRemote = new ArrayList<>();
		ArrayList<SVNLogEntry> weekEntriesRemote = new ArrayList<>();
		ArrayList<SVNLogEntry> monthEntriesRemote = new ArrayList<>();
		ArrayList<SVNLogEntry> earlierEntriesRemote = new ArrayList<>();

		Calendar yesterdayCal = Calendar.getInstance();
		yesterdayCal.set(Calendar.HOUR_OF_DAY, 0);
		yesterdayCal.set(Calendar.MINUTE, 0);
		yesterdayCal.set(Calendar.SECOND, 0);
		long yesterdayDate = yesterdayCal.getTimeInMillis();
		Calendar beforeYesterdayCal = Calendar.getInstance();
		beforeYesterdayCal.roll(Calendar.DAY_OF_YEAR, -1);
		beforeYesterdayCal.set(Calendar.HOUR_OF_DAY, 0);
		beforeYesterdayCal.set(Calendar.MINUTE, 0);
		beforeYesterdayCal.set(Calendar.SECOND, 0);
		long beforeYesterdayDate = beforeYesterdayCal.getTimeInMillis();
		Calendar weekCal = Calendar.getInstance();
		weekCal.roll(Calendar.DAY_OF_YEAR, -7);
		weekCal.set(Calendar.HOUR_OF_DAY, 0);
		weekCal.set(Calendar.MINUTE, 0);
		weekCal.set(Calendar.SECOND, 0);
		long lastWeekDate = weekCal.getTimeInMillis();
		Calendar monthCal = Calendar.getInstance();
		monthCal.set(Calendar.DAY_OF_MONTH, 1);
		monthCal.set(Calendar.HOUR_OF_DAY, 0);
		monthCal.set(Calendar.MINUTE, 0);
		monthCal.set(Calendar.SECOND, 0);
		long lastMonthDate = monthCal.getTimeInMillis();

		//Filling timing ArrayLists
		if (remoteHistory != null) {
			for (SVNLogEntry element : remoteHistory) {
				mapPathData(element);
				if (element.date >= yesterdayDate) {
					todayEntriesAll.add(element);
					todayEntriesRemote.add(element);
				} else if (element.date < yesterdayDate && element.date >= beforeYesterdayDate) {
					yesterdayEntriesAll.add(element);
					yesterdayEntriesRemote.add(element);
				} else if (element.date < beforeYesterdayDate && element.date >= lastWeekDate) {
					weekEntriesAll.add(element);
					weekEntriesRemote.add(element);
				} else if (element.date < lastWeekDate && element.date >= lastMonthDate) {
					monthEntriesAll.add(element);
					monthEntriesRemote.add(element);
				} else {
					earlierEntriesAll.add(element);
					earlierEntriesRemote.add(element);
				}
			}
		}
		if (localHistory != null) {
			for (SVNLocalFileRevision element : localHistory) {
				if (element.getTimestamp() >= yesterdayDate) {
					todayEntriesAll.add(element);
					todayEntriesLocal.add(element);
				} else if (element.getTimestamp() < yesterdayDate && element.getTimestamp() >= beforeYesterdayDate) {
					yesterdayEntriesAll.add(element);
					yesterdayEntriesLocal.add(element);
				} else if (element.getTimestamp() < beforeYesterdayDate && element.getTimestamp() >= lastWeekDate) {
					weekEntriesAll.add(element);
					weekEntriesLocal.add(element);
				} else if (element.getTimestamp() < lastWeekDate && element.getTimestamp() >= lastMonthDate) {
					monthEntriesAll.add(element);
					monthEntriesLocal.add(element);
				} else {
					earlierEntriesAll.add(element);
					earlierEntriesLocal.add(element);
				}
			}
		}
		HistoryCategory cat = null;

		//Fill both local and remote
		if (todayEntriesAll.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_TODAY, todayEntriesAll.toArray());
			categoriesAll.add(cat);
		}
		if (yesterdayEntriesAll.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_YESTERDAY, yesterdayEntriesAll.toArray());
			categoriesAll.add(cat);
		}
		if (weekEntriesAll.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_THIS_WEEK, weekEntriesAll.toArray());
			categoriesAll.add(cat);
		}
		if (monthEntriesAll.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_THIS_MONTH, monthEntriesAll.toArray());
			categoriesAll.add(cat);
		}
		if (earlierEntriesAll.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_EARLIER, earlierEntriesAll.toArray());
			categoriesAll.add(cat);
		}

		//Fill local
		if (todayEntriesLocal.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_TODAY, todayEntriesLocal.toArray());
			categoriesLocal.add(cat);
		}
		if (yesterdayEntriesLocal.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_YESTERDAY, yesterdayEntriesLocal.toArray());
			categoriesLocal.add(cat);
		}
		if (weekEntriesLocal.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_THIS_WEEK, weekEntriesLocal.toArray());
			categoriesLocal.add(cat);
		}
		if (monthEntriesLocal.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_THIS_MONTH, monthEntriesLocal.toArray());
			categoriesLocal.add(cat);
		}
		if (earlierEntriesLocal.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_EARLIER, earlierEntriesLocal.toArray());
			categoriesLocal.add(cat);
		}

		//Fill remote
		if (todayEntriesRemote.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_TODAY, todayEntriesRemote.toArray());
			categoriesRemote.add(cat);
		}
		if (yesterdayEntriesRemote.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_YESTERDAY, yesterdayEntriesRemote.toArray());
			categoriesRemote.add(cat);
		}
		if (weekEntriesRemote.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_THIS_WEEK, weekEntriesRemote.toArray());
			categoriesRemote.add(cat);
		}
		if (monthEntriesRemote.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_THIS_MONTH, monthEntriesRemote.toArray());
			categoriesRemote.add(cat);
		}
		if (earlierEntriesRemote.size() > 0) {
			cat = new HistoryCategory(HistoryCategory.CATEGORY_EARLIER, earlierEntriesRemote.toArray());
			categoriesRemote.add(cat);
		}

		categoriesBoth = categoriesAll.size() > 0
				? categoriesAll.toArray(new HistoryCategory[categoriesAll.size()])
				: null;
		this.categoriesLocal = categoriesLocal.size() > 0
				? categoriesLocal.toArray(new HistoryCategory[categoriesLocal.size()])
				: null;
		this.categoriesRemote = categoriesRemote.size() > 0
				? categoriesRemote.toArray(new HistoryCategory[categoriesRemote.size()])
				: null;
	}

	protected void mapPathData(SVNLogEntry key) {
		SVNChangedPathData[] pathData = new SVNChangedPathData[key.changedPaths == null ? 0 : key.changedPaths.length];
		for (int i = 0; i < pathData.length; i++) {
			String path = key.changedPaths[i].path;
			path = path.startsWith("/") ? path.substring(1) : path; //$NON-NLS-1$
			int idx = path.lastIndexOf("/"); //$NON-NLS-1$
			pathData[i] = new SVNChangedPathData(
					key.changedPaths[i].action, idx != -1 ? path.substring(idx + 1) : path,
					idx != -1 ? path.substring(0, idx) : "", //$NON-NLS-1$
					key.changedPaths[i].copiedFromRevision != SVNRevision.INVALID_REVISION_NUMBER
							? key.changedPaths[i].copiedFromPath
							: "", //$NON-NLS-1$
					key.changedPaths[i].copiedFromRevision
			);
		}
		this.pathData.put(key, pathData);
		SVNLogEntry[] children = key.getChildren();
		if (children != null) {
			for (SVNLogEntry child : children) {
				mapPathData(child);
			}
		}
	}

	protected Object[] getLocalHistoryInternal() {
		if (localHistory == null) {
			return RootHistoryCategory.NO_LOCAL;
		}
		return info.isGrouped() ? categoriesLocal : localHistory;
	}

	protected Object[] getRemoteHistoryInternal() {
		if (remoteHistory == null) {
			return info.isPending() ? RootHistoryCategory.PENDING : RootHistoryCategory.NO_REMOTE;
		}
		return info.isGrouped() ? categoriesRemote : remoteHistory;
	}

	protected Object[] getAllHistoryInternal() {
		if (allHistory == null) {
			return info.isPending() ? RootHistoryCategory.PENDING : RootHistoryCategory.NO_REVS;
		}
		return info.isGrouped() && categoriesBoth != null ? categoriesBoth : allHistory;
	}

}
