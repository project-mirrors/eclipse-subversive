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

package org.eclipse.team.svn.ui.composite;

import java.io.FileOutputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.core.operation.local.property.RemovePropertiesOperation;
import org.eclipse.team.svn.core.operation.remote.GetRemotePropertiesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.RemoteResourceTransfer;
import org.eclipse.team.svn.ui.RemoteResourceTransferrable;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.local.SetExternalDefinitionAction;
import org.eclipse.team.svn.ui.action.local.SetKeywordsAction;
import org.eclipse.team.svn.ui.action.local.SetPropertyAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.dialog.SetPropertyWithOverrideDialog;
import org.eclipse.team.svn.ui.panel.view.property.PropertyApplyPanel;
import org.eclipse.team.svn.ui.properties.PropertiesView;
import org.eclipse.team.svn.ui.properties.RemovePropertyDialog;
import org.eclipse.team.svn.ui.properties.ResourcePropertyEditPanel;
import org.eclipse.team.svn.ui.repository.model.RepositoryPending;
import org.eclipse.team.svn.ui.utility.ColumnedViewerComparator;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ResourceTransfer;

/**
 * Property viewer composite
 * 
 * @author Sergiy Logvin
 */
public class PropertiesComposite extends Composite {
	public static final int APPLY_TO_ALL = 0;

	public static final int APPLY_TO_FILES = 1;

	public static final int APPLY_TO_FOLDERS = 2;

	protected static final int COLUMN_NAME = 0;

	protected static final int COLUMN_VALUE = 1;

	protected SVNProperty[] properties;

	protected TableViewer propertyViewer;

	protected Text propertyText;

	protected boolean isProcessing;

	protected PropertiesView propertiesView;

	protected IResourcePropertyProvider provider;

	protected IResource wcResource;

	protected IRepositoryResource repositoryResource;

	protected IAdaptable resource;

	public PropertiesComposite(Composite parent) {
		super(parent, SWT.NONE);
		isProcessing = false;
		createControls(parent);
	}

	public synchronized void setResource(IAdaptable resource, IResourcePropertyProvider provider) {
		if (resource instanceof IResource) {
			repositoryResource = null;
			wcResource = (IResource) resource;
		} else if (resource instanceof IRepositoryResource) {
			repositoryResource = (IRepositoryResource) resource;
		}
		this.provider = provider;
	}

	public void setPropertiesView(PropertiesView view) {
		propertiesView = view;
	}

	public IActionOperation getRefreshViewOperation() {
		return new AbstractActionOperation("Operation_PShowProperties", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				synchronized (PropertiesComposite.this) {
					if (provider != null && provider.getExecutionState() == IActionOperation.OK) {
						if (resource != null
								&& !(resource.equals(provider.getLocal()) || resource.equals(provider.getRemote()))) {
							//do nothing if by this moment resource selected in Properties Composite is different
							//from the resource for which the Get(Remote)PropertiesOperation was executed
							return;
						}
						provider.refresh();
						properties = provider.getProperties();
						if (properties == null) {
							properties = new SVNProperty[0];
						}
					} else {
						properties = null;
					}
				}
				if (!PropertiesComposite.this.isDisposed()) {
					PropertiesComposite.this.getDisplay().syncExec(() -> {
						if (properties != null) {
							PropertiesComposite.this.setPending(false);
						}
						PropertiesComposite.this.initializeComposite();
					});
				}
			}
		};
	}

	private void createControls(Composite parent) {
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
		PropertiesTableComparator comparator = new PropertiesTableComparator(propertyViewer);

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
		comparator.setColumnNumber(PropertiesComposite.COLUMN_NAME);
		propertyViewer.getTable().setSortColumn(propertyViewer.getTable().getColumn(PropertiesComposite.COLUMN_NAME));
		propertyViewer.getTable().setSortDirection(SWT.UP);

		propertyViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (isProcessing) {
					return (Object[]) inputElement;
				}
				if (wcResource == null && repositoryResource == null) {
					return new SVNProperty[0];
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
				if (isProcessing) {
					if (columnIndex == 0 && wcResource != null) {
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
			synchronized (PropertiesComposite.this) {
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				final IStructuredSelection tSelection = (IStructuredSelection) propertyViewer.getSelection();
				if (tSelection.size() == 1 && tSelection.getFirstElement() instanceof String) {
					return;
				}
				Action tAction = null;
				boolean isEditAllowed = provider != null && provider.isEditAllowed();
				if (wcResource != null && repositoryResource == null) {
					manager.add(tAction = new Action(SVNUIMessages.PropertiesComposite_Add) {
						@Override
						public void run() {
							PropertiesComposite.this.editProperty(null);
						}
					});
					tAction.setEnabled(isEditAllowed);
					manager.add(tAction = new Action(SVNUIMessages.PropertiesComposite_Edit) {
						@Override
						public void run() {
							SVNProperty data = (SVNProperty) tSelection.getFirstElement();
							PropertiesComposite.this.editProperty(data);
						}
					});
					tAction.setEnabled(isEditAllowed && tSelection.size() == 1);
					manager.add(tAction = new Action(SVNUIMessages.PropertiesComposite_Remove) {
						@Override
						public void run() {
							SVNProperty[] data = (SVNProperty[]) tSelection.toList()
									.toArray(new SVNProperty[tSelection.size()]);
							PropertiesComposite.this.removeProperty(data);
						}
					});
					tAction.setEnabled(isEditAllowed && tSelection.size() > 0);
					manager.add(new Separator());
					manager.add(tAction = new Action(SVNUIMessages.PropertiesComposite_SetKeywords) {
						@Override
						public void run() {
							PropertiesComposite.this.setKeywords();
						}
					});
					manager.add(tAction = new Action(SVNUIMessages.Action_SetExternals) {
						@Override
						public void run() {
							IActionOperation op = SetExternalDefinitionAction.getAction(
									wcResource, PropertiesComposite.this.getShell());
							if (op != null) {
								UIMonitorUtility.doTaskScheduledDefault(op);
							}
						}
					});
					tAction.setEnabled(wcResource instanceof IContainer);
					manager.add(tAction = new Action(SVNUIMessages.PropertiesComposite_ApplyRecursively) {
						@Override
						public void run() {
							SVNProperty[] data = (SVNProperty[]) tSelection.toList()
									.toArray(new SVNProperty[tSelection.size()]);
							PropertiesComposite.this.setPropertyRecursive(data);
						}
					});
					tAction.setEnabled(isEditAllowed && tSelection.size() > 0 && wcResource instanceof IContainer);
				}
				manager.add(tAction = new Action(SVNUIMessages.PropertiesComposite_SaveValueToFile) {
					@Override
					public void run() {
						SVNProperty data = (SVNProperty) tSelection.getFirstElement();
						PropertiesComposite.this.saveValueToFile(data);
					}
				});
				tAction.setEnabled(provider != null && tSelection.size() == 1);

				manager.add(new Separator());
				manager.add(tAction = new Action(SVNUIMessages.PropertiesComposite_Refresh) {
					@Override
					public void run() {
						synchronized (PropertiesComposite.this) {
							if (repositoryResource != null) {
								provider = new GetRemotePropertiesOperation(
										repositoryResource);
							}
							CompositeOperation composite = new CompositeOperation("Operation_PRefresh", //$NON-NLS-1$
									SVNUIMessages.class);
							if (provider != null && provider.getExecutionState() != IStatus.OK) {
								composite.add(provider);
								composite.add(PropertiesComposite.this.getRefreshViewOperation(),
										new IActionOperation[] { provider });
							} else {
								composite.add(PropertiesComposite.this.getRefreshViewOperation());
							}

							UIMonitorUtility.doTaskScheduledActive(composite);
						}
					}
				});
				tAction.setImageDescriptor(
						SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/refresh.gif")); //$NON-NLS-1$
				tAction.setEnabled(provider != null);
			}
		});
		menuMgr.setRemoveAllWhenShown(true);
		table.setMenu(menu);

		propertyViewer.addDropSupport(DND.DROP_LINK,
				new Transfer[] { ResourceTransfer.getInstance(), RemoteResourceTransfer.getInstance() },
				new DropTargetAdapter() {

					@Override
					public void dragEnter(DropTargetEvent event) {
						event.detail = DND.DROP_LINK;
					}

					@Override
					public void drop(DropTargetEvent event) {
						if (propertiesView == null) {
							return;
						}
						if (event.data instanceof IResource[]) {
							IResource resource = ((IResource[]) event.data)[0];
							if (IStateFilter.SF_VERSIONED
									.accept(SVNRemoteStorage.instance().asLocalResource(resource))) {
								propertiesView.setResource(resource, new GetPropertiesOperation(resource), true);
							}
							return;
						}
						if (event.data instanceof RemoteResourceTransferrable) {
							IRepositoryResource resource = ((RemoteResourceTransferrable) event.data).resources[0];
							propertiesView.setResource(resource, new GetRemotePropertiesOperation(resource), false);
						}
					}
				});

		propertyViewer.addDoubleClickListener(e -> {
			synchronized (PropertiesComposite.this) {
				if (provider != null && provider.isEditAllowed()) {
					IStructuredSelection selection = (IStructuredSelection) e.getSelection();
					if (selection.size() == 1) {
						SVNProperty data1 = (SVNProperty) selection.getFirstElement();
						PropertiesComposite.this.editProperty(data1);
					}
				}
			}
		});
	}

	protected void removeProperty(SVNProperty[] data) {
		RemovePropertyDialog dialog = new RemovePropertyDialog(getShell(), data.length == 1,
				wcResource instanceof IFile);
		if (dialog.open() != 0) {
			return;
		}
		CompositeOperation composite = new CompositeOperation("Operation_PRemoveProperty", SVNUIMessages.class); //$NON-NLS-1$

		composite.add(new RemovePropertiesOperation(new IResource[] { wcResource }, data, dialog.isRecursive()));
		composite.add(getRefreshOperation(dialog.isRecursive() ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO));
		UIMonitorUtility.doTaskScheduledActive(composite);
	}

	protected void setKeywords() {
		SetKeywordsAction.doSetKeywords(new IResource[] { wcResource });
	}

	protected void setPropertyRecursive(SVNProperty[] data) {
		PropertyApplyPanel panel = new PropertyApplyPanel(data.length == 1);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);

		if (dialog.open() == 0) {
			SetPropertyAction.doSetProperty(new IResource[] { wcResource }, data, null, true, panel.getApplyMethod(),
					panel.useMask(), panel.getFilterMask(), false, getRefreshViewOperation());
		}
	}

	protected void editProperty(SVNProperty data) {
		boolean propertyAlreadyExists = false;
		boolean override = true;
		IResource[] resources = { wcResource };
		final ResourcePropertyEditPanel panel = new ResourcePropertyEditPanel(
				data == null ? null : new SVNProperty[] { data }, resources, false);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);

		if (dialog.open() != 0) {
			return;
		}

		for (SVNProperty property : properties) {
			if (property.name.equals(panel.getPropertyName())) {
				propertyAlreadyExists = true;
				break;
			}
		}
		if (propertyAlreadyExists && data == null) {
			SetPropertyWithOverrideDialog overrideDialog = new SetPropertyWithOverrideDialog(getShell(),
					panel.getPropertyName());
			if (overrideDialog.open() != 0) {
				override = false;
			}
		}
		if (override) {
			SetPropertyAction.doSetProperty(resources, panel, getRefreshViewOperation());
		}
	}

	protected void saveValueToFile(final SVNProperty data) {
		FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
		fileDialog.setFileName(data.name);
		final String fileName = fileDialog.open();

		AbstractActionOperation saveValue = new AbstractActionOperation("Operation_PSaveValueToFile", //$NON-NLS-1$
				SVNUIMessages.class) {
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				FileOutputStream output = null;
				if (fileName != null) {
					output = new FileOutputStream(fileName);
					byte[] value = data.value.getBytes();
					output.write(value);
				}
				if (output != null) {
					output.close();
				}
			}
		};
		UIMonitorUtility.doTaskBusyDefault(saveValue);
	}

	protected IActionOperation getRefreshOperation(int depth) {
		CompositeOperation composite = new CompositeOperation("Operation_PRefreshView", SVNUIMessages.class); //$NON-NLS-1$
		composite.add(getRefreshViewOperation());
		if (wcResource != null) {
			composite.add(new RefreshResourcesOperation(new IResource[] { wcResource }, IResource.DEPTH_INFINITE,
					RefreshResourcesOperation.REFRESH_ALL));
		}
		return composite;
	}

	public void initializeComposite() {
		if (isProcessing) {
			propertyViewer.setInput(new String[] { "" }); //$NON-NLS-1$
			propertyViewer.getTable().setLinesVisible(false);
		} else {
			propertyViewer.setInput(properties);
			propertyViewer.getTable().setLinesVisible(true);
		}
		propertyText.setText(""); //$NON-NLS-1$
	}

	public void setPending(boolean isProcessing) {
		this.isProcessing = isProcessing;
	}

	public synchronized void disconnectComposite() {
		wcResource = null;
		properties = null;
		provider = null;
	}

	protected class PropertiesTableComparator extends ColumnedViewerComparator {

		public PropertiesTableComparator(Viewer tableViewer) {
			super(tableViewer);
		}

		@Override
		public int compareImpl(Viewer viewer, Object row1, Object row2) {
			if (row1 instanceof SVNProperty) {
				SVNProperty data1 = (SVNProperty) row1;
				SVNProperty data2 = (SVNProperty) row2;
				return column == PropertiesComposite.COLUMN_NAME
						? ColumnedViewerComparator.compare(data1.name, data2.name)
						: ColumnedViewerComparator.compare(data1.value, data2.value);
			}
			return 0;
		}

	}

}
