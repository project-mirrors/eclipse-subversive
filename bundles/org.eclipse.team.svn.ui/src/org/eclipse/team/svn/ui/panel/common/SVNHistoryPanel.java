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

package org.eclipse.team.svn.ui.panel.common;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.GetLogMessagesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.history.ISVNHistoryViewInfo;
import org.eclipse.team.svn.ui.history.LogMessagesComposite;
import org.eclipse.team.svn.ui.history.SVNHistoryPage;
import org.eclipse.team.svn.ui.history.data.SVNLocalFileRevision;
import org.eclipse.team.svn.ui.history.filter.AuthorNameLogEntryFilter;
import org.eclipse.team.svn.ui.history.filter.ChangeNameLogEntryFilter;
import org.eclipse.team.svn.ui.history.filter.CommentLogEntryFilter;
import org.eclipse.team.svn.ui.history.filter.CompositeLogEntryFilter;
import org.eclipse.team.svn.ui.history.filter.ILogEntryFilter;
import org.eclipse.team.svn.ui.history.model.ILogNode;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.panel.view.HistoryFilterPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Show history for resource in panel, i.e. it can be used in a dialog This is a simplified version of history view, e.g. it doesn't contain
 * actions for affected paths etc.
 * 
 * @author Alexander Gurov
 */
public class SVNHistoryPanel extends AbstractDialogPanel implements ISVNHistoryViewInfo {
	protected LogMessagesComposite history;

	protected SVNLogEntry[] logMessages;

	protected IRepositoryResource resource;

	protected long currentRevision;

	protected boolean multiSelect;

	protected boolean useCheckboxes;

	protected long limit;

	protected boolean pagingEnabled;

	protected Text resourceLabel;

	protected ToolItem hideUnrelatedItem;

	protected ToolItem stopOnCopyItem;

	protected ToolItem pagingItem;

	protected ToolItem pagingAllItem;

	protected ToolItem filterItem;

	protected ToolItem clearFilterItem;

	protected ToolItem refreshItem;

	protected ToolItem groupByDateItem;

	protected CommentLogEntryFilter commentFilter;

	protected AuthorNameLogEntryFilter authorFilter;

	protected ChangeNameLogEntryFilter changeFilter;

	protected CompositeLogEntryFilter logEntriesFilter;

	protected ISelectionChangedListener tableViewerListener;

	protected IPropertyChangeListener configurationListener;

	protected boolean initialStopOnCopy;

	protected boolean pending;

	public SVNHistoryPanel(String title, String description, String message, GetLogMessagesOperation msgOp,
			boolean multiSelect, boolean useCheckboxes, long currentRevision) {
		this.multiSelect = multiSelect;
		this.useCheckboxes = useCheckboxes;
		dialogTitle = title;
		dialogDescription = description;
		defaultMessage = message;
		resource = msgOp.getResource();
		this.currentRevision = currentRevision;
		logMessages = msgOp.getMessages();
		initialStopOnCopy = msgOp.getStopOnCopy();
		authorFilter = new AuthorNameLogEntryFilter();
		commentFilter = new CommentLogEntryFilter();
		changeFilter = new ChangeNameLogEntryFilter();
		logEntriesFilter = new CompositeLogEntryFilter(
				new ILogEntryFilter[] { authorFilter, commentFilter, changeFilter });
	}

	public static GetLogMessagesOperation getMsgsOp(IRepositoryResource resource, boolean stopOnCopy) {
		GetLogMessagesOperation msgsOp = new GetLogMessagesOperation(resource, stopOnCopy);
		msgsOp.setIncludeMerged(SVNTeamPreferences.getMergeBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.MERGE_INCLUDE_MERGED_NAME));

		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		if (SVNTeamPreferences.getHistoryBoolean(store, SVNTeamPreferences.HISTORY_PAGING_ENABLE_NAME)) {
			msgsOp.setLimit(SVNTeamPreferences.getHistoryInt(store, SVNTeamPreferences.HISTORY_PAGE_SIZE_NAME));
		}
		return msgsOp;
	}

	@Override
	public String getImagePath() {
		return "icons/dialogs/select_revision.gif"; //$NON-NLS-1$
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(700, SWT.DEFAULT);
	}

	@Override
	public boolean isGrouped() {
		return groupByDateItem.getSelection();
	}

	@Override
	public long getCurrentRevision() {
		return currentRevision;
	}

	@Override
	public IRepositoryResource getRepositoryResource() {
		return resource;
	}

	@Override
	public SVNLogEntry[] getRemoteHistory() {
		return SVNHistoryPage.filterMessages(logMessages, logEntriesFilter);
	}

	@Override
	public SVNLocalFileRevision[] getLocalHistory() {
		return null;
	}

	@Override
	public IResource getResource() {
		return null;
	}

	@Override
	public int getMode() {
		return ISVNHistoryViewInfo.MODE_REMOTE;
	}

	@Override
	public boolean isRelatedPathsOnly() {
		return hideUnrelatedItem.getSelection();
	}

	@Override
	public boolean isPending() {
		return pending;
	}

	public void addFilter(ILogEntryFilter filter) {
		logEntriesFilter.addFilter(filter);
	}

	public void removeFilter(ILogEntryFilter filter) {
		logEntriesFilter.removeFilter(filter);
	}

	@Override
	public void createControlsImpl(Composite parent) {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();

		GridData data;
		GridLayout layout;

		Composite labelAndToolbar = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 2;
		labelAndToolbar.setLayout(layout);

		data = new GridData(GridData.FILL_HORIZONTAL);
		labelAndToolbar.setLayoutData(data);

		//Create resource image and label
		Composite resourceLabelComposite = new Composite(labelAndToolbar, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		resourceLabelComposite.setLayout(layout);
		resourceLabelComposite.setLayoutData(data);

		resourceLabel = new Text(resourceLabelComposite, SWT.LEFT);
		resourceLabel.setEditable(false);
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		resourceLabel.setLayoutData(data);

		// Create the toolbar
		ToolBarManager toolBarMgr = new ToolBarManager(SWT.FLAT);
		final ToolBar toolBar = toolBarMgr.createControl(labelAndToolbar);
		data = new GridData();
		data.horizontalAlignment = GridData.END;
		toolBar.setLayoutData(data);

		refreshItem = new ToolItem(toolBar, SWT.FLAT);
		new ToolItem(toolBar, SWT.SEPARATOR);
		groupByDateItem = new ToolItem(toolBar, SWT.FLAT | SWT.CHECK);
		new ToolItem(toolBar, SWT.SEPARATOR);
		hideUnrelatedItem = new ToolItem(toolBar, SWT.FLAT | SWT.CHECK);
		stopOnCopyItem = new ToolItem(toolBar, SWT.FLAT | SWT.CHECK);
		new ToolItem(toolBar, SWT.SEPARATOR);
		filterItem = new ToolItem(toolBar, SWT.FLAT);
		clearFilterItem = new ToolItem(toolBar, SWT.FLAT);
		new ToolItem(toolBar, SWT.SEPARATOR);
		pagingItem = new ToolItem(toolBar, SWT.FLAT);
		pagingAllItem = new ToolItem(toolBar, SWT.FLAT);

		groupByDateItem.setImage(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/group_by_date.gif").createImage()); //$NON-NLS-1$
		hideUnrelatedItem.setImage(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/hide_unrelated.gif").createImage()); //$NON-NLS-1$
		stopOnCopyItem.setImage(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/stop_on_copy.gif").createImage()); //$NON-NLS-1$
		stopOnCopyItem.setSelection(initialStopOnCopy);
		filterItem.setImage(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/filter.gif").createImage()); //$NON-NLS-1$
		clearFilterItem.setDisabledImage(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/clear.gif").createImage()); //$NON-NLS-1$
		clearFilterItem.setImage(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/clear_filter.gif").createImage()); //$NON-NLS-1$
		pagingItem.setImage(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/paging.gif").createImage()); //$NON-NLS-1$
		pagingAllItem.setImage(
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/paging_all.gif").createImage()); //$NON-NLS-1$
		refreshItem.setImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/refresh.gif").createImage()); //$NON-NLS-1$

		groupByDateItem.setToolTipText(SVNUIMessages.SVNHistoryPanel_GroupByDate);
		hideUnrelatedItem.setToolTipText(SVNUIMessages.SVNHistoryPanel_Unrelated);
		stopOnCopyItem.setToolTipText(SVNUIMessages.SVNHistoryPanel_StopOnCopy);
		filterItem.setToolTipText(SVNUIMessages.SVNHistoryPanel_QuickFilter);
		clearFilterItem.setToolTipText(SVNUIMessages.SVNHistoryPanel_ClearFilter);
		pagingAllItem.setToolTipText(SVNUIMessages.SVNHistoryPanel_ShowAll);
		refreshItem.setToolTipText(SVNUIMessages.SVNHistoryPanel_Refresh);

		Composite group = new Composite(parent, SWT.BORDER);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 1;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_BOTH));

		history = new LogMessagesComposite(group, multiSelect, useCheckboxes, this);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 350;
		history.setLayoutData(data);
		history.setFocus();

		initTableViewerListener();

		int type = SVNTeamPreferences.getHistoryInt(store, SVNTeamPreferences.HISTORY_GROUPING_TYPE_NAME);

		if (SVNTeamPreferences.getHistoryBoolean(store, SVNTeamPreferences.HISTORY_PAGING_ENABLE_NAME)) {
			limit = SVNTeamPreferences.getHistoryInt(store, SVNTeamPreferences.HISTORY_PAGE_SIZE_NAME);
			String msg = BaseMessages.format(SVNUIMessages.SVNHistoryPanel_ShowNextX,
					new String[] { String.valueOf(limit) });
			pagingItem.setToolTipText(msg);
			pagingEnabled = true;
		} else {
			limit = 0;
			pagingItem.setToolTipText(SVNUIMessages.SVNHistoryPanel_ShowNextPage);
			pagingEnabled = false;
		}
		pagingEnabled = SVNHistoryPanel.this.limit > 0 && logMessages.length == SVNHistoryPanel.this.limit;

		setPagingEnabled();
		clearFilterItem.setEnabled(false);

		groupByDateItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				history.refresh(LogMessagesComposite.REFRESH_UI_ALL);
			}
		});
		groupByDateItem.setSelection(type == SVNTeamPreferences.HISTORY_GROUPING_TYPE_DATE);

		hideUnrelatedItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				history.refresh(LogMessagesComposite.REFRESH_UI_AFFECTED);
			}
		});
		stopOnCopyItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNHistoryPanel.this.refresh();
			}
		});

		filterItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNHistoryPanel.this.setFilter();
			}
		});
		clearFilterItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNHistoryPanel.this.clearFilter();
			}
		});

		pagingItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNHistoryPanel.this.showNextPage(false);
			}
		});
		pagingAllItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNHistoryPanel.this.showNextPage(true);
			}
		});

		refreshItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SVNHistoryPanel.this.refresh();
			}
		});

		pending = false;
		history.refresh(LogMessagesComposite.REFRESH_ALL);
		TreeViewer treeTable = SVNHistoryPanel.this.history.getTreeViewer();
		TreeItem firstItem = treeTable.getTree().getItem(0);
		if (((ILogNode) firstItem.getData()).getType() == ILogNode.TYPE_CATEGORY) {
			firstItem = firstItem.getItem(0);
		}
		treeTable.getTree().setSelection(firstItem);
		history.refresh(LogMessagesComposite.REFRESH_UI_AFFECTED);

		showResourceLabel();
	}

	/*
	 * Can be overridden in subclasses to initialize table viewer listener
	 */
	protected void initTableViewerListener() {

	}

	protected void showResourceLabel() {
		String resourceName = SVNUIMessages.SVNHistoryPanel_NotSelected;
		if (resource != null) {
			resourceName = resource.getUrl();
		}
		resourceLabel.setText(resourceName);
	}

	@Override
	protected void saveChangesImpl() {
	}

	@Override
	protected void cancelChangesImpl() {
	}

	protected void addPage(SVNLogEntry[] newMessages) {
		if (logMessages == null) {
			pending = false;
			logMessages = newMessages.length > 0 ? newMessages : null;
			pagingEnabled = limit > 0 && newMessages.length == limit;
		} else if (newMessages.length > 1) {
			LinkedHashSet<SVNLogEntry> entries = new LinkedHashSet<>(Arrays.asList(logMessages));
			int oldSize = entries.size();
			entries.addAll(Arrays.asList(newMessages));
			logMessages = entries.toArray(new SVNLogEntry[entries.size()]);
			pagingEnabled = limit > 0
					&& (newMessages.length == limit + 1 || entries.size() - oldSize < newMessages.length - 1);
		}
	}

	protected void setPagingEnabled() {
		pagingEnabled &= resource != null && limit > 0;
		pagingItem.setEnabled(pagingEnabled);
		pagingAllItem.setEnabled(pagingEnabled);
		filterItem.setEnabled(resource != null && logMessages != null);
		clearFilterItem.setEnabled(isFilterEnabled() && logMessages != null);
	}

	protected void fetchHistory(final GetLogMessagesOperation msgsOp) {
		msgsOp.setIncludeMerged(SVNTeamPreferences.getMergeBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.MERGE_INCLUDE_MERGED_NAME));

		final IStructuredSelection selected = (IStructuredSelection) history.getTreeViewer().getSelection();
		IActionOperation showOp = new AbstractActionOperation("Operation_ShowMessages", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				SVNTeamUIPlugin.instance().getWorkbench().getDisplay().syncExec(() -> {
					if (msgsOp.getExecutionState() != IActionOperation.OK) {
						pending = false;
						history.refresh(LogMessagesComposite.REFRESH_ALL);
						return;
					}
					SVNHistoryPanel.this.addPage(msgsOp.getMessages());
					history.refresh(LogMessagesComposite.REFRESH_ALL);
					SVNHistoryPanel.this.setPagingEnabled();

					TreeViewer treeTable = history.getTreeViewer();
					if (!treeTable.getTree().isDisposed() && treeTable.getTree().getItems().length > 0) {
						if (selected.size() != 0) {
							treeTable.setSelection(selected);
						} else {
							TreeItem firstItem = treeTable.getTree().getItem(0);
							if (((ILogNode) firstItem.getData()).getType() == ILogNode.TYPE_CATEGORY) {
								firstItem = firstItem.getItem(0);
							}
							treeTable.getTree().setSelection(firstItem);
						}
						history.refresh(LogMessagesComposite.REFRESH_UI_AFFECTED);
						if (tableViewerListener != null) {
							tableViewerListener.selectionChanged(null);
						}
					}
				});
			}
		};
		CompositeOperation op = new CompositeOperation(showOp.getId(), showOp.getMessagesClass());
		op.add(msgsOp);
		op.add(showOp);
		UIMonitorUtility.doTaskNowDefault(op, true);
	}

	protected void showNextPage(boolean showAll) {
		if (resource != null) {
			final GetLogMessagesOperation msgsOp = new GetLogMessagesOperation(resource, stopOnCopyItem.getSelection());
			msgsOp.setLimit(showAll ? 0 : logMessages == null ? limit : limit + 1);
			SVNRevision revision = resource.getSelectedRevision();
			if (logMessages != null && logMessages.length > 1) {
				SVNLogEntry lm = logMessages[logMessages.length - 1];
				revision = SVNRevision.fromNumber(lm.revision);
			}
			msgsOp.setStartRevision(revision);
			fetchHistory(msgsOp);
		}
	}

	protected void setFilter() {
		HistoryFilterPanel panel = new HistoryFilterPanel(authorFilter.getAuthorNameToAccept(),
				commentFilter.getCommentToAccept(), changeFilter.getGangedPathToAccept(),
				SVNHistoryPage.getSelectedAuthors(logMessages));
		DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getDisplay().getActiveShell(), panel);
		if (dialog.open() == 0) {
			authorFilter.setAuthorNameToAccept(panel.getAuthor());
			commentFilter.setCommentToAccept(panel.getComment());
			clearFilterItem.setEnabled(isFilterEnabled());
			SVNHistoryPanel.this.history.refresh(LogMessagesComposite.REFRESH_ALL);
		}
	}

	protected void clearFilter() {
		authorFilter.setAuthorNameToAccept(null);
		commentFilter.setCommentToAccept(null);
		changeFilter.setGangedPathToAccept(null);
		clearFilterItem.setEnabled(false);
		SVNHistoryPanel.this.history.refresh(LogMessagesComposite.REFRESH_ALL);
	}

	protected boolean isFilterEnabled() {
		return authorFilter.getAuthorNameToAccept() != null || commentFilter.getCommentToAccept() != null
				|| changeFilter.getGangedPathToAccept() != null;
	}

	protected void refresh() {
		long revision = history.getSelectedRevision();
		pagingEnabled = true;
		logMessages = null;
		pending = true;
		history.refresh(LogMessagesComposite.REFRESH_ALL);
		showNextPage(false);
		history.setSelectedRevision(revision);
	}
}
