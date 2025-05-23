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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.utility;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Abstract columned viewer comparator
 * 
 * @author Alexei Goncharov
 */
public abstract class ColumnedViewerComparator extends ViewerComparator implements SelectionListener {
	protected int column;

	protected boolean reversed;

	protected Viewer basedOn;

	protected IPropertyChangeListener configurationListener;

	public static boolean CASE_INSENSITIVE = true;

	public ColumnedViewerComparator(Viewer basedOn) {
		this.basedOn = basedOn;
		reversed = false;
		column = 0;
		final IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		ColumnedViewerComparator.CASE_INSENSITIVE = SVNTeamPreferences.getBehaviourBoolean(store,
				SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_NAME);
		configurationListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty()
						.equals(SVNTeamPreferences
								.fullBehaviourName(SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_NAME))) {
					ColumnedViewerComparator.CASE_INSENSITIVE = SVNTeamPreferences.getBehaviourBoolean(store,
							SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_NAME);
					if (!ColumnedViewerComparator.this.basedOn.getControl().isDisposed()) {
						ColumnedViewerComparator.this.basedOn.refresh();
					} else {
						store.removePropertyChangeListener(this);
					}
				}
			}
		};
		store.addPropertyChangeListener(configurationListener);

	}

	public boolean isReversed() {
		return reversed;
	}

	public void setReversed(boolean reversed) {
		this.reversed = reversed;
	}

	public int getColumnNumber() {
		return column;
	}

	public void setColumnNumber(int column) {
		this.column = column;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (basedOn instanceof TreeViewer) {
			TreeViewer treeViewer = (TreeViewer) basedOn;
			int column = treeViewer.getTree().indexOf((TreeColumn) e.widget);
			ColumnedViewerComparator oldSorter = (ColumnedViewerComparator) treeViewer.getComparator();
			TreeColumn treeColumn = (TreeColumn) e.widget;
			if (oldSorter == null) {
				return;
			}
			if (column == oldSorter.getColumnNumber()) {
				oldSorter.setReversed(!oldSorter.isReversed());
				treeViewer.getTree().setSortColumn(treeColumn);
				treeViewer.getTree().setSortDirection(oldSorter.isReversed() ? SWT.DOWN : SWT.UP);
				treeViewer.refresh();
			} else {
				oldSorter.setColumnNumber(column);
				oldSorter.setReversed(false);
				treeViewer.getTree().setSortColumn(treeColumn);
				treeViewer.getTree().setSortDirection(SWT.UP);
				treeViewer.refresh();
			}
		} else if (basedOn instanceof TableViewer) {
			TableViewer tableViewer = (TableViewer) basedOn;
			int column = tableViewer.getTable().indexOf((TableColumn) e.widget);
			ColumnedViewerComparator oldSorter = (ColumnedViewerComparator) tableViewer.getComparator();
			TableColumn tableColumn = (TableColumn) e.widget;
			if (oldSorter == null) {
				return;
			}
			if (column == oldSorter.getColumnNumber()) {
				oldSorter.setReversed(!oldSorter.isReversed());
				tableViewer.getTable().setSortColumn(tableColumn);
				tableViewer.getTable().setSortDirection(oldSorter.isReversed() ? SWT.DOWN : SWT.UP);
				tableViewer.refresh();
			} else {
				oldSorter.setColumnNumber(column);
				oldSorter.setReversed(false);
				tableViewer.getTable().setSortColumn(tableColumn);
				tableViewer.getTable().setSortDirection(SWT.UP);
				tableViewer.refresh();
			}
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}

	@Override
	public final int compare(Viewer viewer, Object row1, Object row2) {
		return compareImpl(viewer, reversed ? row2 : row1, reversed ? row1 : row2);
	}

	public abstract int compareImpl(Viewer viewer, Object row1, Object row2);

	public static int compare(String first, String second) {
		first = first == null ? "" : first; //$NON-NLS-1$
		second = second == null ? "" : second; //$NON-NLS-1$
		return ColumnedViewerComparator.CASE_INSENSITIVE ? first.compareToIgnoreCase(second) : first.compareTo(second);
	}

}
