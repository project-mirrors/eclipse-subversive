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
 *    Rene Link - [patch] NPE in Interactive Merge UI
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.team.svn.core.resource.IRepositoryBase;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.repository.RepositoryTreeViewer;
import org.eclipse.team.svn.ui.repository.model.IRepositoryContentFilter;
import org.eclipse.team.svn.ui.repository.model.RepositoriesRoot;
import org.eclipse.team.svn.ui.repository.model.RepositoryContentProvider;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.repository.model.RepositoryLocation;
import org.eclipse.team.svn.ui.repository.model.RepositoryResource;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;

/**
 * Repositories tree composite
 * 
 * @author Alexander Gurov
 */
public class RepositoryTreeComposite extends Composite {
	protected RepositoryTreeViewer repositoryTree;

	protected DrillDownAdapter ddAdapter;

	protected RepositoryContentProvider provider;

	protected boolean autoExpandFirstLevel;

	public RepositoryTreeComposite(Composite parent, int style) {
		this(parent, style, false);
	}

	public RepositoryTreeComposite(Composite parent, int style, boolean multiSelect) {
		this(parent, style, multiSelect, new RepositoriesRoot());
	}

	public RepositoryTreeComposite(Composite parent, int style, boolean multiSelect, Object input) {
		super(parent, style);
		createControls(multiSelect ? SWT.MULTI : SWT.SINGLE, input);
	}

	public RepositoryTreeViewer getRepositoryTreeViewer() {
		return repositoryTree;
	}

	public void setAutoExpandFirstLevel(boolean autoExpandFirstLevel) {
		this.autoExpandFirstLevel = autoExpandFirstLevel;
	}

	public Object getModelRoot() {
		return repositoryTree.getInput();
	}

	public void setModelRoot(Object root) {
		if (root instanceof IRepositoryLocation) {
			repositoryTree.setInput(new RepositoryLocation((IRepositoryLocation) root));
		} else if (root instanceof IRepositoryBase) {
			RepositoryResource resource = RepositoryFolder.wrapChild(null, (IRepositoryResource) root, null);
			resource.setViewer(repositoryTree);
			repositoryTree.setInput(resource);
		} else {
			repositoryTree.setInput(root);
		}
	}

	public IRepositoryContentFilter getFilter() {
		return provider.getFilter();
	}

	public void setFilter(IRepositoryContentFilter filter) {
		provider.setFilter(filter);
		repositoryTree.refresh();
	}

	private void createControls(int style, Object input) {
		GridData data = null;
		GridLayout layout = null;

		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);

		ToolBarManager toolBarMgr = new ToolBarManager(SWT.FLAT);
		ToolBar toolBar = toolBarMgr.createControl(this);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.BEGINNING;
		toolBar.setLayoutData(data);

		repositoryTree = new RepositoryTreeViewer(this, style | SWT.H_SCROLL | SWT.V_SCROLL);
		if (autoExpandFirstLevel) {
			repositoryTree.setAutoExpandLevel(2);
		}
		repositoryTree.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		provider = new RepositoryContentProvider(repositoryTree);
		repositoryTree.setContentProvider(provider);
		repositoryTree.setLabelProvider(new WorkbenchLabelProvider());
		setModelRoot(input);

		repositoryTree.setAutoExpandLevel(2);
		ddAdapter = new DrillDownAdapter(repositoryTree);
		ddAdapter.addNavigationActions(toolBarMgr);
		toolBarMgr.update(true);
	}

}
