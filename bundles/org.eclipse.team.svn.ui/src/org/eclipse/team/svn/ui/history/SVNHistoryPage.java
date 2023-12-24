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
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *    Igor Burilo - Bug 211415: Export History log
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history;

import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.remote.GetLogMessagesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.svnstorage.events.IRevisionPropertyChangeListener;
import org.eclipse.team.svn.core.svnstorage.events.RevisonPropertyChangeEvent;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.history.HistoryActionManager.HistoryAction;
import org.eclipse.team.svn.ui.history.data.SVNLocalFileRevision;
import org.eclipse.team.svn.ui.history.filter.AuthorNameLogEntryFilter;
import org.eclipse.team.svn.ui.history.filter.ChangeNameLogEntryFilter;
import org.eclipse.team.svn.ui.history.filter.CommentLogEntryFilter;
import org.eclipse.team.svn.ui.history.filter.CompositeLogEntryFilter;
import org.eclipse.team.svn.ui.history.filter.ILogEntryFilter;
import org.eclipse.team.svn.ui.history.model.ILogNode;
import org.eclipse.team.svn.ui.operation.CorrectRevisionOperation;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;
import org.eclipse.team.svn.ui.panel.view.HistoryFilterPanel;
import org.eclipse.team.svn.ui.panel.view.HistoryRangePanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.repository.model.RepositoryLocation;
import org.eclipse.team.svn.ui.repository.model.RepositoryResource;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;

/**
 * Generic HistoryView page
 * 
 * @author Alexander Gurov
 */
public class SVNHistoryPage extends HistoryPage
		implements ISVNHistoryView, IResourceStatesListener, IPropertyChangeListener, IRevisionPropertyChangeListener {
	protected IResource wcResource;

	protected IRepositoryResource repositoryResource;

	protected SVNRevision endRevision;

	protected SVNRevision startRevision;

	protected LogMessagesComposite history;

	protected CommentLogEntryFilter commentFilter;

	protected AuthorNameLogEntryFilter authorFilter;

	protected ChangeNameLogEntryFilter changeFilter;

	protected CompositeLogEntryFilter logEntriesFilter;

	protected Action showCommentViewerAction;

	protected Action showAffectedPathsViewerAction;

	protected Action hideUnrelatedAction;

	protected Action hideUnrelatedDropDownAction;

	protected Action stopOnCopyAction;

	protected Action stopOnCopyDropDownAction;

	protected Action groupByDateDropDownAction;

	protected Action getNextPageAction;

	protected Action getAllPagesAction;

	protected Action clearFilterDropDownAction;

	protected Action filterDropDownAction;

	protected Action revisionsRangeDropDownAction;

	protected Action hierarchicalAction;

	protected Action flatAction;

	protected Action compareModeAction;

	protected Action compareModeDropDownAction;

	protected Action groupByDateAction;

	protected Action showLocalAction;

	protected Action showRemoteAction;

	protected Action showBothAction;

	protected Action collapseAllAction;

	protected Action exportLogAction;

	protected Action showLocalActionDropDown;

	protected Action showRemoteActionDropDown;

	protected Action showBothActionDropDown;

	protected long limit = 25;

	protected boolean pagingEnabled = false;

	protected boolean pending;

	protected int options = 0;

	protected IResource compareWith;

	protected IRepositoryLocation currentlyInvolvedLocation;

	protected long currentRevision = 0;

	protected SVNLogEntry[] logMessages;

	protected SVNLocalFileRevision[] localHistory;

	protected HistoryActionManager actionManager;

	public SVNHistoryPage(Object input) {
		SVNRemoteStorage.instance().addResourceStatesListener(ResourceStatesChangedEvent.class, this);
		SVNRemoteStorage.instance().addRevisionPropertyChangeListener(this);
		actionManager = new HistoryActionManager(this);
		SVNTeamUIPlugin.instance().getPreferenceStore().addPropertyChangeListener(this);
		authorFilter = new AuthorNameLogEntryFilter();
		commentFilter = new CommentLogEntryFilter();
		changeFilter = new ChangeNameLogEntryFilter();
		logEntriesFilter = new CompositeLogEntryFilter(
				new ILogEntryFilter[] { authorFilter, commentFilter, changeFilter });
	}

	public void setStartRevision(SVNRevision startRevision) {
		this.startRevision = startRevision;
	}

	public SVNRevision getStartRevision() {
		return startRevision;
	}

	public void setEndRevision(SVNRevision endRevision) {
		this.endRevision = endRevision;
	}

	public SVNRevision getEndRevision() {
		return endRevision;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(SVNTeamPreferences.fullHistoryName(SVNTeamPreferences.HISTORY_PAGE_SIZE_NAME))
				|| event.getProperty()
						.equals(SVNTeamPreferences.fullHistoryName(SVNTeamPreferences.HISTORY_PAGING_ENABLE_NAME))) {
			refreshLimitOption();
		}
		if (event.getProperty().startsWith(SVNTeamPreferences.DATE_FORMAT_BASE)) {
			this.refresh(ISVNHistoryView.REFRESH_VIEW);
		}
		if (event.getProperty()
				.equals(SVNTeamPreferences.fullMergeName(SVNTeamPreferences.MERGE_INCLUDE_MERGED_NAME))) {
			this.refresh(ISVNHistoryView.REFRESH_REMOTE);
		}
	}

	@Override
	public void revisionPropertyChanged(RevisonPropertyChangeEvent event) {
		if (currentlyInvolvedLocation == null || !currentlyInvolvedLocation.equals(event.getLocation())) {
			return;
		}
		if (event.getProperty().name.equals("svn:author") //$NON-NLS-1$
				|| event.getProperty().name.equals("svn:log") //$NON-NLS-1$
				|| event.getProperty().name.equals("svn:date")) { //$NON-NLS-1$
			for (int i = 0; i < logMessages.length; i++) {
				SVNLogEntry current = logMessages[i];
				if (SVNRevision.fromNumber(current.revision).equals(event.getRevision())) {
					if (event.getProperty().name.equals("svn:author")) { //$NON-NLS-1$
						logMessages[i] = new SVNLogEntry(current.revision, current.date, event.getProperty().value,
								current.message, current.changedPaths, current.hasChildren());
						if (current.hasChildren()) {
							logMessages[i].addAll(current.getChildren());
						}
					}
					if (event.getProperty().name.equals("svn:log")) { //$NON-NLS-1$
						logMessages[i] = new SVNLogEntry(current.revision, current.date, current.author,
								event.getProperty().value, current.changedPaths, current.hasChildren());
						if (current.hasChildren()) {
							logMessages[i].addAll(current.getChildren());
						}
					}
					if (event.getProperty().name.equals("svn:date")) { //$NON-NLS-1$
						try {
							logMessages[i] = new SVNLogEntry(current.revision,
									new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") //$NON-NLS-1$
											.parse(event.getProperty().value)
											.getTime(),
									current.author, current.message, current.changedPaths, current.hasChildren());
						} catch (ParseException ex) {
							// uninteresting in this context
						}
						if (current.hasChildren()) {
							logMessages[i].addAll(current.getChildren());
						}
					}
				}
			}
			UIMonitorUtility.getDisplay().syncExec(() -> SVNHistoryPage.this.refresh(ISVNHistoryView.REFRESH_VIEW));
		}
	}

	@Override
	public void resourcesStateChanged(ResourceStatesChangedEvent event) {
		IResource resource = wcResource;
		if (resource == null) {
			return;
		}
		ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
		if (!resource.isAccessible() || !FileUtility.isConnected(resource) || !resource.exists()
				|| IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
			disconnectView();
		} else if (event.contains(resource)) {
			if (IStateFilter.SF_ONREPOSITORY.accept(local)) {
				refreshChanges(ISVNHistoryView.REFRESH_ALL);
			} else if (resource instanceof IFile) {
				refreshChanges(ISVNHistoryView.REFRESH_LOCAL);
			}
		}
	}

	public void addFilter(ILogEntryFilter filter) {
		logEntriesFilter.addFilter(filter);
	}

	public void removeFilter(ILogEntryFilter filter) {
		logEntriesFilter.removeFilter(filter);
	}

	public void showHistory(IResource resource) {
		if (!resource.equals(wcResource)) {
			clear();

			wcResource = resource;
			currentlyInvolvedLocation = SVNRemoteStorage.instance()
					.asRepositoryResource(resource)
					.getRepositoryLocation();

			this.refresh(ISVNHistoryView.REFRESH_ALL);
		}
	}

	public void showHistory(IRepositoryResource remoteResource) {
		if (!remoteResource.equals(repositoryResource)) {
			clear();

			repositoryResource = remoteResource;
			currentlyInvolvedLocation = repositoryResource.getRepositoryLocation();

			this.refresh(ISVNHistoryView.REFRESH_ALL);
		}
	}

	public void clear() {
		currentRevision = SVNRevision.INVALID_REVISION_NUMBER;
		repositoryResource = null;
		wcResource = null;
		logMessages = null;
		localHistory = null;
		authorFilter.setAuthorNameToAccept(null);
		commentFilter.setCommentToAccept(null);
		pending = false;
		startRevision = endRevision = null;

		setButtonsEnablement();
		history.refresh(LogMessagesComposite.REFRESH_ALL);
	}

	public void selectRevision(long revision) {
		history.setSelectedRevision(revision);
	}

	public void setCompareWith(IResource compareWith) {
		this.compareWith = compareWith;
	}

	public void setOptions(int mask, int values) {
		options = options & ~mask | mask & values;
		refreshOptionButtons();
	}

	@Override
	public IResource getResource() {
		return wcResource;
	}

	@Override
	public IRepositoryResource getRepositoryResource() {
		return repositoryResource;
	}

	@Override
	public IResource getCompareWith() {
		return compareWith == null ? wcResource : compareWith;
	}

	@Override
	public HistoryPage getHistoryPage() {
		return this;
	}

	@Override
	public SVNLogEntry[] getRemoteHistory() {
		return SVNHistoryPage.filterMessages(logMessages, logEntriesFilter);
	}

	@Override
	public SVNLogEntry[] getFullRemoteHistory() {
		return logMessages;
	}

	@Override
	public SVNLocalFileRevision[] getLocalHistory() {
		return localHistory;
	}

	@Override
	public boolean isAllRemoteHistoryFetched() {
		return !getNextPageAction.isEnabled();
	}

	@Override
	public boolean isFilterEnabled() {
		return authorFilter.getAuthorNameToAccept() != null || commentFilter.getCommentToAccept() != null
				|| changeFilter.getGangedPathToAccept() != null;
	}

	@Override
	public int getOptions() {
		return options;
	}

	@Override
	public void clearFilter() {
		authorFilter.setAuthorNameToAccept(null);
		commentFilter.setCommentToAccept(null);
		changeFilter.setGangedPathToAccept(null);
		clearFilterDropDownAction.setEnabled(false);
		history.refresh(LogMessagesComposite.REFRESH_ALL);
	}

	@Override
	public void setFilter() {
		HistoryFilterPanel panel = new HistoryFilterPanel(authorFilter.getAuthorNameToAccept(),
				commentFilter.getCommentToAccept(), changeFilter.getGangedPathToAccept(),
				SVNHistoryPage.getSelectedAuthors(logMessages));
		DefaultDialog dialog = new DefaultDialog(getPartSite().getShell(), panel);
		if (dialog.open() == 0) {
			authorFilter.setAuthorNameToAccept(panel.getAuthor());
			commentFilter.setCommentToAccept(panel.getComment());
			changeFilter.setGangedPathToAccept(panel.getChangedPath());
			clearFilterDropDownAction.setEnabled(isFilterEnabled());
			history.refresh(LogMessagesComposite.REFRESH_ALL);
		}
	}

	@Override
	public void refresh(int refreshType) {
		IResource resource = wcResource;
		if (resource != null) {
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
			if (IStateFilter.SF_ONREPOSITORY.accept(local)) {
				currentRevision = local.getRevision();
				repositoryResource = local.isCopied()
						? SVNUtility.getCopiedFrom(resource)
						: SVNRemoteStorage.instance().asRepositoryResource(resource);
			} else {
				repositoryResource = null;
			}

			if (resource instanceof IFile && refreshType != ISVNHistoryView.REFRESH_REMOTE
					&& refreshType != ISVNHistoryView.REFRESH_VIEW) {
				try {
					fetchLocalHistory(local, new NullProgressMonitor());
				} catch (CoreException ex) {
					UILoggedOperation.reportError(SVNUIMessages.HistoryView_Name, ex);
				}
			}
		}

		if (repositoryResource != null
				&& (refreshType == ISVNHistoryView.REFRESH_ALL || refreshType == ISVNHistoryView.REFRESH_REMOTE)) {
			logMessages = null;
			pending = true;
			setButtonsEnablement();
			history.refresh(LogMessagesComposite.REFRESH_ALL);
			GetLogMessagesOperation msgOp = createRemoteHistoryFetcher();
			if (startRevision != null) {
				msgOp.setStartRevision(startRevision);
			}
			msgOp.setLimit(limit);
			fetchRemoteHistory(msgOp);
		} else {
			setButtonsEnablement();
			history.refresh(LogMessagesComposite.REFRESH_ALL);
		}
	}

	@Override
	public long getCurrentRevision() {
		return currentRevision;
	}

	@Override
	public boolean isGrouped() {
		return groupByDateAction.isChecked();
	}

	@Override
	public boolean isPending() {
		return pending;
	}

	@Override
	public int getMode() {
		return options & ISVNHistoryViewInfo.MODE_MASK;
	}

	@Override
	public boolean isRelatedPathsOnly() {
		return hideUnrelatedAction.isChecked();
	}

	@Override
	public void dispose() {
		SVNRemoteStorage.instance().removeResourceStatesListener(ResourceStatesChangedEvent.class, this);
		SVNRemoteStorage.instance().removeRevisionPropertyChangeListener(this);
		SVNTeamUIPlugin.instance().getPreferenceStore().removePropertyChangeListener(this);
		// log messages composite is disposed by HistoryPage.dispose()
		super.dispose();
	}

	@Override
	public boolean inputSet() {
		if (getInput() instanceof IResource) {
			this.showHistory((IResource) getInput());
			return true;
		} else if (getInput() instanceof IRepositoryResource) {
			this.showHistory((IRepositoryResource) getInput());
			return true;
		} else if (getInput() instanceof RepositoryResource) {
			this.showHistory(((RepositoryResource) getInput()).getRepositoryResource());
			return true;
		} else if (getInput() instanceof RepositoryLocation) {
			this.showHistory(((RepositoryLocation) getInput()).getRepositoryResource());
			return true;
		}
		return false;
	}

	@Override
	public Control getControl() {
		return history;
	}

	@Override
	public void setFocus() {

	}

	@Override
	public String getDescription() {
		return getName();
	}

	@Override
	public String getName() {
		if (getResource() != null) {
			return getResource().getFullPath().toString().substring(1);
		}
		if (getRepositoryResource() != null) {
			return getRepositoryResource().getUrl();
		}
		return SVNUIMessages.SVNView_ResourceNotSelected;
	}

	@Override
	public boolean isValidInput(Object object) {
		return SVNHistoryPage.isValidData(object);
	}

	@Override
	public void refresh() {
		this.refresh(ISVNHistoryView.REFRESH_ALL);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public void createControl(Composite parent) {
		IActionBars actionBars = getHistoryPageSite().getWorkbenchPageSite().getActionBars();

		groupByDateAction = new HistoryAction(SVNUIMessages.HistoryView_GroupByDate,
				"icons/views/history/group_by_date.gif", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				options ^= ISVNHistoryView.GROUP_BY_DATE;
				groupByDateDropDownAction.setChecked((options & ISVNHistoryView.GROUP_BY_DATE) != 0);
				SVNHistoryPage.saveInt(SVNTeamPreferences.HISTORY_GROUPING_TYPE_NAME,
						(options & ISVNHistoryView.GROUP_BY_DATE) == 0
								? SVNTeamPreferences.HISTORY_GROUPING_TYPE_NONE
								: SVNTeamPreferences.HISTORY_GROUPING_TYPE_DATE);
				history.refresh(LogMessagesComposite.REFRESH_UI_ALL);
			}
		};

		showBothAction = new HistoryAction(SVNUIMessages.HistoryView_ShowBoth,
				"icons/views/history/both_history_mode.gif", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				options = options & ~(ISVNHistoryViewInfo.MODE_LOCAL | ISVNHistoryViewInfo.MODE_REMOTE)
						| ISVNHistoryViewInfo.MODE_BOTH;
				showBothActionDropDown.setChecked(true);
				showLocalActionDropDown.setChecked(false);
				showRemoteActionDropDown.setChecked(false);
				SVNHistoryPage.this.saveShowMode();
				history.refresh(LogMessagesComposite.REFRESH_UI_ALL);
			}
		};
		showLocalAction = new HistoryAction(SVNUIMessages.HistoryView_ShowLocal,
				"icons/views/history/local_history_mode.gif", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				options = options & ~(ISVNHistoryViewInfo.MODE_REMOTE | ISVNHistoryViewInfo.MODE_BOTH)
						| ISVNHistoryViewInfo.MODE_LOCAL;
				showLocalActionDropDown.setChecked(true);
				showRemoteActionDropDown.setChecked(false);
				showBothActionDropDown.setChecked(false);
				SVNHistoryPage.this.saveShowMode();
				history.refresh(LogMessagesComposite.REFRESH_UI_ALL);
			}
		};
		showRemoteAction = new HistoryAction(SVNUIMessages.HistoryView_ShowRemote,
				"icons/views/history/remote_history_mode.gif", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				options = options & ~(ISVNHistoryViewInfo.MODE_LOCAL | ISVNHistoryViewInfo.MODE_BOTH)
						| ISVNHistoryViewInfo.MODE_REMOTE;
				showRemoteActionDropDown.setChecked(true);
				showLocalActionDropDown.setChecked(false);
				showBothActionDropDown.setChecked(false);
				SVNHistoryPage.this.saveShowMode();
				history.refresh(LogMessagesComposite.REFRESH_UI_ALL);
			}
		};

		hideUnrelatedAction = new HistoryAction(SVNUIMessages.HistoryView_HideUnrelatedPaths,
				"icons/views/history/hide_unrelated.gif", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				options ^= ISVNHistoryView.HIDE_UNRELATED;
				hideUnrelatedDropDownAction.setChecked((options & ISVNHistoryView.HIDE_UNRELATED) != 0);
				history.refresh(LogMessagesComposite.REFRESH_UI_AFFECTED);
			}
		};
		stopOnCopyAction = new HistoryAction(SVNUIMessages.HistoryView_StopOnCopy,
				"icons/views/history/stop_on_copy.gif", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				options ^= ISVNHistoryView.STOP_ON_COPY;
				stopOnCopyDropDownAction.setChecked((options & ISVNHistoryView.STOP_ON_COPY) != 0);
				SVNHistoryPage.this.refresh(ISVNHistoryView.REFRESH_REMOTE);
			}
		};

		getNextPageAction = new HistoryAction(SVNUIMessages.HistoryView_GetNextPage, "icons/views/history/paging.gif") { //$NON-NLS-1$
			@Override
			public void run() {
				GetLogMessagesOperation msgOp = SVNHistoryPage.this.createRemoteHistoryFetcher();
				msgOp.setLimit(limit + 1);
				if (logMessages != null) {
					SVNLogEntry lm = logMessages[logMessages.length - 1];
					msgOp.setStartRevision(SVNRevision.fromNumber(lm.revision));
				}
				SVNHistoryPage.this.fetchRemoteHistory(msgOp);
			}
		};
		String msg = limit > 0
				? BaseMessages.format(SVNUIMessages.HistoryView_ShowNextX, new String[] { String.valueOf(limit) })
				: SVNUIMessages.HistoryView_ShowNextPage;
		getNextPageAction.setToolTipText(msg);
		getAllPagesAction = new HistoryAction(SVNUIMessages.HistoryView_ShowAll, "icons/views/history/paging_all.gif") { //$NON-NLS-1$
			@Override
			public void run() {
				GetLogMessagesOperation msgOp = SVNHistoryPage.this.createRemoteHistoryFetcher();
				msgOp.setLimit(0);
				if (logMessages != null) {
					SVNLogEntry lm = logMessages[logMessages.length - 1];
					msgOp.setStartRevision(SVNRevision.fromNumber(lm.revision));
				}
				SVNHistoryPage.this.fetchRemoteHistory(msgOp);
			}
		};

		collapseAllAction = new HistoryAction(SVNUIMessages.RepositoriesView_CollapseAll_Label,
				"icons/common/collapseall.gif") { //$NON-NLS-1$
			@Override
			public void run() {
				history.collapseAll();
			}
		};
		compareModeAction = new HistoryAction(SVNUIMessages.HistoryView_CompareMode,
				"icons/views/history/compare_mode.gif", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				options ^= ISVNHistoryView.COMPARE_MODE;
				compareModeDropDownAction.setChecked((options & ISVNHistoryView.COMPARE_MODE) != 0);
				SVNHistoryPage.saveBoolean(SVNTeamPreferences.HISTORY_COMPARE_MODE,
						(options & ISVNHistoryView.COMPARE_MODE) != 0);
			}
		};

		IToolBarManager tbm = actionBars.getToolBarManager();
		tbm.add(new Separator());
		tbm.add(groupByDateAction);
		tbm.add(new Separator());
		tbm.add(showBothAction);
		tbm.add(showLocalAction);
		tbm.add(showRemoteAction);
		tbm.add(new Separator());
		tbm.add(hideUnrelatedAction);
		tbm.add(stopOnCopyAction);
		tbm.add(new Separator());
		tbm.add(getNextPageAction);
		tbm.add(getAllPagesAction);
		tbm.add(new Separator());
		tbm.add(collapseAllAction);
		tbm.add(compareModeAction);

		// drop-down menu
		showCommentViewerAction = new HistoryAction(SVNUIMessages.HistoryView_ShowCommentViewer) {
			@Override
			public void run() {
				history.setCommentViewerVisible(showCommentViewerAction.isChecked());
				SVNHistoryPage.saveBoolean(SVNTeamPreferences.HISTORY_SHOW_MULTILINE_COMMENT_NAME, isChecked());
			}
		};
		showAffectedPathsViewerAction = new HistoryAction(SVNUIMessages.HistoryView_ShowAffectedPathsViewer) {
			@Override
			public void run() {
				boolean showAffected = isChecked();
				history.setAffectedPathsViewerVisible(showAffected);
				flatAction.setEnabled(showAffected);
				hierarchicalAction.setEnabled(showAffected);
				SVNHistoryPage.saveBoolean(SVNTeamPreferences.HISTORY_SHOW_AFFECTED_PATHS_NAME, showAffected);
			}
		};

		hideUnrelatedDropDownAction = new HistoryAction(SVNUIMessages.HistoryView_HideUnrelatedPaths,
				"icons/views/history/hide_unrelated.gif") { //$NON-NLS-1$
			@Override
			public void run() {
				options ^= ISVNHistoryView.HIDE_UNRELATED;
				hideUnrelatedAction.setChecked((options & ISVNHistoryView.HIDE_UNRELATED) != 0);
				history.refresh(LogMessagesComposite.REFRESH_UI_AFFECTED);
			}
		};
		stopOnCopyDropDownAction = new HistoryAction(SVNUIMessages.HistoryView_StopOnCopy,
				"icons/views/history/stop_on_copy.gif") { //$NON-NLS-1$
			@Override
			public void run() {
				options ^= ISVNHistoryView.STOP_ON_COPY;
				stopOnCopyAction.setChecked((options & ISVNHistoryView.STOP_ON_COPY) != 0);
				SVNHistoryPage.this.refresh(ISVNHistoryView.REFRESH_REMOTE);
			}
		};

		groupByDateDropDownAction = new HistoryAction(SVNUIMessages.HistoryView_GroupByDate,
				"icons/views/history/group_by_date.gif") { //$NON-NLS-1$
			@Override
			public void run() {
				options ^= ISVNHistoryView.GROUP_BY_DATE;
				groupByDateAction.setChecked((options & ISVNHistoryView.GROUP_BY_DATE) != 0);
				SVNHistoryPage.saveInt(SVNTeamPreferences.HISTORY_GROUPING_TYPE_NAME,
						(options & ISVNHistoryView.GROUP_BY_DATE) == 0
								? SVNTeamPreferences.HISTORY_GROUPING_TYPE_NONE
								: SVNTeamPreferences.HISTORY_GROUPING_TYPE_DATE);
				history.refresh(LogMessagesComposite.REFRESH_UI_ALL);
			}
		};

		exportLogAction = new HistoryAction(SVNUIMessages.HistoryView_ExportLog) {
			@Override
			public void run() {
				FileDialog dlg = new FileDialog(UIMonitorUtility.getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
				dlg.setText(SVNUIMessages.ExportLogDialog_Title);
				String caption = SVNHistoryPage.this.getName();
				dlg.setFileName(caption.substring(caption.lastIndexOf('/') + 1) + "_history.log"); //$NON-NLS-1$
				dlg.setFilterExtensions(new String[] { "log", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
				String file = dlg.open();
				if (file != null) {
					ILogNode input = (ILogNode) history.getTreeViewer().getInput();
					try {
						FileOutputStream stream = new FileOutputStream(file);
						stream.write(
								actionManager.getSelectedMessagesAsString(new ILogNode[] { input }).getBytes());
						stream.flush();
						stream.close();
					} catch (Exception ex) {
						LoggedOperation.reportError(SVNUIMessages.Operation_ExportLog, ex);
					}
				}
			}
		};

		showBothActionDropDown = new HistoryAction(SVNUIMessages.HistoryView_ShowBoth,
				"icons/views/history/both_history_mode.gif", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				options = options & ~(ISVNHistoryViewInfo.MODE_LOCAL | ISVNHistoryViewInfo.MODE_REMOTE)
						| ISVNHistoryViewInfo.MODE_BOTH;
				showBothAction.setChecked(true);
				showLocalAction.setChecked(false);
				showRemoteAction.setChecked(false);
				SVNHistoryPage.this.saveShowMode();
				history.refresh(LogMessagesComposite.REFRESH_UI_ALL);
			}
		};
		showLocalActionDropDown = new HistoryAction(SVNUIMessages.HistoryView_ShowLocal,
				"icons/views/history/local_history_mode.gif", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				options = options & ~(ISVNHistoryViewInfo.MODE_REMOTE | ISVNHistoryViewInfo.MODE_BOTH)
						| ISVNHistoryViewInfo.MODE_LOCAL;
				showBothAction.setChecked(false);
				showLocalAction.setChecked(true);
				showRemoteAction.setChecked(false);
				SVNHistoryPage.this.saveShowMode();
				history.refresh(LogMessagesComposite.REFRESH_UI_ALL);
			}
		};
		showRemoteActionDropDown = new HistoryAction(SVNUIMessages.HistoryView_ShowRemote,
				"icons/views/history/remote_history_mode.gif", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				options = options & ~(ISVNHistoryViewInfo.MODE_LOCAL | ISVNHistoryViewInfo.MODE_BOTH)
						| ISVNHistoryViewInfo.MODE_REMOTE;
				showBothAction.setChecked(false);
				showLocalAction.setChecked(false);
				showRemoteAction.setChecked(true);
				SVNHistoryPage.this.saveShowMode();
				history.refresh(LogMessagesComposite.REFRESH_UI_ALL);
			}
		};

		revisionsRangeDropDownAction = new HistoryAction(SVNUIMessages.HistoryView_RevisionsRange) {
			@Override
			public void run() {
				SVNHistoryPage.this.defineRevisionsRange();
			}
		};

		filterDropDownAction = new HistoryAction(SVNUIMessages.HistoryView_QuickFilter,
				"icons/views/history/filter.gif") { //$NON-NLS-1$
			@Override
			public void run() {
				SVNHistoryPage.this.setFilter();
			}
		};
		clearFilterDropDownAction = new HistoryAction(SVNUIMessages.HistoryView_ClearFilter,
				"icons/views/history/clear_filter.gif") { //$NON-NLS-1$
			@Override
			public void run() {
				SVNHistoryPage.this.clearFilter();
			}
		};

		compareModeDropDownAction = new HistoryAction(SVNUIMessages.HistoryView_CompareMode,
				"icons/views/history/compare_mode.gif") { //$NON-NLS-1
			@Override
			public void run() {
				options ^= ISVNHistoryView.COMPARE_MODE;
				compareModeAction.setChecked((options & ISVNHistoryView.COMPARE_MODE) != 0);
				SVNHistoryPage.saveBoolean(SVNTeamPreferences.HISTORY_COMPARE_MODE,
						(options & ISVNHistoryView.COMPARE_MODE) != 0);
			}
		};

		flatAction = new HistoryAction(SVNUIMessages.HistoryView_Flat, "icons/views/history/flat_layout.gif", //$NON-NLS-1$
				IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				history.setResourceTreeVisible(false);
				SVNHistoryPage.saveBoolean(SVNTeamPreferences.HISTORY_HIERARCHICAL_LAYOUT, false);
			}
		};
		hierarchicalAction = new HistoryAction(SVNUIMessages.HistoryView_Hierarchical,
				"icons/views/history/tree_layout.gif", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				history.setResourceTreeVisible(true);
				SVNHistoryPage.saveBoolean(SVNTeamPreferences.HISTORY_HIERARCHICAL_LAYOUT, true);
			}
		};

		IMenuManager actionBarsMenu = actionBars.getMenuManager();
		actionBarsMenu.add(showCommentViewerAction);
		actionBarsMenu.add(showAffectedPathsViewerAction);
		MenuManager sub = new MenuManager(SVNUIMessages.HistoryView_AffectedPathLayout,
				IWorkbenchActionConstants.GROUP_MANAGING);
		sub.add(flatAction);
		sub.add(hierarchicalAction);
		actionBarsMenu.add(sub);
		actionBarsMenu.add(new Separator());
		actionBarsMenu.add(groupByDateDropDownAction);
		actionBarsMenu.add(new Separator());
		actionBarsMenu.add(showBothActionDropDown);
		actionBarsMenu.add(showLocalActionDropDown);
		actionBarsMenu.add(showRemoteActionDropDown);
		actionBarsMenu.add(new Separator());
		actionBarsMenu.add(hideUnrelatedDropDownAction);
		actionBarsMenu.add(stopOnCopyDropDownAction);
		actionBarsMenu.add(new Separator());
		actionBarsMenu.add(revisionsRangeDropDownAction);
		actionBarsMenu.add(filterDropDownAction);
		actionBarsMenu.add(clearFilterDropDownAction);
		actionBarsMenu.add(exportLogAction);
		actionBarsMenu.add(new Separator());
		actionBarsMenu.add(compareModeDropDownAction);

		history = new LogMessagesComposite(parent, true, false, this);

		GridData data = new GridData(GridData.FILL_BOTH);
		history.setLayoutData(data);

		history.registerActionManager(actionManager, getPartSite());

		// Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.historyViewContext"); //$NON-NLS-1$

		refreshOptionButtons();
	}

	public static String[] getSelectedAuthors(SVNLogEntry[] logMessages) {
		HashSet<String> authors = new HashSet<>();
		if (logMessages != null) {
			for (SVNLogEntry entry : logMessages) {
				if (entry.author != null) {
					authors.add(entry.author);
				}
			}
		}
		return authors.toArray(new String[authors.size()]);
	}

	public static SVNLogEntry[] filterMessages(SVNLogEntry[] msgs, ILogEntryFilter filter) {
		if (msgs == null) {
			return null;
		}
		ArrayList<SVNLogEntry> filteredMessages = new ArrayList<>();
		for (SVNLogEntry msg : msgs) {
			if (filter.accept(msg)) {
				filteredMessages.add(msg);
			}
		}
		if (filteredMessages.size() == 0) {
			return null;
		}
		return filteredMessages.toArray(new SVNLogEntry[filteredMessages.size()]);
	}

	public static boolean isValidData(Object object) {
		return object instanceof IRepositoryResource || object instanceof RepositoryResource
				|| object instanceof RepositoryLocation
				|| object instanceof IResource && FileUtility.isConnected((IResource) object);
	}

	protected IWorkbenchPartSite getPartSite() {
		IWorkbenchPart part = getHistoryPageSite().getPart();
		if (part == null) {
			return null;
		}
		IWorkbenchPartSite site = part.getSite();
		while (site == null) {
			try {
				// await while site is initialized, see IWorkbenchPart.getSite() documentation
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				break;
			}
			site = part.getSite();
		}
		return site;
	}

	protected GetLogMessagesOperation createRemoteHistoryFetcher() {
		GetLogMessagesOperation msgOp = new GetLogMessagesOperation(repositoryResource, stopOnCopyAction.isChecked());
		if (endRevision != null) {
			msgOp.setEndRevision(endRevision);
		}

		/* if merge info isn't supported by server then retry without it,
		 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=257669
		 */
		msgOp.setRetryIfMergeInfoNotSupported(true);
		// set revision range here
		return msgOp;
	}

	protected void fetchRemoteHistory(final GetLogMessagesOperation msgsOp) {
		msgsOp.setIncludeMerged(SVNTeamPreferences.getMergeBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.MERGE_INCLUDE_MERGED_NAME));

		final IStructuredSelection selected = (IStructuredSelection) history.getTreeViewer().getSelection();
		IActionOperation showOp = new AbstractActionOperation("Operation_HShowHistory", SVNUIMessages.class) { //$NON-NLS-1$
			private long revision = currentRevision;

			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				IRepositoryResource resource = repositoryResource;
				if (msgsOp.getExecutionState() != IActionOperation.OK || resource == null) {
					pending = false;
					UIMonitorUtility.getDisplay().syncExec(() -> history.refresh(LogMessagesComposite.REFRESH_ALL));
					return;
				}
				if (wcResource == null) {
					revision = resource.getRevision();
				}

				if (!resource.equals(msgsOp.getResource())) {
					return;
				}

				currentRevision = revision;
				SVNHistoryPage.this.addPage(msgsOp.getMessages());

				UIMonitorUtility.getDisplay().syncExec(() -> {
					history.refresh(LogMessagesComposite.REFRESH_ALL);
					SVNHistoryPage.this.setButtonsEnablement();

					TreeViewer treeTable = history.getTreeViewer();
					if (!treeTable.getTree().isDisposed() && treeTable.getTree().getItems().length > 0) {
						if (selected.size() != 0) {
							treeTable.setSelection(selected, true);
						} else {
							TreeItem firstItem = treeTable.getTree().getItem(0);
							if (((ILogNode) firstItem.getData()).getType() == ILogNode.TYPE_CATEGORY) {
								firstItem = firstItem.getItem(0);
							}
							treeTable.getTree().setSelection(firstItem);
						}
						history.refresh(LogMessagesComposite.REFRESH_UI_AFFECTED);
					}
				});
			}
		};
		CompositeOperation op = new CompositeOperation(showOp.getId(), showOp.getMessagesClass(), true);
		op.add(new CorrectRevisionOperation(msgsOp, repositoryResource, currentRevision, wcResource));
		op.add(msgsOp);
		op.add(showOp);

		ProgressMonitorUtility.doTaskScheduled(op, false);
	}

	protected void fetchLocalHistory(ILocalResource local, IProgressMonitor monitor) throws CoreException {
		IFile file = (IFile) wcResource;
		ArrayList<SVNLocalFileRevision> history = new ArrayList<>();
		IFileState[] states = file.getHistory(monitor);
		if (states.length > 0 || IStateFilter.SF_NOTONREPOSITORY.accept(local)) {
			history.add(new SVNLocalFileRevision(file));
		}
		for (IFileState state : states) {
			history.add(new SVNLocalFileRevision(state));
		}
		localHistory = history.size() == 0 ? null : history.toArray(new SVNLocalFileRevision[history.size()]);
	}

	protected void addPage(SVNLogEntry[] newMessages) {
		if (logMessages == null) {
			if (newMessages.length > 0) {
				pending = false;
				logMessages = newMessages;
				pagingEnabled = limit > 0 && newMessages.length == limit;
			}
		} else if (newMessages.length > 1) {
			LinkedHashSet<SVNLogEntry> entries = new LinkedHashSet<>(Arrays.asList(logMessages));
			int oldSize = entries.size();
			entries.addAll(Arrays.asList(newMessages));
			logMessages = entries.toArray(new SVNLogEntry[entries.size()]);
			pagingEnabled = limit > 0
					&& (newMessages.length == limit + 1 || entries.size() - oldSize < newMessages.length - 1);
		}
	}

	protected void refreshOptionButtons() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();

		boolean showMultiline = SVNTeamPreferences.getHistoryBoolean(store,
				SVNTeamPreferences.HISTORY_SHOW_MULTILINE_COMMENT_NAME);
		showCommentViewerAction.setChecked(showMultiline);
		showCommentViewerAction.run();

		boolean showAffected = SVNTeamPreferences.getHistoryBoolean(store,
				SVNTeamPreferences.HISTORY_SHOW_AFFECTED_PATHS_NAME);
		showAffectedPathsViewerAction.setChecked(showAffected);
		showAffectedPathsViewerAction.run();

		boolean hierarchicalAffectedView = SVNTeamPreferences.getHistoryBoolean(store,
				SVNTeamPreferences.HISTORY_HIERARCHICAL_LAYOUT);
		int groupingType = SVNTeamPreferences.getHistoryInt(store, SVNTeamPreferences.HISTORY_GROUPING_TYPE_NAME);
		int revisionMode = SVNTeamPreferences.getHistoryInt(store, SVNTeamPreferences.HISTORY_REVISION_MODE_NAME);
		if (revisionMode == 0) {
			revisionMode = ISVNHistoryViewInfo.MODE_BOTH;
		} else if (revisionMode == 1) {
			revisionMode = ISVNHistoryViewInfo.MODE_REMOTE;
		} else {
			revisionMode = ISVNHistoryViewInfo.MODE_LOCAL;
		}

		boolean compareMode = SVNTeamPreferences.getHistoryBoolean(store, SVNTeamPreferences.HISTORY_COMPARE_MODE);
		if (compareMode) {
			options |= ISVNHistoryView.COMPARE_MODE;
		}
		options |= groupingType == SVNTeamPreferences.HISTORY_GROUPING_TYPE_DATE ? ISVNHistoryView.GROUP_BY_DATE : 0;
		options = options
				& ~(ISVNHistoryViewInfo.MODE_BOTH | ISVNHistoryViewInfo.MODE_LOCAL | ISVNHistoryViewInfo.MODE_REMOTE)
				| revisionMode;

		hideUnrelatedDropDownAction.setChecked((options & ISVNHistoryView.HIDE_UNRELATED) != 0);
		hideUnrelatedAction.setChecked((options & ISVNHistoryView.HIDE_UNRELATED) != 0);
		stopOnCopyDropDownAction.setChecked((options & ISVNHistoryView.STOP_ON_COPY) != 0);
		stopOnCopyAction.setChecked((options & ISVNHistoryView.STOP_ON_COPY) != 0);
		groupByDateAction.setChecked((options & ISVNHistoryView.GROUP_BY_DATE) != 0);
		showBothAction.setChecked((options & ISVNHistoryViewInfo.MODE_BOTH) != 0);
		showBothActionDropDown.setChecked((options & ISVNHistoryViewInfo.MODE_BOTH) != 0);
		showLocalAction.setChecked((options & ISVNHistoryViewInfo.MODE_LOCAL) != 0);
		showLocalActionDropDown.setChecked((options & ISVNHistoryViewInfo.MODE_LOCAL) != 0);
		showRemoteAction.setChecked((options & ISVNHistoryViewInfo.MODE_REMOTE) != 0);
		showRemoteActionDropDown.setChecked((options & ISVNHistoryViewInfo.MODE_REMOTE) != 0);
		groupByDateDropDownAction.setChecked((options & ISVNHistoryView.GROUP_BY_DATE) != 0);
		compareModeDropDownAction.setChecked((options & ISVNHistoryView.COMPARE_MODE) != 0);
		compareModeAction.setChecked((options & ISVNHistoryView.COMPARE_MODE) != 0);

		flatAction.setChecked(!hierarchicalAffectedView);
		hierarchicalAction.setChecked(hierarchicalAffectedView);
		history.setResourceTreeVisible(hierarchicalAffectedView);

		refreshLimitOption();
		setButtonsEnablement();
	}

	protected void defineRevisionsRange() {
		HistoryRangePanel panel = new HistoryRangePanel(repositoryResource, startRevision, endRevision);
		DefaultDialog dialog = new DefaultDialog(getPartSite().getShell(), panel);
		if (dialog.open() == 0) {
			startRevision = panel.getStartRevision();
			endRevision = panel.getStopRevision();
			this.refresh(ISVNHistoryView.REFRESH_REMOTE);
		}
	}

	protected void refreshLimitOption() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();

		if (SVNTeamPreferences.getHistoryBoolean(store, SVNTeamPreferences.HISTORY_PAGING_ENABLE_NAME)) {
			limit = SVNTeamPreferences.getHistoryInt(store, SVNTeamPreferences.HISTORY_PAGE_SIZE_NAME);
			getNextPageAction.setToolTipText("Show Next " + limit);
			options |= ISVNHistoryView.PAGING_ENABLED;
		} else {
			limit = 0;
			getNextPageAction.setToolTipText("Show Next Page");
			options &= ~ISVNHistoryView.PAGING_ENABLED;
		}
	}

	protected void setButtonsEnablement() {
		ILocalResource local = SVNRemoteStorage.instance().asLocalResource(wcResource);
		boolean enableRepo = (IStateFilter.SF_ONREPOSITORY.accept(local) || repositoryResource != null) && !pending;

		revisionsRangeDropDownAction.setEnabled(repositoryResource != null);
		filterDropDownAction.setEnabled(enableRepo && repositoryResource != null && logMessages != null);
		clearFilterDropDownAction.setEnabled(isFilterEnabled());
		getNextPageAction.setEnabled(enableRepo && pagingEnabled & (options & ISVNHistoryView.PAGING_ENABLED) != 0);
		getAllPagesAction.setEnabled(enableRepo && pagingEnabled & (options & ISVNHistoryView.PAGING_ENABLED) != 0);

		stopOnCopyAction.setEnabled(enableRepo);
		stopOnCopyDropDownAction.setEnabled(enableRepo);
		hideUnrelatedAction.setEnabled(enableRepo);
		hideUnrelatedDropDownAction.setEnabled(enableRepo);
	}

	protected void saveShowMode() {
		int prefToSet = SVNTeamPreferences.HISTORY_REVISION_MODE_REMOTE;
		if ((options & ISVNHistoryViewInfo.MODE_BOTH) != 0) {
			prefToSet = SVNTeamPreferences.HISTORY_REVISION_MODE_BOTH;
		} else if ((options & ISVNHistoryViewInfo.MODE_LOCAL) != 0) {
			prefToSet = SVNTeamPreferences.HISTORY_REVISION_MODE_LOCAL;
		}
		SVNHistoryPage.saveInt(SVNTeamPreferences.HISTORY_REVISION_MODE_NAME, prefToSet);
	}

	protected static void saveBoolean(String name, boolean value) {
		SVNTeamPreferences.setHistoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(), name, value);
		SVNTeamUIPlugin.instance().savePreferences();
	}

	protected static void saveInt(String name, int value) {
		SVNTeamPreferences.setHistoryInt(SVNTeamUIPlugin.instance().getPreferenceStore(), name, value);
		SVNTeamUIPlugin.instance().savePreferences();
	}

	protected void disconnectView() {
		UIMonitorUtility.getDisplay().syncExec(SVNHistoryPage.this::clear);
	}

	protected void refreshChanges(final int refreshType) {
		UIMonitorUtility.getDisplay().syncExec(() -> SVNHistoryPage.this.refresh(refreshType));
	}

}
