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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.wizard.shareproject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.ColumnedViewerComparator;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Allows to select group of projects which will be shared at once
 * 
 * @author Alexander Gurov
 */
public class SelectProjectsGroupPage extends AbstractVerifiedWizardPage implements IResourceProvider {
	protected IProject[] allProjects;

	protected Map projectGroups;

	protected int analysisDepth;

	protected int maxURLLength;

	protected String selectedGroup;

	protected Combo groupsCombo;

	protected Combo analysisDepthCombo;

	protected TableViewer viewer;

	public SelectProjectsGroupPage(IProject[] projects) {
		super(
				SelectProjectsGroupPage.class.getName(), SVNUIMessages.SelectProjectsGroupPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$

		setDescription(SVNUIMessages.SelectProjectsGroupPage_Description);
		allProjects = projects;
		analysisDepth = 2;
		maxURLLength = 0;

		performAnalysis();
		selectedGroup = projectGroups.get(null) != null ? null : (String) projectGroups.keySet().iterator().next();
	}

	public boolean isGroupSelectionRequired() {
		return allProjects.length > 1;
	}

	@Override
	public IResource[] getResources() {
		List group = getProjectsGroup(selectedGroup);
		return (IProject[]) group.toArray(new IProject[group.size()]);
	}

	protected void performAnalysis() {
		projectGroups = new LinkedHashMap();
		for (IProject project : allProjects) {
			SVNChangeStatus info = SVNUtility.getSVNInfoForNotConnected(project);
			if (info == null) {
				getProjectsGroup(null).add(project);
			} else {
				IPath url = SVNUtility.createPathForSVNUrl(SVNUtility.decodeURL(info.url));
				if (maxURLLength < url.segmentCount()) {
					maxURLLength = url.segmentCount();
				}
				if (url.segmentCount() > analysisDepth) {
					url = url.uptoSegment(analysisDepth);
				}
				getProjectsGroup(url.toString()).add(project);
			}
		}
		if (maxURLLength < analysisDepth) {
			analysisDepth = maxURLLength;
		}
	}

	protected List getProjectsGroup(Object key) {
		List retVal = (List) projectGroups.get(key);
		if (retVal == null) {
			projectGroups.put(key, retVal = new ArrayList());
		}
		return retVal;
	}

	protected void setGroupsComboItems() {
		ArrayList groups = new ArrayList(projectGroups.keySet());
		groups.remove(null);
		groupsCombo.setItems((String[]) groups.toArray(new String[groups.size()]));
		groupsCombo.select(0);
		if (groups.size() > 0) {
			selectedGroup = groupsCombo.getItem(0);
		}
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		initializeDialogUnits(parent);

		Label label = new Label(composite, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.heightHint = convertHeightInCharsToPixels(3);
		data.widthHint = 420;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.SelectProjectsGroupPage_Hint);

		Button shareProjectsButton = new Button(composite, SWT.RADIO);
		data = new GridData();
		data.horizontalSpan = 2;
		shareProjectsButton.setLayoutData(data);
		shareProjectsButton.setText(SVNUIMessages.SelectProjectsGroupPage_ShareNew);
		boolean newEnabled = projectGroups.get(null) != null;
		shareProjectsButton.setEnabled(newEnabled);
		shareProjectsButton.setSelection(newEnabled);
		shareProjectsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					groupsCombo.setEnabled(false);
					analysisDepthCombo.setEnabled(false);
					selectedGroup = null;
				}
				viewer.setInput(projectGroups);
			}
		});

		Button reconnectProjectsButton = new Button(composite, SWT.RADIO);
		data = new GridData();
		data.horizontalSpan = 2;
		reconnectProjectsButton.setLayoutData(data);
		reconnectProjectsButton.setEnabled(projectGroups.size() > 1 || projectGroups.get(null) == null);
		reconnectProjectsButton.setText(SVNUIMessages.SelectProjectsGroupPage_Reconnect);
		reconnectProjectsButton.setSelection(!newEnabled);
		reconnectProjectsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					groupsCombo.setEnabled(true);
					analysisDepthCombo.setEnabled(true);
					selectedGroup = groupsCombo.getItem(groupsCombo.getSelectionIndex());
				}
				viewer.setInput(projectGroups);
			}
		});

		analysisDepthCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		data = new GridData();
		analysisDepthCombo.setLayoutData(data);
		analysisDepthCombo.setVisibleItemCount(10);
		analysisDepthCombo.setEnabled(!newEnabled);
		analysisDepthCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				analysisDepth = ((Combo) e.widget).getSelectionIndex() + 1;
				SelectProjectsGroupPage.this.performAnalysis();
				SelectProjectsGroupPage.this.setGroupsComboItems();
				viewer.setInput(projectGroups);
			}
		});
		String[] allItems = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
		String[] items = allItems;
		if (maxURLLength < 10) {
			items = new String[maxURLLength];
			System.arraycopy(allItems, 0, items, 0, maxURLLength);
		}
		analysisDepthCombo.setItems(items);
		analysisDepthCombo.select(1);

		groupsCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		groupsCombo.setLayoutData(data);
		setGroupsComboItems();
		groupsCombo.setEnabled(!newEnabled);
		groupsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo c = (Combo) e.widget;
				selectedGroup = c.getItem(c.getSelectionIndex());
				viewer.setInput(projectGroups);
			}
		});

		label = new Label(composite, SWT.NONE);
		data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.SelectProjectsGroupPage_ProjectsList);

		viewer = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 420;
		data.horizontalSpan = 2;
		table.setLayoutData(data);

		viewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				List group = SelectProjectsGroupPage.this.getProjectsGroup(selectedGroup);
				return group.toArray();
			}

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		viewer.setLabelProvider(new ITableLabelProvider() {
			@Override
			public void removeListener(ILabelProviderListener listener) {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
			}

			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				return ((IProject) element).getName();
			}
		});

		ColumnedViewerComparator comparator = new ColumnedViewerComparator(viewer) {
			@Override
			public int compareImpl(Viewer viewer, Object row1, Object row2) {
				return ColumnedViewerComparator.compare(row1.toString(), row2.toString());
			}
		};

		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(false);
		col.setText(SVNUIMessages.SelectProjectsGroupPage_ProjectName);
		col.addSelectionListener(comparator);
		TableLayout tLayout = new TableLayout();
		tLayout.addColumnData(new ColumnWeightData(100));
		table.setLayout(tLayout);

		viewer.getTable().setSortDirection(SWT.UP);
		viewer.getTable().setSortColumn(viewer.getTable().getColumn(0));

		selectedGroup = projectGroups.get(null) != null ? null : (String) projectGroups.keySet().iterator().next();
		viewer.setInput(projectGroups);

//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, "org.eclipse.team.svn.help.projectGroupContext"); //$NON-NLS-1$

		return composite;
	}

}
