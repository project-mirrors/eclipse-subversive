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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.common.RepositoryTreePanel;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * A UI component for selecting the recursion depth
 * 
 * @author Alexei Goncharov
 */
public class DepthSelectionComposite extends Composite {

	//getting strings for options
	protected final static String empty = SVNUIMessages.RecurseDepthSelector_Empty;

	protected final static String files = SVNUIMessages.RecurseDepthSelector_Files;

	protected final static String immediates = SVNUIMessages.RecurseDepthSelector_Immediates;

	protected final static String infinity = SVNUIMessages.RecurseDepthSelector_Infinity;

	protected final static String exclude = SVNUIMessages.RecurseDepthSelector_Exclude;

	protected final static String unknown = SVNUIMessages.RecurseDepthSelector_Unknown;

	protected boolean useWorkingCopyDepth;

	protected boolean supportSetDepth;

	//if there were selected several resources to update then there's not sense to allow to specify path
	protected boolean isShowUpdateDepthPath;

	protected IRepositoryResource resource;

	protected IValidationManager validationManager;

	//output
	protected SVNDepth depth;

	protected boolean isStickyDepth;

	protected String updatePath;

	protected Combo depthSelector;

	protected Button updateDepthButton;

	protected Text pathInput;

	protected Button browseButton;

	protected RepositoryResourceSelectionComposite selectionComposite;

	protected boolean svn15compatible;

	protected boolean svn16compatible;

	public DepthSelectionComposite(Composite parent, int style, boolean useWorkingCopyDepth) {
		this(parent, style, useWorkingCopyDepth, false, false, null, null);
	}

	public DepthSelectionComposite(Composite parent, int style, boolean useWorkingCopyDepth, boolean supportSetDepth,
			boolean canShowUpdateDepthPath, IRepositoryResource resource, IValidationManager validationManager) {
		super(parent, style);

		svn15compatible = CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x;
		svn16compatible = CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_6_x;

		this.supportSetDepth = supportSetDepth && svn15compatible;
		isStickyDepth = false;
		isShowUpdateDepthPath = this.supportSetDepth && canShowUpdateDepthPath;
		this.resource = resource;
		this.validationManager = validationManager;

		if (useWorkingCopyDepth && svn15compatible) {
			this.useWorkingCopyDepth = true;
			depth = isStickyDepth ? SVNDepth.INFINITY : SVNDepth.UNKNOWN;
		} else {
			this.useWorkingCopyDepth = false;
			depth = SVNDepth.INFINITY;
		}

		createControls();
	}

	protected void createControls() {
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);

		Composite parent;
		if (isShowUpdateDepthPath) {
			Group group = new Group(this, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 3;
			group.setLayout(layout);
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			group.setLayoutData(data);
			group.setText(SVNUIMessages.DepthSelectionComposite_DepthGroup);

			parent = group;
		} else {
			layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 0;
			layout.numColumns = supportSetDepth ? 3 : 2;
			setLayout(layout);

			parent = this;
		}

		Label label = new Label(parent, SWT.NONE);
		label.setText(SVNUIMessages.RecurseDepthSelector_Label);
		GridData data = new GridData();
		label.setLayoutData(data);

		depthSelector = new Combo(parent, SWT.READ_ONLY);
		depthSelector.setText(infinity);
		data = new GridData(GridData.FILL_HORIZONTAL);
		depthSelector.setLayoutData(data);
		depthSelector.setVisibleItemCount(6);

		if (supportSetDepth) {
			updateDepthButton = new Button(parent, SWT.CHECK);
			updateDepthButton.setLayoutData(new GridData());
			updateDepthButton.setText(SVNUIMessages.DepthSelectionComposite_UpdateDepth);
			updateDepthButton.addListener(SWT.Selection, event -> DepthSelectionComposite.this.refreshStickyDepth());
		}

		if (isShowUpdateDepthPath) {
			Label pathLabel = new Label(parent, SWT.NONE);
			pathLabel.setLayoutData(new GridData());
			pathLabel.setText(SVNUIMessages.DepthSelectionComposite_PathLabel);

			pathInput = new Text(parent, SWT.BORDER | SWT.SINGLE);
			data = new GridData(GridData.FILL_HORIZONTAL);
			pathInput.setLayoutData(data);
			pathInput.addModifyListener(e -> updatePath = pathInput.getText());

			browseButton = new Button(parent, SWT.PUSH);
			browseButton.setText(SVNUIMessages.Button_Browse);
			data = new GridData();
			data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
			browseButton.setLayoutData(data);

			browseButton.addListener(SWT.Selection, event -> DepthSelectionComposite.this.showPathSelectionPanel());
		}

		depthSelector.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Combo) e.widget).getItem(((Combo) e.widget).getSelectionIndex()).equals(infinity)) {
					depth = SVNDepth.INFINITY;
				} else if (((Combo) e.widget).getItem(((Combo) e.widget).getSelectionIndex()).equals(immediates)) {
					depth = SVNDepth.IMMEDIATES;
				} else if (((Combo) e.widget).getItem(((Combo) e.widget).getSelectionIndex()).equals(files)) {
					depth = SVNDepth.FILES;
				} else if (((Combo) e.widget).getItem(((Combo) e.widget).getSelectionIndex())
						.equals(DepthSelectionComposite.unknown)) {
					depth = SVNDepth.UNKNOWN;
				} else if (((Combo) e.widget).getItem(((Combo) e.widget).getSelectionIndex()).equals(exclude)) {
					depth = SVNDepth.EXCLUDE;
				} else {
					depth = SVNDepth.EMPTY;
				}
			}
		});

		//init values
		if (svn15compatible) {
			depthSelector.add(empty);
		}
		depthSelector.add(files);
		if (svn15compatible) {
			depthSelector.add(immediates);
		}
		depthSelector.add(infinity);

		if (supportSetDepth) {
			updateDepthButton.setSelection(isStickyDepth);
			refreshStickyDepth();
		} else if (useWorkingCopyDepth) {
			depthSelector.add(unknown);
		}

		//set depth
		setDepthComboValue();
	}

	protected void setDepthComboValue() {
		String strDepth;
		switch (depth) {
			case INFINITY:
				strDepth = infinity;
				break;
			case IMMEDIATES:
				strDepth = immediates;
				break;
			case FILES:
				strDepth = files;
				break;
			case UNKNOWN:
				strDepth = unknown;
				break;
			case EXCLUDE:
				strDepth = exclude;
				break;
			default:
				strDepth = empty;
		}
		int index = depthSelector.indexOf(strDepth);
		if (index == -1) {
			index = 0;
		}
		depthSelector.select(index);
	}

	protected void refreshStickyDepth() {
		isStickyDepth = updateDepthButton.getSelection();

		if (isShowUpdateDepthPath) {
			pathInput.setEnabled(isStickyDepth);
			browseButton.setEnabled(isStickyDepth);
		}

		//add or remove 'exclude'
		if (svn16compatible) {
			if (isStickyDepth) {
				depthSelector.add(exclude);
			} else {
				int index = depthSelector.indexOf(exclude);
				if (index != -1) {
					int selectionIndex = depthSelector.getSelectionIndex();
					depthSelector.remove(index);
					if (index == selectionIndex) {
						depthSelector.select(0);
					}
				}
			}
		}

		//add or remove 'working copy'
		if (useWorkingCopyDepth) {
			if (isStickyDepth) {
				int index = depthSelector.indexOf(unknown);
				if (index != -1) {
					int selectionIndex = depthSelector.getSelectionIndex();
					depthSelector.remove(index);
					if (index == selectionIndex) {
						depthSelector.select(depthSelector.indexOf(infinity));
					}
				}
			} else {
				depthSelector.add(unknown);
			}
		}
	}

	protected void showPathSelectionPanel() {
		RepositoryTreePanel panel = new RepositoryTreePanel(
				SVNUIMessages.RepositoryResourceSelectionComposite_Select_Title,
				SVNUIMessages.DepthSelectionComposite_RepositoryPanelDescription,
				SVNUIMessages.DepthSelectionComposite_RepositoryPanelMessage, new IRepositoryResource[0], false,
				resource, false);
		DefaultDialog browser = new DefaultDialog(getShell(), panel);
		if (browser.open() == 0) {
			IRepositoryResource selected = panel.getSelectedResource();
			if (selected != null) {
				pathInput.setText(selected.getName());
			}
		}
	}

	public SVNDepth getDepth() {
		return depth;
	}

	public boolean isStickyDepth() {
		return isStickyDepth;
	}

	public String getUpdatePath() {
		return updatePath != null ? updatePath.trim() : null;
	}

}
