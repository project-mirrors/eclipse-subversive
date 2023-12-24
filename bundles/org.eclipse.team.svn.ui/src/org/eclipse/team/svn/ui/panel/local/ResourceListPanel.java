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

package org.eclipse.team.svn.ui.panel.local;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.ArrayStructuredContentProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Resource List Panel implementation
 * 
 * @author Sergiy Logvin
 */
public class ResourceListPanel extends AbstractDialogPanel {
	protected IResource[] resources;

	protected TableViewer tableViewer;

	protected boolean showLocalNames;

	protected String helpId;

	protected Map<ImageDescriptor, Image> images;

	public ResourceListPanel(IResource[] resources, String dialogTitle, String dialogDescription, String defaultMessage,
			String[] buttons) {
		this(resources, dialogTitle, dialogDescription, defaultMessage, buttons, null);
	}

	public ResourceListPanel(IResource[] resources, String dialogTitle, String dialogDescription, String defaultMessage,
			String[] buttons, String helpId) {
		super(buttons);
		this.dialogTitle = dialogTitle;
		this.dialogDescription = dialogDescription;
		this.defaultMessage = defaultMessage;
		this.resources = resources;
		images = new HashMap<>();
	}

	public boolean isShowLocalNames() {
		return showLocalNames;
	}

	public void setShowLocalNames(boolean showLocalNames) {
		this.showLocalNames = showLocalNames;
	}

	@Override
	public String getHelpId() {
		return helpId;
	}

	@Override
	public void dispose() {
		for (Image img : images.values()) {
			img.dispose();
		}
		super.dispose();
	}

	@Override
	public void createControlsImpl(Composite parent) {
		Table table = new Table(parent, SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
		TableLayout layout = new TableLayout();
		table.setLayout(layout);

		tableViewer = new TableViewer(table);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 120;
		tableViewer.getTable().setLayoutData(data);

		final TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		layout.addColumnData(new ColumnWeightData(100, true));

		tableViewer.getTable().addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				col.setWidth(tableViewer.getTable().getClientArea().width);
			}
		});

		tableViewer.setLabelProvider(new ITableLabelProvider() {
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				IWorkbenchAdapter adapter = ((IAdaptable) element).getAdapter(IWorkbenchAdapter.class);
				if (adapter == null) {
					return null;
				}
				ImageDescriptor descriptor = adapter.getImageDescriptor(element);
				if (descriptor == null) {
					return null;
				}
				Image image = images.get(descriptor);
				if (image == null) {
					image = descriptor.createImage();
					images.put(descriptor, image);
				}
				return image;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				IResource resource = (IResource) element;
				if (showLocalNames) {
					return resource.getFullPath().toString().substring(1);
				}
				IRepositoryResource node = SVNRemoteStorage.instance().asRepositoryResource(resource);
				return node.getUrl();
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
			}
		});

		tableViewer.setContentProvider(new ArrayStructuredContentProvider());

		tableViewer.setInput(resources);
	}

	@Override
	protected void saveChangesImpl() {
	}

	@Override
	protected void cancelChangesImpl() {
	}

}
