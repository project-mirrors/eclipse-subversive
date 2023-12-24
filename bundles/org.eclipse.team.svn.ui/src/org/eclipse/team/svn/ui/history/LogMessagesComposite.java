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
 *    Gabor Liptak - Speedup Pattern's usage
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.extension.factory.ICommentView;
import org.eclipse.team.svn.ui.history.data.RootHistoryCategory;
import org.eclipse.team.svn.ui.history.data.SVNChangedPathData;
import org.eclipse.team.svn.ui.history.model.CategoryLogNode;
import org.eclipse.team.svn.ui.history.model.ILogNode;
import org.eclipse.team.svn.ui.history.model.SVNLogNode;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.ColumnedViewerComparator;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;

/**
 * LogMessage's viewer implementation
 * 
 * @author Alexander Gurov
 */
public class LogMessagesComposite extends SashForm {
	public static final int REFRESH_UI_AFFECTED = 1;

	public static final int REFRESH_UI_ALL = 2;

	public static final int REFRESH_ALL = 3;

	protected SashForm innerSashForm;

	protected CheckboxTreeViewer historyTable;

	protected ISelectionChangedListener historyTableListener;

	protected Font currentRevisionFont;

	protected AffectedPathsComposite affectedPathsComposite;

	protected ICommentView commentViewManager;

	protected RootHistoryCategory rootCategory;

	protected HistoryActionManager actionManager;

	protected ISVNHistoryViewInfo info;

	protected boolean useCheckboxes;

	public LogMessagesComposite(Composite parent, boolean multiSelect, boolean useCheckboxes,
			ISVNHistoryViewInfo info) {
		super(parent, SWT.VERTICAL);

		this.info = info;
		this.useCheckboxes = useCheckboxes;
		rootCategory = new RootHistoryCategory(info);

		initializeFont();
		initializeTableView(multiSelect ? SWT.MULTI : SWT.SINGLE);
	}

	public CheckboxTreeViewer getTreeViewer() {
		return historyTable;
	}

	public void setCommentViewerVisible(boolean visible) {
		if (visible) {
			innerSashForm.setMaximizedControl(null);
		} else {
			innerSashForm.setMaximizedControl(historyTable.getControl());
		}
	}

	public void setAffectedPathsViewerVisible(boolean visible) {
		if (visible) {
			setMaximizedControl(null);
		} else {
			setMaximizedControl(innerSashForm);
		}
	}

	public void setResourceTreeVisible(boolean visible) {
		affectedPathsComposite.setResourceTreeVisible(visible);
	}

	public void collapseAll() {
		historyTable.collapseAll();
	}

	public void expandAll() {
		historyTable.expandAll();
	}

	public void registerActionManager(HistoryActionManager manager, IWorkbenchPartSite site) {
		actionManager = manager;

		manager.logMessagesManager.installKeyBindings(historyTable);
		manager.logMessagesManager.installDefaultAction(historyTable);
		manager.logMessagesManager.installMenuActions(historyTable, site);

		affectedPathsComposite.registerActionManager(manager, site);
	}

	public void setSelectedRevision(long revision) {
		SVNLogEntry[] msgs = rootCategory.getRemoteHistory();
		if (msgs != null) {
			for (SVNLogEntry msg : msgs) {
				if (msg.revision == revision) {
					historyTable.setSelection(new StructuredSelection(new SVNLogNode(msg, null)), true);
					return;
				}
			}
		}
		historyTable.setSelection(new StructuredSelection());
	}

	public SVNLogEntry[] getSelectedLogMessages() {
		ArrayList<SVNLogEntry> entries = new ArrayList<>();
		if (useCheckboxes) {
			Object[] checked = historyTable.getCheckedElements();
			for (Object current : checked) {
				ILogNode node = (ILogNode) current;
				if (node.getType() == ILogNode.TYPE_SVN) {
					entries.add((SVNLogEntry) node.getEntity());
				}
			}
		} else {
			IStructuredSelection tSelection = (IStructuredSelection) historyTable.getSelection();
			for (Iterator it = tSelection.iterator(); it.hasNext();) {
				ILogNode node = (ILogNode) it.next();
				if (node.getType() == ILogNode.TYPE_SVN) {
					entries.add((SVNLogEntry) node.getEntity());
				}
			}
		}
		return entries.toArray(new SVNLogEntry[entries.size()]);
	}

	public long getSelectedRevision() {
		IStructuredSelection tSelection = (IStructuredSelection) historyTable.getSelection();
		if (tSelection.size() > 0) {
			ILogNode node = (ILogNode) tSelection.getFirstElement();
			if (node.getType() == ILogNode.TYPE_SVN) {
				return ((SVNLogEntry) node.getEntity()).revision;
			}
		}
		return SVNRevision.INVALID_REVISION_NUMBER;
	}

	public void refresh(int refreshType) {
		if (refreshType == LogMessagesComposite.REFRESH_ALL) {
			if (info.getResource() != null) {
				commentViewManager.usedFor(info.getResource());
			} else {
				commentViewManager.usedFor(info.getRepositoryResource());
			}

			rootCategory.refreshModel();
		}

		refreshImpl(refreshType);
	}

	@Override
	public void dispose() {
		historyTable.removeSelectionChangedListener(historyTableListener);
		if (historyTable.getContentProvider() != null) {
			historyTable.setInput(null);
		}
		currentRevisionFont.dispose();
		super.dispose();
	}

	protected void refreshImpl(int refreshType) {
		if (!historyTable.getTree().isDisposed()) {
			if (refreshType != LogMessagesComposite.REFRESH_UI_AFFECTED) {
				Object[] entries = rootCategory.getEntries();
				historyTable.getTree()
						.setLinesVisible(entries != RootHistoryCategory.NO_REMOTE
								&& entries != RootHistoryCategory.NO_LOCAL && entries != RootHistoryCategory.NO_REVS);
				historyTable.setInput(new CategoryLogNode(rootCategory));
			}
			historyTableListener.selectionChanged(null);
		}
	}

	private void initializeTableView(int style) {
		innerSashForm = new SashForm(this, SWT.VERTICAL);

		Tree treeTable = new Tree(innerSashForm,
				style | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | (useCheckboxes ? SWT.CHECK : SWT.NONE));
		treeTable.setHeaderVisible(true);
		treeTable.setLinesVisible(true);
		TableLayout layout = new TableLayout();
		treeTable.setLayout(layout);

		commentViewManager = ExtensionsManager.getInstance().getCurrentMessageFactory().getCommentView();
		commentViewManager.createCommentView(innerSashForm, SWT.V_SCROLL | SWT.MULTI | SWT.WRAP);

		affectedPathsComposite = new AffectedPathsComposite(this, SWT.NONE);

		innerSashForm.setWeights(new int[] { 75, 25 });
		setWeights(new int[] { 70, 30 });

		historyTable = new CheckboxTreeViewer(treeTable);

		//creating a comparator now to get listeners for columns
		HistoryTableComparator comparator = new HistoryTableComparator(historyTable);

		//revision
		TreeColumn col = new TreeColumn(treeTable, SWT.NONE);
		col.setResizable(true);
		col.setAlignment(SWT.RIGHT);
		col.setText(SVNUIMessages.LogMessagesComposite_Revision);
		col.addSelectionListener(comparator);
		layout.addColumnData(new ColumnWeightData(14, true));

		// creation date
		col = new TreeColumn(treeTable, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.LogMessagesComposite_Date);
		col.addSelectionListener(comparator);
		layout.addColumnData(new ColumnWeightData(15, true));

		//file count
		col = new TreeColumn(treeTable, SWT.NONE);
		col.setResizable(true);
		col.setAlignment(SWT.RIGHT);
		col.setText(SVNUIMessages.LogMessagesComposite_Changes);
		col.addSelectionListener(comparator);
		layout.addColumnData(new ColumnWeightData(7, true));

		// author
		col = new TreeColumn(treeTable, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.LogMessagesComposite_Author);
		col.addSelectionListener(comparator);
		layout.addColumnData(new ColumnWeightData(14, true));

		//comment
		col = new TreeColumn(treeTable, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.LogMessagesComposite_Comment);
		col.addSelectionListener(comparator);
		layout.addColumnData(new ColumnWeightData(50, true));

		//adding a comparator and initializing default sort column and direction
		historyTable.setComparator(comparator);
		comparator.setColumnNumber(ILogNode.COLUMN_DATE);
		comparator.setReversed(true);
		historyTable.getTree().setSortColumn(historyTable.getTree().getColumn(ILogNode.COLUMN_DATE));
		historyTable.getTree().setSortDirection(SWT.DOWN);

		historyTable.setContentProvider(new ITreeContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return inputElement == null ? new Object[0] : ((ILogNode) inputElement).getChildren();
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return ((ILogNode) parentElement).getChildren();
			}

			@Override
			public Object getParent(Object element) {
				return ((ILogNode) element).getParent();
			}

			@Override
			public boolean hasChildren(Object element) {
				return ((ILogNode) element).hasChildren();
			}

			@Override
			public void dispose() {
			}
		});
		historyTable.setLabelProvider(new HistoryLabelProvider());

		historyTableListener = new ISelectionChangedListener() {
			protected ILogNode oldSelection;

			protected boolean hideUnrelated = info.isRelatedPathsOnly();

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (LogMessagesComposite.this.isDisposed()) {
					return;
				}
				IStructuredSelection tSelection = (IStructuredSelection) historyTable.getSelection();
				if (tSelection.size() > 0) {
					ILogNode selection = (ILogNode) tSelection.getFirstElement();
					if (oldSelection != selection || hideUnrelated != info.isRelatedPathsOnly()) {
						String message = selection.getComment();
						if (message == null || message.length() == 0) {
							message = selection.getType() == ILogNode.TYPE_SVN ? SVNMessages.SVNInfo_NoComment : ""; //$NON-NLS-1$
						}
						commentViewManager.setComment(message);

						Collection<String> relatedPrefixes = info.isRelatedPathsOnly()
								? rootCategory.getRelatedPathPrefixes()
								: null;
						Collection<String> relatedParents = info.isRelatedPathsOnly()
								? rootCategory.getRelatedParents()
								: null;
						SVNChangedPathData[] pathData = rootCategory.getPathData(selection);
						long revision = LogMessagesComposite.this.getSelectedRevision();

						if (actionManager != null) {
							actionManager.setSelectedRevision(revision);
						}
						affectedPathsComposite.setInput(pathData, relatedPrefixes, relatedParents, revision);

						oldSelection = selection;
						hideUnrelated = info.isRelatedPathsOnly();
					}
				} else {
					commentViewManager.setComment(""); //$NON-NLS-1$
					affectedPathsComposite.setInput(null, null, null, -1);
					oldSelection = null;
				}
			}
		};
		historyTable.addSelectionChangedListener(historyTableListener);

		if (useCheckboxes) {
			historyTable.addCheckStateListener(event -> {
				boolean isChecked = event.getChecked();
				ILogNode checkedNode = (ILogNode) event.getElement();
				if (checkedNode.getType() == ILogNode.TYPE_CATEGORY) {
					for (ILogNode node : checkedNode.getChildren()) {
						historyTable.setChecked(node, isChecked);
					}
					historyTable.setGrayed(checkedNode, false);
				} else if (checkedNode.getType() == ILogNode.TYPE_SVN) {
					if (checkedNode.getParent().getType() == ILogNode.TYPE_SVN) {
						historyTable.setChecked(checkedNode, false);// could fail if many revisions with the same id are present
					} else {
						int uncheckedCount = 0;
						ILogNode parent = checkedNode.getParent();
						ILogNode[] children = parent.getChildren();
						for (ILogNode node : children) {
							if (!historyTable.getChecked(node)) {
								uncheckedCount++;
							}
						}
						if (uncheckedCount == children.length) {
							historyTable.setChecked(parent, false);
							historyTable.setGrayed(parent, false);
						} else {
							historyTable.setChecked(parent, true);
							historyTable.setGrayed(parent, uncheckedCount > 0);
						}
					}
				}
			});
		}
		historyTable.setAutoExpandLevel(2); //auto-expand all categories
		historyTable.setInput(new CategoryLogNode(rootCategory));
	}

	private void initializeFont() {
		Font defaultFont = JFaceResources.getDefaultFont();
		FontData[] data = defaultFont.getFontData();
		for (FontData element : data) {
			element.setStyle(SWT.BOLD);
		}
		currentRevisionFont = new Font(getDisplay(), data);
	}

	protected class HistoryLabelProvider implements ITableLabelProvider, IFontProvider, IColorProvider {
		private Color mergedRevisionsForeground;

		private Map<ImageDescriptor, Image> images;

		protected IPropertyChangeListener configurationListener;

		public HistoryLabelProvider() {
			images = new HashMap<>();
			configurationListener = event -> {
				if (event.getProperty().startsWith(SVNTeamPreferences.DECORATION_BASE)) {
					UIMonitorUtility.getDisplay().syncExec(() -> {
						HistoryLabelProvider.this.loadConfiguration();
						refresh(LogMessagesComposite.REFRESH_UI_ALL);
					});
				}
			};
			loadConfiguration();
			PlatformUI.getWorkbench()
					.getThemeManager()
					.getCurrentTheme()
					.addPropertyChangeListener(configurationListener);
		}

		protected void loadConfiguration() {
			if (mergedRevisionsForeground != null) {
				mergedRevisionsForeground.dispose();
			}
			ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			mergedRevisionsForeground = new Color(Display.getCurrent(),
					current.getColorRegistry()
							.get(SVNTeamPreferences
									.fullDecorationName(SVNTeamPreferences.NAME_OF_MERGED_REVISIONS_FOREGROUND_COLOR))
							.getRGB());
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				ILogNode node = (ILogNode) element;
				ImageDescriptor descr = node.getImageDescriptor();
				Image retVal = images.get(descr);
				if (descr != null && retVal == null) {
					images.put(descr, retVal = descr.createImage());
				}
				return retVal;
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			return ((ILogNode) element).getLabel(columnIndex, ILogNode.LABEL_TRIM, info.getCurrentRevision());
		}

		@Override
		public Font getFont(Object element) {
			return ((ILogNode) element).requiresBoldFont(info.getCurrentRevision()) ? currentRevisionFont : null;
		}

		@Override
		public void dispose() {
			PlatformUI.getWorkbench()
					.getThemeManager()
					.getCurrentTheme()
					.removePropertyChangeListener(configurationListener);
			for (Image img : images.values()) {
				img.dispose();
			}
			mergedRevisionsForeground.dispose();
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return true;
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			ILogNode node = (ILogNode) element;
			return node.getType() == ILogNode.TYPE_SVN && node.getParent() instanceof SVNLogNode
					? mergedRevisionsForeground
					: null;
		}

	}

	protected class HistoryTableComparator extends ColumnedViewerComparator {
		public HistoryTableComparator(Viewer treeViewer) {
			super(treeViewer);
		}

		@Override
		public int compareImpl(Viewer viewer, Object row1, Object row2) {
			ILogNode node1 = (ILogNode) row1;
			ILogNode node2 = (ILogNode) row2;
			switch (column) {
				case ILogNode.COLUMN_REVISION: {
					long rev1 = node1.getRevision();
					long rev2 = node2.getRevision();
					if (rev1 != SVNRevision.INVALID_REVISION_NUMBER && rev2 != SVNRevision.INVALID_REVISION_NUMBER) {
						return rev1 < rev2 ? -1 : rev1 > rev2 ? 1 : 0;
					}
				}
				case ILogNode.COLUMN_DATE: {
					return node1.getTimeStamp() < node2.getTimeStamp()
							? -1
							: node1.getTimeStamp() > node2.getTimeStamp() ? 1 : 0;
				}
				case ILogNode.COLUMN_CHANGES: {
					return node1.getChangesCount() < node2.getChangesCount()
							? -1
							: node1.getChangesCount() > node2.getChangesCount() ? 1 : 0;
				}
				case ILogNode.COLUMN_AUTHOR: {
					return ColumnedViewerComparator.compare(node1.getAuthor(), node2.getAuthor());
				}
				case ILogNode.COLUMN_COMMENT: {
					return ColumnedViewerComparator.compare(node1.getComment(), node2.getComment());
				}
			}
			return 0;
		}
	}

}
