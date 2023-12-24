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

package org.eclipse.team.svn.ui.panel;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.svn.ui.utility.ArrayStructuredContentProvider;

/**
 * Item List Panel implementation
 * 
 * @author Sergiy Logvin
 */
public class ItemListPanel extends AbstractDialogPanel {
	protected String[] items;

	protected Image itemIcon;

	protected Table table;

	public ItemListPanel(String[] items, ImageDescriptor imageDescriptor, String dialogTitle, String dialogDescription,
			String defaultMessage) {
		this(items, imageDescriptor, dialogTitle, dialogDescription, defaultMessage,
				new String[] { IDialogConstants.OK_LABEL });
	}

	public ItemListPanel(String[] items, ImageDescriptor imageDescriptor, String dialogTitle, String dialogDescription,
			String defaultMessage, String[] buttons) {
		super(buttons);
		this.items = items;
		this.dialogTitle = dialogTitle;
		this.dialogDescription = dialogDescription;
		this.defaultMessage = defaultMessage;
		itemIcon = imageDescriptor.createImage();
	}

	@Override
	protected void saveChangesImpl() {
	}

	@Override
	protected void cancelChangesImpl() {
	}

	@Override
	public void createControlsImpl(Composite parent) {
		table = new Table(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		TableLayout layout = new TableLayout();
		table.setLayout(layout);
		TableViewer viewer = new TableViewer(table);

		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setAlignment(SWT.CENTER);
		layout.addColumnData(new ColumnWeightData(0, 60, true));

		viewer.setContentProvider(new ArrayStructuredContentProvider());
		ITableLabelProvider labelProvider = new ITableLabelProvider() {
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return itemIcon;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				return element.toString();
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return true;
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
			}
		};
		viewer.setLabelProvider(labelProvider);
		viewer.setInput(items);
	}

}
