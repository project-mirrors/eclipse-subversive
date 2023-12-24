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

package org.eclipse.team.svn.ui.wizard.shareproject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.ArrayStructuredContentProvider;
import org.eclipse.team.svn.ui.utility.ColumnedViewerComparator;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Already connected to SVN wizard page
 * 
 * @author Alexander Gurov
 */
public class AlreadyConnectedPage extends AbstractVerifiedWizardPage {
	protected boolean useProjectSettings;

	protected boolean createUsingProjectSettings;

	protected String url;

	protected Text urlText;

	protected TableViewer repositoryRootsView;

	protected IRepositoryRoot[] repositoryRoots;

	protected IRepositoryRoot selectedRoot;

	protected Button useProjectSettingsButton;

	protected Button createProjectLocationButton;

	protected Button reconnectButton;

	public AlreadyConnectedPage() {
		super(
				AlreadyConnectedPage.class.getName(), SVNUIMessages.AlreadyConnectedPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$

		setDescription(SVNUIMessages.AlreadyConnectedPage_Description);
	}

	public void setProjects(IProject[] projects) {
		url = null;
		for (IProject project : projects) {
			SVNChangeStatus info = SVNUtility.getSVNInfoForNotConnected(project);
			String tmp = SVNUtility.decodeURL(info.url);
			if (url == null) {
				url = tmp;
			} else {
				IPath tPath = SVNUtility.createPathForSVNUrl(url);
				IPath tPath2 = SVNUtility.createPathForSVNUrl(tmp);
				if (tPath2.isPrefixOf(tPath)) {
					url = tmp;
				} else {
					while (!tPath.isPrefixOf(tPath2)) {
						tPath = tPath.removeLastSegments(1);
					}
					url = tPath.toString();
				}
			}
		}
		repositoryRoots = SVNUtility.findRoots(url, false);

		initControls();
	}

	public IRepositoryRoot getSelectedRoot() {
		return selectedRoot;
	}

	public String getResourceUrl() {
		return url;
	}

	public boolean useProjectSettings() {
		return useProjectSettings;
	}

	public boolean createUsingProjectSettings() {
		return createUsingProjectSettings;
	}

	@Override
	public boolean canFlipToNextPage() {
		return !useProjectSettings();
	}

	@Override
	public Composite createControlImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		initializeDialogUnits(parent);

		Label description = new Label(composite, SWT.WRAP);
		description.setLayoutData(makeGridData());
		description.setText(SVNUIMessages.AlreadyConnectedPage_ProjectURL);

		urlText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		urlText.setLayoutData(data);
		urlText.setEditable(false);

		description = new Label(composite, SWT.WRAP);
		description.setLayoutData(makeGridData());
		description.setText(SVNUIMessages.AlreadyConnectedPage_RepositoryLocation);

		Table table = new Table(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableLayout tLayout = new TableLayout();
		tLayout.addColumnData(new ColumnWeightData(30, true));
		tLayout.addColumnData(new ColumnWeightData(70, true));
		table.setLayout(tLayout);

		repositoryRootsView = new TableViewer(table);
		repositoryRootsView.setContentProvider(new ArrayStructuredContentProvider());
		final ITableLabelProvider labelProvider = new ITableLabelProvider() {
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				IRepositoryRoot root = (IRepositoryRoot) element;
				if (columnIndex == 0) {
					return root.getRepositoryLocation().getLabel();
				}
				return root.getRepositoryLocation().getUrl();
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
		repositoryRootsView.setLabelProvider(labelProvider);

		ColumnedViewerComparator comparator = new ColumnedViewerComparator(repositoryRootsView) {
			@Override
			public int compareImpl(Viewer viewer, Object row1, Object row2) {
				String val1 = labelProvider.getColumnText(row1, column);
				String val2 = labelProvider.getColumnText(row2, column);
				return ColumnedViewerComparator.compare(val1, val2);
			}
		};
		repositoryRootsView.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) repositoryRootsView.getSelection();
			selectedRoot = (IRepositoryRoot) selection.getFirstElement();
			AlreadyConnectedPage.this.setPageComplete(true);
		});

		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setResizable(true);
		col.setText(SVNUIMessages.AlreadyConnectedPage_LocationLabel);
		col.addSelectionListener(comparator);

		col = new TableColumn(table, SWT.LEFT);
		col.setResizable(true);
		col.setText(SVNUIMessages.AlreadyConnectedPage_URL);
		col.addSelectionListener(comparator);

		repositoryRootsView.getTable().setSortDirection(SWT.UP);
		repositoryRootsView.getTable().setSortColumn(repositoryRootsView.getTable().getColumn(0));

		Composite btnComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		btnComposite.setLayout(layout);
		data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		btnComposite.setLayoutData(data);

		description = new Label(btnComposite, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.heightHint = convertHeightInCharsToPixels(2);
		description.setLayoutData(data);
		description.setText(SVNUIMessages.AlreadyConnectedPage_Hint);

		useProjectSettingsButton = new Button(btnComposite, SWT.RADIO);
		useProjectSettingsButton.setLayoutData(makeGridData());
		useProjectSettingsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
				boolean selectEnabled = useProjectSettings = button.getSelection();
				repositoryRootsView.getTable().setEnabled(selectEnabled);
				AlreadyConnectedPage.this.setPageComplete(true);
			}
		});
		useProjectSettingsButton.setText(SVNUIMessages.AlreadyConnectedPage_UseProjectSettings);

		createProjectLocationButton = new Button(btnComposite, SWT.RADIO);
		createProjectLocationButton.setLayoutData(makeGridData());
		createProjectLocationButton.setText(SVNUIMessages.AlreadyConnectedPage_CreateLocation);
		createProjectLocationButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
				createUsingProjectSettings = button.getSelection();
				repositoryRootsView.getTable().setEnabled(false);
				AlreadyConnectedPage.this.setPageComplete(true);
			}
		});

		reconnectButton = new Button(btnComposite, SWT.RADIO);
		reconnectButton.setLayoutData(makeGridData());
		reconnectButton.setText(SVNUIMessages.AlreadyConnectedPage_ReconnectToAnother);

		initControls();

//		Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(composite, "org.eclipse.team.svn.help.alreadyConnectedContext"); //$NON-NLS-1$

		return composite;
	}

	protected void initControls() {
		if (urlText != null && url != null) {
			urlText.setText(url);
			repositoryRootsView.setInput(repositoryRoots);
			if (repositoryRoots.length > 0) {
				repositoryRootsView.getTable().select(0);
				selectedRoot = repositoryRoots[0];
				repositoryRootsView.getTable().setEnabled(true);
				useProjectSettings = true;
				createUsingProjectSettings = false;
				useProjectSettingsButton.setSelection(true);
				reconnectButton.setSelection(false);
				//this.createProjectLocationButton.setEnabled(false);
			} else {
				repositoryRootsView.getTable().setEnabled(false);
				useProjectSettingsButton.setSelection(false);
				useProjectSettingsButton.setEnabled(false);
				useProjectSettings = false;
				createUsingProjectSettings = true;
				createProjectLocationButton.setSelection(false);
				createProjectLocationButton.setSelection(true);
			}
		}
	}

	protected GridData makeGridData() {
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		return data;
	}

}
