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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.lock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.utility.ArrayStructuredContentProvider;

/**
 * Lock resource selection composite
 * 
 * Shows resources for lock operations: break lock, unlock, lock/steal lock
 * 
 * @author Igor Burilo
 */
public class LockResourceSelectionComposite extends Composite {

	public final static int COLUMN_NAME = 1;

	public final static int COLUMN_PATH = 2;

	public final static int COLUMN_STATE = 3;

	public final static int COLUMN_OWNER = 4;

	public final static int COLUMN_DATE = 5;

	protected LockResource[] resources;

	protected boolean showCheckBoxesAndButtons;

	protected boolean hasBorder;

	protected CheckboxTableViewer tableViewer;

	protected Label lblSelectedResourcesNumber;

	protected ISelectionChangedListener selectionListener;

	protected List<ILockResourceSelectionChangeListener> selectionChangedListeners;

	protected LockResource[] selectedResources;

	protected LockResource[] notSelectedResources;

	public interface ILockResourceSelectionChangeListener {
		void resourcesSelectionChanged(LockResourceSelectionChangedEvent event);
	}

	public static class LockResourceSelectionChangedEvent {
		public final LockResource[] checkedResources;

		public final IStructuredSelection selection;

		public LockResourceSelectionChangedEvent(LockResource[] resources, ISelection selection) {
			checkedResources = resources;
			this.selection = (IStructuredSelection) selection;
		}
	}

	public LockResourceSelectionComposite(Composite parent, int style, boolean hasBorder,
			boolean showCheckBoxesAndButtons) {
		super(parent, style);
		this.showCheckBoxesAndButtons = showCheckBoxesAndButtons;
		this.hasBorder = hasBorder;

		resources = new LockResource[0];
		selectedResources = new LockResource[0];
		notSelectedResources = new LockResource[0];

		selectionChangedListeners = new ArrayList<>();

		createControls();
	}

	protected void createControls() {
		GridLayout gridLayout = null;
		GridData data = null;

		gridLayout = new GridLayout();
		gridLayout.marginHeight = gridLayout.marginWidth = 0;
		setLayout(gridLayout);

		int style = SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI;
		if (hasBorder) {
			style |= SWT.BORDER;
		}
		Table table = new Table(this, showCheckBoxesAndButtons ? style | SWT.CHECK : style);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableLayout layout = new TableLayout();
		table.setLayout(layout);

		tableViewer = new CheckboxTableViewer(table);
		data = new GridData(GridData.FILL_BOTH);
		tableViewer.getTable().setLayoutData(data);

		LockResourcesTableComparator tableComparator = new LockResourcesTableComparator(tableViewer);

		if (showCheckBoxesAndButtons) {
			//0.checkbox
			TableColumn col = new TableColumn(table, SWT.NONE);
			col.setResizable(false);
			layout.addColumnData(new ColumnPixelData(20, false));
		} else {
			//0.image
			TableColumn col = new TableColumn(table, SWT.NONE);
			col.setText(""); //$NON-NLS-1$
			col.setResizable(false);
			col.setAlignment(SWT.CENTER);
			layout.addColumnData(new ColumnPixelData(20, false));
		}

		//1.name
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText(SVNUIMessages.LocksComposite_Name);
		col.addSelectionListener(tableComparator);
		layout.addColumnData(new ColumnWeightData(20, true));

		//2.path
		col = new TableColumn(table, SWT.NONE);
		col.setText(SVNUIMessages.LocksComposite_Path);
		col.addSelectionListener(tableComparator);
		layout.addColumnData(new ColumnWeightData(30, true));

		//3.state
		col = new TableColumn(table, SWT.NONE);
		col.setText(SVNUIMessages.LocksComposite_State);
		col.addSelectionListener(tableComparator);
		layout.addColumnData(new ColumnWeightData(20, true));

		//4.owner
		col = new TableColumn(table, SWT.NONE);
		col.setText(SVNUIMessages.LocksComposite_Owner);
		col.addSelectionListener(tableComparator);
		layout.addColumnData(new ColumnWeightData(20, true));

		//5.date
		col = new TableColumn(table, SWT.NONE);
		col.setText(SVNUIMessages.LocksComposite_CreationDate);
		col.addSelectionListener(tableComparator);
		layout.addColumnData(new ColumnWeightData(10, true));

		tableComparator.setReversed(false);
		tableComparator.setColumnNumber(LockResourceSelectionComposite.COLUMN_PATH);
		tableViewer.setComparator(tableComparator);
		tableViewer.getTable()
				.setSortColumn(tableViewer.getTable().getColumn(LockResourceSelectionComposite.COLUMN_PATH));
		tableViewer.getTable().setSortDirection(SWT.UP);

		tableViewer.setContentProvider(new ArrayStructuredContentProvider());
		tableViewer.setLabelProvider(new LockResourcesTableLabelProvider(showCheckBoxesAndButtons));

		tableViewer.addSelectionChangedListener(selectionListener = event -> {
			if (showCheckBoxesAndButtons) {
				LockResourceSelectionComposite.this.updateSelectedResources();
				int selectedNumber = selectedResources.length;
				lblSelectedResourcesNumber
						.setText(LockResourceSelectionComposite.this.resourceNumberToString(selectedNumber));
			}
			LockResourceSelectionComposite.this.fireResourcesSelectionChanged(
					new LockResourceSelectionChangedEvent(selectedResources,
							event != null ? event.getSelection() : null));
		});

		if (!showCheckBoxesAndButtons) {
			return;
		}

		Composite tComposite = new Composite(this, SWT.RIGHT);
		GridLayout gLayout = new GridLayout();
		gLayout.numColumns = 3;
		gLayout.marginWidth = 0;
		tComposite.setLayout(gLayout);
		data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		tComposite.setLayoutData(data);

		Button selectButton = new Button(tComposite, SWT.PUSH);
		selectButton.setText(SVNUIMessages.Button_SelectAll);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(selectButton);
		selectButton.setLayoutData(data);
		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setAllChecked(true);
				Object[] elements = tableViewer.getCheckedElements();
				selectionListener.selectionChanged(null);
				LockResourceSelectionComposite.this.fireResourcesSelectionChanged(new LockResourceSelectionChangedEvent(
						Arrays.asList(elements).toArray(new LockResource[elements.length]), null));
			}
		};
		selectButton.addSelectionListener(listener);

		Button deselectButton = new Button(tComposite, SWT.PUSH);
		deselectButton.setText(SVNUIMessages.Button_ClearSelection);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(deselectButton);
		deselectButton.setLayoutData(data);
		listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setAllChecked(false);
				selectionListener.selectionChanged(null);
				LockResourceSelectionComposite.this.fireResourcesSelectionChanged(
						new LockResourceSelectionChangedEvent(new LockResource[0], null));
			}
		};
		deselectButton.addSelectionListener(listener);

		Composite lComposite = new Composite(tComposite, SWT.NONE);
		GridLayout lLayout = new GridLayout();
		lLayout.horizontalSpacing = 0;
		lLayout.marginWidth = 0;
		lComposite.setLayout(lLayout);
		data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		lComposite.setLayoutData(data);

		lblSelectedResourcesNumber = new Label(lComposite, SWT.RIGHT);
		lblSelectedResourcesNumber.setText(resourceNumberToString(selectedResources.length));
		data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		lblSelectedResourcesNumber.setLayoutData(data);
	}

	protected String resourceNumberToString(int value) {
		return BaseMessages.format(SVNUIMessages.ResourceSelectionComposite_Info,
				new String[] { String.valueOf(value), String.valueOf(resources.length) });
	}

	public void setInput(LockResource[] resources) {
		selectedResources = this.resources = resources;
		tableViewer.setInput(resources);
		if (this.resources != null) {
			for (LockResource element : this.resources) {
				tableViewer.setChecked(element, true);
			}

			if (showCheckBoxesAndButtons) {
				lblSelectedResourcesNumber.setText(resourceNumberToString(selectedResources.length));
			}
		}

		//TODO select first row in View
//		if (!this.showCheckBoxesAndButtons) {
//			Table table = this.tableViewer.getTable();
//			TableItem firstItem = table.getItem(0);
//
//			//TODO it doesn't work when change selection outside, e.g. from Package Explorer
//			table.setSelection(firstItem);
//			((Table)this.tableViewer.getControl()).showSelection();
//		}

		if (showCheckBoxesAndButtons) {
			updateSelectedResources();
			selectionListener.selectionChanged(null);
		}
		tableViewer.refresh();
	}

	protected void updateSelectedResources() {
		TableItem[] items = tableViewer.getTable().getItems();
		List<LockResource> checked = new ArrayList<>(items.length);
		List<LockResource> unchecked = new ArrayList<>();
		for (TableItem item : items) {
			(item.getChecked() ? checked : unchecked).add((LockResource) item.getData());
		}
		selectedResources = checked.toArray(new LockResource[checked.size()]);
		notSelectedResources = unchecked.toArray(new LockResource[unchecked.size()]);
	}

	public TableViewer getTableViewer() {
		return tableViewer;
	}

	public LockResource[] getSelectedResources() {
		return selectedResources;
	}

	public LockResource[] getNotSelectedResources() {
		return notSelectedResources;
	}

	public void addResourcesSelectionChangedListener(ILockResourceSelectionChangeListener listener) {
		selectionChangedListeners.add(listener);
	}

	public void removeResourcesSelectionChangedListener(ILockResourceSelectionChangeListener listener) {
		selectionChangedListeners.remove(listener);
	}

	public void fireResourcesSelectionChanged(LockResourceSelectionChangedEvent event) {
		ILockResourceSelectionChangeListener[] listeners = selectionChangedListeners
				.toArray(new ILockResourceSelectionChangeListener[selectionChangedListeners.size()]);
		for (ILockResourceSelectionChangeListener listener : listeners) {
			listener.resourcesSelectionChanged(event);
		}
	}

	public void setMenuManager(MenuManager menuMgr) {
		Menu menu = menuMgr.createContextMenu(tableViewer.getTable());
		tableViewer.getTable().setMenu(menu);
	}
}
