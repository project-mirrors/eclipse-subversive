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

package org.eclipse.team.svn.ui.wizard.checkoutas;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;

/**
 * Project selection page
 *
 * @author Sergiy Logvin
 */
public class ProjectsSelectionPage extends AbstractVerifiedWizardPage {

	protected List selectedProjects;

	protected IRepositoryResource[] projects;

	protected CheckboxTableViewer listViewer;

	protected boolean respectHierarchy;

	protected boolean checkoutAsFolders;

	protected ProjectLocationSelectionPage locationPage;

	protected Button respectHierarchyButton;

	protected Button checkoutAsFolderButton;

	protected Button checkoutAsProjectButton;

	public ProjectsSelectionPage() {
		super(ProjectsSelectionPage.class.getName(), "", //$NON-NLS-1$
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
	}

	public boolean isCheckoutAsFoldersSelected() {
		return checkoutAsFolders;
	}

	public List getSelectedProjects() {
		return selectedProjects;
	}

	public boolean isRespectHierarchy() {
		return !checkoutAsFolders && respectHierarchy;
	}

	public void postInit(ProjectLocationSelectionPage locationPage, IRepositoryResource[] projects,
			ITableLabelProvider labelProvider, IStructuredContentProvider contentProvider) {
		this.locationPage = locationPage;
		this.projects = projects;
		listViewer.setLabelProvider(labelProvider);
		listViewer.setContentProvider(contentProvider);
		if (projects.length > 1) {
			setTitle(SVNUIMessages.ProjectsSelectionPage_Title_Multi);
			setDescription(SVNUIMessages.ProjectsSelectionPage_Description_Multi);
		} else {
			setTitle(SVNUIMessages.ProjectsSelectionPage_Title_Single);
			setDescription(SVNUIMessages.ProjectsSelectionPage_Description_Single);
		}

		checkoutAsFolderButton.setText(this.projects.length > 0
				? SVNUIMessages.ProjectsSelectionPage_CheckoutAsFolder_Multi
				: SVNUIMessages.ProjectsSelectionPage_CheckoutAsFolder_Single);
		checkoutAsProjectButton.setText(this.projects.length > 0
				? SVNUIMessages.ProjectsSelectionPage_CheckoutAsProject_Multi
				: SVNUIMessages.ProjectsSelectionPage_CheckoutAsProject_Single);

		listViewer.setInput(projects);
		listViewer.setAllChecked(true);
		refreshSelectedResult();
		validateContent();
	}

	@Override
	public Composite createControlImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 12;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Composite coTypeComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		coTypeComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		coTypeComposite.setLayoutData(data);

		checkoutAsFolderButton = new Button(coTypeComposite, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		checkoutAsFolderButton.setLayoutData(data);
		checkoutAsFolderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkoutAsFolders = true;
				respectHierarchyButton.setEnabled(false);
				ProjectsSelectionPage.this.validateContent();
			}
		});
		checkoutAsFolderButton.setSelection(false);

		checkoutAsProjectButton = new Button(coTypeComposite, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		checkoutAsProjectButton.setLayoutData(data);
		checkoutAsProjectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkoutAsFolders = false;
				respectHierarchyButton.setEnabled(true);
				ProjectsSelectionPage.this.validateContent();
			}
		});
		checkoutAsProjectButton.setSelection(true);

		listViewer = createViewer(composite);
		data = new GridData(GridData.FILL_BOTH);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		listViewer.getTable().setLayoutData(data);

		listViewer.addSelectionChangedListener(event -> {
			ProjectsSelectionPage.this.refreshSelectedResult();
			ProjectsSelectionPage.this.validateContent();
		});
		layout = null;

		attachTo(listViewer.getTable(), new AbstractVerifier() {
			@Override
			protected String getWarningMessage(Control input) {
				return null;
			}

			@Override
			protected String getErrorMessage(Control input) {
				Object[] elements = listViewer.getCheckedElements();
				return elements == null || elements.length == 0
						? SVNUIMessages.ProjectsSelectionPage_CheckoutAsProject_Verifier_Error
						: null;
			}
		});

		Composite bottomPart = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		bottomPart.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		bottomPart.setLayoutData(data);

		Composite tComposite = new Composite(bottomPart, SWT.LEFT);
		GridLayout gLayout = new GridLayout();
		gLayout.numColumns = 2;
		gLayout.marginWidth = gLayout.marginHeight = 0;
		tComposite.setLayout(gLayout);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		tComposite.setData(data);

		Button selectButton = new Button(tComposite, SWT.PUSH);
		selectButton.setText(SVNUIMessages.Button_SelectAll);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(selectButton);
		selectButton.setLayoutData(data);
		selectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(true);
				ProjectsSelectionPage.this.refreshSelectedResult();
				ProjectsSelectionPage.this.validateContent();
			}
		});

		Button deselectButton = new Button(tComposite, SWT.PUSH);
		deselectButton.setText(SVNUIMessages.Button_ClearSelection);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(deselectButton);
		deselectButton.setLayoutData(data);
		deselectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(false);
				ProjectsSelectionPage.this.refreshSelectedResult();
				ProjectsSelectionPage.this.validateContent();
			}
		});

		respectHierarchyButton = new Button(bottomPart, SWT.CHECK);
		respectHierarchyButton.setText(SVNUIMessages.ProjectsSelectionPage_RespectHierarchy);
		respectHierarchyButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_HORIZONTAL));
		respectHierarchyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				respectHierarchy = ((Button) e.widget).getSelection();
				if (locationPage != null) {
					locationPage.setUseDefaultLocation(!respectHierarchy);
					locationPage.validateContent();
				}
				ProjectsSelectionPage.this.validateContent();
			}
		});

		return composite;
	}

	protected CheckboxTableViewer createViewer(Composite parent) {
		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.CHECK | SWT.BORDER);
		TableLayout layout = new TableLayout();
		GridData data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
		table.setLayout(layout);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);

		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(false);
		layout.addColumnData(new ColumnPixelData(20, false));

		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.ProjectsSelectionPage_RepositoryURL);
		layout.addColumnData(new ColumnPixelData(270, true));

		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.ProjectsSelectionPage_ProjectName);
		layout.addColumnData(new ColumnPixelData(150, true));

		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(SVNUIMessages.ProjectsSelectionPage_ProjectType);
		layout.addColumnData(new ColumnPixelData(100, true));

		return viewer;
	}

	public void refreshSelectedResult() {
		if (projects != null) {
			ArrayList list = new ArrayList();
			for (IRepositoryResource project : projects) {
				if (listViewer.getChecked(project)) {
					list.add(project);
				}
			}
			selectedProjects = list;
		}
	}

}
