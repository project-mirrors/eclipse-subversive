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

package org.eclipse.team.svn.ui.composite;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.properties.RevPropertiesEditPanel;
import org.eclipse.team.svn.ui.repository.model.RepositoryPending;
import org.eclipse.team.svn.ui.utility.ColumnedViewerComparator;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Composite for viewing revision properties
 * 
 * @author Alexei Goncharov
 */
public class RevisionPropertiesComposite extends Composite {

	protected static final int COLUMN_NAME = 0;

	protected static final int COLUMN_VALUE = 1;

	protected SVNProperty[] properties;

	protected IRepositoryLocation location;

	protected SVNRevision revision;

	protected TableViewer propertyViewer;

	protected Text propertyText;

	protected boolean isPending;

	public RevisionPropertiesComposite(Composite parent) {
		super(parent, SWT.NONE);
		properties = new SVNProperty[0];
		createControls(parent);
	}

	public void setPending(final boolean isPending) {
		this.isPending = isPending;
		UIMonitorUtility.getDisplay().syncExec(() -> {
			if (isPending) {
				propertyViewer.getTable().setLinesVisible(false);
			} else {
				propertyViewer.getTable().setLinesVisible(true);
			}
			RevisionPropertiesComposite.this.rereshTableData();
		});
	}

	public synchronized void disconnectComposite() {
		properties = new SVNProperty[0];
	}

	public void setInput(SVNProperty[] revProperties) {
		properties = revProperties;
		UIMonitorUtility.getDisplay().syncExec(RevisionPropertiesComposite.this::rereshTableData);
	}

	public SVNProperty[] getSetProps() {
		return properties;
	}

	public void setLocationAndRevision(IRepositoryLocation location, SVNRevision revision) {
		this.location = location;
		this.revision = revision;
	}

	protected void rereshTableData() {
		if (isPending) {
			propertyViewer.setInput(new String[] { "" }); //$NON-NLS-1$
			return;
		}
		propertyViewer.setInput(RevisionPropertiesComposite.this.properties);
		propertyText.setText(""); //$NON-NLS-1$
	}

	protected void createControls(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		SashForm innerSashForm = new SashForm(this, SWT.VERTICAL);

		data = new GridData(GridData.FILL_BOTH);
		innerSashForm.setLayoutData(data);

		Table table = new Table(innerSashForm, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		table.setLayoutData(data);
		TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);

		propertyText = new Text(innerSashForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		propertyText.setBackground(propertyText.getBackground());
		propertyText.setEditable(false);
		data = new GridData(GridData.FILL_BOTH);
		propertyText.setLayoutData(data);

		innerSashForm.setWeights(new int[] { 70, 30 });

		propertyViewer = new TableViewer(table);
		propertyViewer.addSelectionChangedListener(event -> {
			if (event.getSelection() instanceof IStructuredSelection) {
				Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (selection != null && selection instanceof SVNProperty) {
					propertyText.setText(((SVNProperty) selection).value);
				}
			}
		});

		//creating a comparator right now to get column listeners
		ColumnedViewerComparator comparator = new ColumnedViewerComparator(propertyViewer) {

			@Override
			public int compareImpl(Viewer viewer, Object row1, Object row2) {
				if (row1 instanceof SVNProperty) {
					SVNProperty data1 = (SVNProperty) row1;
					SVNProperty data2 = (SVNProperty) row2;
					return column == RevisionPropertiesComposite.COLUMN_NAME
							? ColumnedViewerComparator.compare(data1.name, data2.name)
							: ColumnedViewerComparator.compare(data1.value, data2.value);
				}
				return 0;
			}

		};

		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.PropertiesComposite_Name);
		col.addSelectionListener(comparator);
		tableLayout.addColumnData(new ColumnWeightData(30, true));
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.PropertiesComposite_Value);
		col.addSelectionListener(comparator);
		tableLayout.addColumnData(new ColumnWeightData(70, true));

		//adding a comparator and selecting a default sort column
		propertyViewer.setComparator(comparator);
		comparator.setColumnNumber(RevisionPropertiesComposite.COLUMN_NAME);
		propertyViewer.getTable()
				.setSortColumn(propertyViewer.getTable().getColumn(RevisionPropertiesComposite.COLUMN_NAME));
		propertyViewer.getTable().setSortDirection(SWT.UP);

		propertyViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (isPending) {
					return (Object[]) inputElement;
				}
				return properties;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});

		propertyViewer.setLabelProvider(new ITableLabelProvider() {

			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				if (isPending) {
					if (columnIndex == RevisionPropertiesComposite.COLUMN_NAME) {
						return SVNUIMessages.getString(RepositoryPending.PENDING);
					}
					return ""; //$NON-NLS-1$
				}
				SVNProperty data = (SVNProperty) element;
				if (columnIndex == 0) {
					return data.name;
				}
				return FileUtility.formatMultilineText(data.value);
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
		});

		MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(table);
		menuMgr.addMenuListener(manager -> {
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			final IStructuredSelection tSelection = (IStructuredSelection) propertyViewer.getSelection();
			if (tSelection.size() == 1 && tSelection.getFirstElement() instanceof String) {
				return;
			}
			Action tAction = null;
			boolean isItitialized = properties != null;
			if (isItitialized) {
				manager.add(tAction = new Action(SVNUIMessages.PropertiesComposite_Add) {
					@Override
					public void run() {
						RevisionPropertiesComposite.this.editProperty(null);
					}
				});
				tAction.setEnabled(location != null);
				manager.add(tAction = new Action(SVNUIMessages.PropertiesComposite_Edit) {
					@Override
					public void run() {
						SVNProperty data = (SVNProperty) tSelection.getFirstElement();
						RevisionPropertiesComposite.this.editProperty(data);
					}
				});
				tAction.setEnabled(tSelection.size() == 1);
			}
		});
		menuMgr.setRemoveAllWhenShown(true);
		table.setMenu(menu);

		propertyViewer.addDoubleClickListener(e -> {
			IStructuredSelection selection = (IStructuredSelection) e.getSelection();
			if (selection.size() == 1) {
				SVNProperty data1 = (SVNProperty) selection.getFirstElement();
				RevisionPropertiesComposite.this.editProperty(data1);
			}
		});

	}

	protected void editProperty(SVNProperty data) {
		RevPropertiesEditPanel panel = new RevPropertiesEditPanel(properties, revision);
		panel.setPropertyToEdit(data);
		if (new DefaultDialog(UIMonitorUtility.getShell(), panel).open() == 0) {
			RevPropertiesEditPanel.doSetRevisionProperty(panel, location, revision);
		}
	}

}
