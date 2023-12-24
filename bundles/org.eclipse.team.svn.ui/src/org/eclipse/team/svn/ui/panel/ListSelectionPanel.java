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

package org.eclipse.team.svn.ui.panel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;

/**
 * List selection panel implementation
 * 
 * @author Alexander Gurov
 */
public class ListSelectionPanel extends AbstractDialogPanel {
	protected Object inputElement;

	protected IBaseLabelProvider labelProvider;

	protected IStructuredContentProvider contentProvider;

	protected List initialSelections;

	protected List initialGrayed;

	protected Object[] resultSelections;

	protected boolean multipleColumns;

	protected CheckboxTableViewer listViewer;

	public ListSelectionPanel(Object input, IStructuredContentProvider contentProvider,
			IBaseLabelProvider labelProvider, String description, String defaultMessage) {
		this(input, contentProvider, labelProvider, description, defaultMessage, SVNUIMessages.ListSelectionPanel_Title,
				false);
	}

	public ListSelectionPanel(Object input, IStructuredContentProvider contentProvider,
			IBaseLabelProvider labelProvider, String description, String defaultMessage, String title,
			boolean multipleColumns) {
		initialSelections = Collections.EMPTY_LIST;
		initialGrayed = Collections.EMPTY_LIST;
		dialogTitle = title;
		inputElement = input;
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;
		dialogDescription = description;
		this.defaultMessage = defaultMessage;
		this.multipleColumns = multipleColumns;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		listViewer = createViewer(parent);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 120;
		listViewer.getTable().setLayoutData(data);
		listViewer.setLabelProvider(labelProvider);
		listViewer.setContentProvider(contentProvider);
		listViewer.addSelectionChangedListener(event -> ListSelectionPanel.this.validateContent());

		Composite tComposite = new Composite(parent, SWT.RIGHT);
		GridLayout gLayout = new GridLayout();
		gLayout.numColumns = 2;
		gLayout.marginWidth = 0;
		tComposite.setLayout(gLayout);
		data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		tComposite.setData(data);

		Button selectButton = new Button(tComposite, SWT.PUSH);
		selectButton.setText(SVNUIMessages.Button_SelectAll);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(selectButton);
		selectButton.setLayoutData(data);
		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(true);
				ListSelectionPanel.this.validateContent();
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
				listViewer.setAllChecked(false);
				ListSelectionPanel.this.validateContent();
			}
		};
		deselectButton.addSelectionListener(listener);
	}

	@Override
	public void postInit() {
		listViewer.setInput(inputElement);
		for (Iterator it = initialSelections.iterator(); it.hasNext();) {
			listViewer.setChecked(it.next(), true);
		}
		for (Iterator it = initialGrayed.iterator(); it.hasNext();) {
			listViewer.setGrayed(it.next(), true);
		}
		super.postInit();
	}

	public Object[] getResultSelections() {
		return resultSelections;
	}

	public Object[] getInitialSelections() {
		return initialSelections.toArray();
	}

	public void setInitialSelections(Object[] selectedElements) {
		initialSelections = Arrays.asList(selectedElements);
	}

	public void setInitialGrayed(Object[] grayedElements) {
		initialGrayed = Arrays.asList(grayedElements);
	}

	protected CheckboxTableViewer createViewer(Composite parent) {
		if (!multipleColumns) {
			return CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		}
		Table table = new Table(parent, SWT.CHECK | SWT.BORDER);
		table.setLinesVisible(true);
		GridData data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);

		TableLayout layout = new TableLayout();
		table.setLayout(layout);

		// resource name
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.ListSelectionPanel_Resource);
		layout.addColumnData(new ColumnWeightData(60, true));

		// local presentation
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.ListSelectionPanel_LocalPresentation);
		layout.addColumnData(new ColumnWeightData(40, true));

		return new CheckboxTableViewer(table);
	}

	@Override
	protected void saveChangesImpl() {
		Object[] children = contentProvider.getElements(inputElement);
		if (children != null) {
			ArrayList list = new ArrayList();
			for (Object child : children) {
				if (listViewer.getChecked(child)) {
					list.add(child);
				}
			}
			resultSelections = list.toArray();
		}
	}

	@Override
	protected void cancelChangesImpl() {
	}

}
