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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.history.data.AffectedPathsNode;
import org.eclipse.team.svn.ui.history.data.SVNChangedPathData;
import org.eclipse.team.svn.ui.utility.ArrayStructuredContentProvider;
import org.eclipse.team.svn.ui.utility.ColumnedViewerComparator;
import org.eclipse.team.svn.ui.utility.OverlayedImageDescriptor;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Affected paths composite, contains tree and table viewers of affected paths
 *
 * @author Sergiy Logvin
 */
public class AffectedPathsComposite extends Composite {
	public final static int COLUMN_ICON = 0;

	public final static int COLUMN_NAME = 1;

	public final static int COLUMN_PATH = 2;

	public final static int COLUMN_COPIED_FROM = 3;

	public final static int NUM_COLUMNS = 4;

	protected static ImageDescriptor ADDITION_OVERLAY;

	protected static ImageDescriptor MODIFICATION_OVERLAY;

	protected static ImageDescriptor DELETION_OVERLAY;

	protected static ImageDescriptor REPLACEMENT_OVERLAY;

	protected SashForm sashForm;

	protected TableViewer tableViewer;

	protected TreeViewer treeViewer;

	protected AffectedPathsLabelProvider labelProvider;

	public AffectedPathsComposite(Composite parent, int style) {
		super(parent, style);

		if (AffectedPathsComposite.ADDITION_OVERLAY == null) {
			AffectedPathsComposite.ADDITION_OVERLAY = SVNTeamUIPlugin.instance()
					.getImageDescriptor("icons/overlays/addition.gif"); //$NON-NLS-1$
			AffectedPathsComposite.MODIFICATION_OVERLAY = SVNTeamUIPlugin.instance()
					.getImageDescriptor("icons/overlays/change.gif"); //$NON-NLS-1$
			AffectedPathsComposite.DELETION_OVERLAY = SVNTeamUIPlugin.instance()
					.getImageDescriptor("icons/overlays/deletion.gif"); //$NON-NLS-1$
			AffectedPathsComposite.REPLACEMENT_OVERLAY = SVNTeamUIPlugin.instance()
					.getImageDescriptor("icons/overlays/replacement.gif"); //$NON-NLS-1$
		}

		createControls();
	}

	public void setResourceTreeVisible(boolean visible) {
		if (visible) {
			sashForm.setMaximizedControl(null);
		} else {
			sashForm.setMaximizedControl(tableViewer.getControl());
			AffectedPathsContentProvider provider = (AffectedPathsContentProvider) AffectedPathsComposite.this.treeViewer
					.getContentProvider();
			AffectedPathsNode rootNode = provider.getRoot();
			if (rootNode != null) {
				treeViewer.setSelection(new StructuredSelection(rootNode));
			}
		}
	}

	public void setInput(SVNChangedPathData[] input, Collection<String> relatedPathPrefixes,
			Collection<String> relatedParents, long currentRevision) {
		labelProvider.setCurrentRevision(currentRevision);
		AffectedPathsContentProvider provider = (AffectedPathsContentProvider) treeViewer.getContentProvider();
		provider.initialize(input, relatedPathPrefixes, relatedParents, currentRevision);
		if (input != null && (input.length > 0 || currentRevision == 0)) {
			treeViewer.setInput("Root");

			treeViewer.expandAll();
			treeViewer.setSelection(new StructuredSelection(provider.getRoot()));
			((Tree) treeViewer.getControl()).showSelection();
		} else {
			treeViewer.setInput(null);
		}
	}

	public void registerActionManager(HistoryActionManager manager, IWorkbenchPartSite site) {
		manager.affectedTableManager.installKeyBindings(tableViewer);
		manager.affectedTableManager.installDefaultAction(tableViewer);
		manager.affectedTableManager.installMenuActions(tableViewer, site);

		manager.affectedTreeManager.installKeyBindings(treeViewer);
		manager.affectedTreeManager.installDefaultAction(treeViewer);
		manager.affectedTreeManager.installMenuActions(treeViewer, site);
	}

	protected void createControls() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = gridLayout.marginWidth = 0;
		setLayout(gridLayout);

		sashForm = new SashForm(this, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer = new TreeViewer(sashForm, SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer.setContentProvider(new AffectedPathsContentProvider());
		treeViewer.setLabelProvider(labelProvider = new AffectedPathsLabelProvider());
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			protected AffectedPathsNode oldSelection;

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection tSelection = (IStructuredSelection) event.getSelection();
				if (tSelection.size() > 0) {
					AffectedPathsNode selection = (AffectedPathsNode) tSelection.getFirstElement();
					if (oldSelection != selection) {
						tableViewer.setInput(selection.getPathData());
						oldSelection = selection;
					}
				} else {
					tableViewer.setInput(null);
				}
			}
		});

		final Table table = new Table(sashForm, SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
		table.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent e) {
				TableItem item = table.getItem(new Point(e.x, e.y));
				if (item != null) {
					Rectangle rect = item.getBounds(0);
					String tooltip = ""; //$NON-NLS-1$
					if (rect.contains(e.x, e.y)) {
						SVNChangedPathData data = (SVNChangedPathData) item.getData();
						switch (data.action) {
							case ADDED: {
								tooltip = SVNUIMessages.LogMessagesComposite_Add;
								break;
							}
							case MODIFIED: {
								tooltip = SVNUIMessages.LogMessagesComposite_Modify;
								break;
							}
							case DELETED: {
								tooltip = SVNUIMessages.LogMessagesComposite_Delete;
								break;
							}
							case REPLACED: {
								tooltip = SVNUIMessages.LogMessagesComposite_Replace;
								break;
							}
						}
					}
					table.setToolTipText(rect.contains(e.x, e.y) ? tooltip : ""); //$NON-NLS-1$
				}
			}
		});

		TableLayout layout = new TableLayout();
		table.setLayout(layout);

		tableViewer = new TableViewer(table);
		sashForm.setWeights(new int[] { 25, 75 });

		AffectedPathsTableComparator tableComparator = new AffectedPathsTableComparator(tableViewer);

		//0.image
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText(""); //$NON-NLS-1$
		col.setResizable(false);
		col.setAlignment(SWT.CENTER);
		layout.addColumnData(new ColumnPixelData(26, false));

		//1.name
		col = new TableColumn(table, SWT.NONE);
		col.setText(SVNUIMessages.AffectedPathsComposite_Name);
		col.addSelectionListener(tableComparator);
		layout.addColumnData(new ColumnWeightData(20, true));

		//2.path
		col = new TableColumn(table, SWT.NONE);
		col.setText(SVNUIMessages.AffectedPathsComposite_Path);
		col.addSelectionListener(tableComparator);
		layout.addColumnData(new ColumnWeightData(40, true));

		//3.source path
		col = new TableColumn(table, SWT.NONE);
		col.setText(SVNUIMessages.AffectedPathsComposite_CopiedFrom);
		col.addSelectionListener(tableComparator);
		layout.addColumnData(new ColumnWeightData(40, true));

		tableComparator.setReversed(false);
		tableComparator.setColumnNumber(AffectedPathsComposite.COLUMN_PATH);
		tableViewer.setComparator(tableComparator);
		tableViewer.getTable().setSortColumn(tableViewer.getTable().getColumn(AffectedPathsComposite.COLUMN_PATH));
		tableViewer.getTable().setSortDirection(SWT.UP);

		tableViewer.setContentProvider(new ArrayStructuredContentProvider());
		tableViewer.setLabelProvider(new AffectedPathsTableLabelProvider());
	}

	protected class AffectedPathsTableLabelProvider implements ITableLabelProvider {
		protected Map<ImageDescriptor, Image> images = new HashMap<>();

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == AffectedPathsComposite.COLUMN_ICON) {
				String fileName = ((SVNChangedPathData) element).resourceName;
				ImageDescriptor descr = SVNTeamUIPlugin.instance()
						.getWorkbench()
						.getEditorRegistry()
						.getImageDescriptor(fileName);
				Image img = images.get(descr);
				if (img == null) {
					img = descr.createImage();
					images.put(descr, img);
				}
				switch (((SVNChangedPathData) element).action) {
					case ADDED: {
						descr = new OverlayedImageDescriptor(img, AffectedPathsComposite.ADDITION_OVERLAY,
								new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V);
						break;
					}
					case MODIFIED: {
						descr = new OverlayedImageDescriptor(img, AffectedPathsComposite.MODIFICATION_OVERLAY,
								new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V);
						break;
					}
					case DELETED: {
						descr = new OverlayedImageDescriptor(img, AffectedPathsComposite.DELETION_OVERLAY,
								new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V);
						break;
					}
					case REPLACED: {
						descr = new OverlayedImageDescriptor(img, AffectedPathsComposite.REPLACEMENT_OVERLAY,
								new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V);
						break;
					}
				}
				img = images.get(descr);
				if (img == null) {
					img = descr.createImage();
					images.put(descr, img);
				}
				return img;
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			SVNChangedPathData data = (SVNChangedPathData) element;
			switch (columnIndex) {
				case AffectedPathsComposite.COLUMN_NAME: {
					return data.resourceName;
				}
				case AffectedPathsComposite.COLUMN_PATH: {
					return data.resourcePath;
				}
				case AffectedPathsComposite.COLUMN_COPIED_FROM: {
					return data.copiedFromPath + (data.copiedFromRevision == SVNRevision.INVALID_REVISION_NUMBER
							? "" //$NON-NLS-1$
							: '@' + String.valueOf(data.copiedFromRevision));
				}
			}
			return ""; //$NON-NLS-1$
		}

		@Override
		public void dispose() {
			for (Image img : images.values()) {
				img.dispose();
			}
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

	}

	protected class AffectedPathsTableComparator extends ColumnedViewerComparator {
		public AffectedPathsTableComparator(Viewer tableViewer) {
			super(tableViewer);
		}

		@Override
		public int compareImpl(Viewer viewer, Object row1, Object row2) {
			SVNChangedPathData data1 = (SVNChangedPathData) row1;
			SVNChangedPathData data2 = (SVNChangedPathData) row2;
			switch (column) {
				case AffectedPathsComposite.COLUMN_NAME: {
					return ColumnedViewerComparator.compare(data1.resourceName, data2.resourceName);
				}
				case AffectedPathsComposite.COLUMN_PATH: {
					return ColumnedViewerComparator.compare(data1.resourcePath, data2.resourcePath);
				}
				case AffectedPathsComposite.COLUMN_COPIED_FROM: {
					String copied1 = data1.copiedFromPath
							+ (data1.copiedFromRevision == SVNRevision.INVALID_REVISION_NUMBER
									? "" //$NON-NLS-1$
									: '@' + String.valueOf(data1.copiedFromRevision));
					String copied2 = data2.copiedFromPath
							+ (data2.copiedFromRevision == SVNRevision.INVALID_REVISION_NUMBER
									? "" //$NON-NLS-1$
									: '@' + String.valueOf(data2.copiedFromRevision));
					return ColumnedViewerComparator.compare(copied1, copied2);
				}
			}
			return 0;
		}

	}

}
