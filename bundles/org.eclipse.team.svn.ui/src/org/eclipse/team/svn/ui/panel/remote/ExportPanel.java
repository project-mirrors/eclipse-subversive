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

package org.eclipse.team.svn.ui.panel.remote;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.DepthSelectionComposite;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.ExistingResourceVerifier;

/**
 * Export panel
 * 
 * @author Sergiy Logvin
 */
public class ExportPanel extends AbstractDialogPanel {
	protected Text locationField;

	protected String location;

	protected RevisionComposite revisionComposite;

	protected IRepositoryResource selectedResource;

	protected DepthSelectionComposite depthSelector;

	public ExportPanel(IRepositoryResource baseResource) {
		dialogTitle = SVNUIMessages.ExportPanel_Title;
		dialogDescription = SVNUIMessages.ExportPanel_Description;
		defaultMessage = SVNUIMessages.ExportPanel_Message;
		selectedResource = baseResource;
	}

	public SVNRevision getSelectedRevision() {
		return revisionComposite != null ? revisionComposite.getSelectedRevision() : SVNRevision.INVALID_REVISION;
	}

	@Override
	protected void saveChangesImpl() {
		location = locationField.getText();
	}

	@Override
	protected void cancelChangesImpl() {
	}

	public SVNDepth getDepth() {
		if (depthSelector == null) {
			return SVNDepth.INFINITY;
		}
		return depthSelector.getDepth();
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite folderComposite = new Composite(parent, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = layout.marginWidth = 0;
		folderComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		folderComposite.setLayoutData(data);

		Label label = new Label(folderComposite, SWT.NONE);
		data = new GridData();
		label.setLayoutData(data);
		label.setText(SVNUIMessages.ExportPanel_Folder);

		locationField = new Text(folderComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		locationField.setLayoutData(data);
		attachTo(locationField, new ExistingResourceVerifier(label.getText(), false));

		Button browseButton = new Button(folderComposite, SWT.PUSH);
		browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
		browseButton.setLayoutData(data);
		browseButton.addListener(SWT.Selection, event -> {
			DirectoryDialog fileDialog = new DirectoryDialog(ExportPanel.this.manager.getShell());
			fileDialog.setText(SVNUIMessages.ExportPanel_ExportFolder);
			fileDialog.setMessage(SVNUIMessages.ExportPanel_ExportFolder_Msg);
			String path = fileDialog.open();
			if (path != null) {
				locationField.setText(path);
			}
		});

		if (selectedResource != null) {
			revisionComposite = new RevisionComposite(parent, this, false, null, SVNRevision.HEAD, false);
			data = new GridData(GridData.FILL_HORIZONTAL);
			revisionComposite.setLayoutData(data);
			revisionComposite.setSelectedResource(selectedResource);
		}

		if (selectedResource instanceof IRepositoryContainer || selectedResource == null) {
			Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			separator.setVisible(false);

			depthSelector = new DepthSelectionComposite(parent, SWT.NONE, false);
			data = new GridData(GridData.FILL_HORIZONTAL);
			depthSelector.setLayoutData(data);
		}
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.remote_exportDialogContext"; //$NON-NLS-1$
	}

	public String getLocation() {
		return location;
	}

}
