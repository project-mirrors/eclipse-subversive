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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.composite.DepthSelectionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.ExistingResourceVerifier;

/**
 * Import Panel
 * 
 * @author Sergiy Logvin
 */
public class ImportPanel extends AbstractDialogPanel {
	protected Text locationField;

	protected String location;

	protected Button recursiveButton;

	protected DepthSelectionComposite depthSelector;

	protected CommentComposite comment;

	public ImportPanel(String importToUrl) {
		dialogTitle = SVNUIMessages.ImportPanel_Title;
		dialogDescription = SVNUIMessages.ImportPanel_Description;
		defaultMessage = BaseMessages.format(SVNUIMessages.ImportPanel_Message, new String[] { importToUrl });
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite folderSelectionComposite = new Composite(parent, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		folderSelectionComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		folderSelectionComposite.setLayoutData(data);

		Label folder = new Label(folderSelectionComposite, SWT.NONE);
		folder.setText(SVNUIMessages.ImportPanel_Folder);

		locationField = new Text(folderSelectionComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		locationField.setLayoutData(data);
		attachTo(locationField, new ExistingResourceVerifier(folder.getText(), false));

		Button browseButton = new Button(folderSelectionComposite, SWT.PUSH);
		browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
		browseButton.setLayoutData(data);
		browseButton.addListener(SWT.Selection, event -> {
			DirectoryDialog fileDialog = new DirectoryDialog(ImportPanel.this.manager.getShell());
			fileDialog.setText(SVNUIMessages.ImportPanel_ImportFolder);
			fileDialog.setMessage(SVNUIMessages.ImportPanel_ImportFolder_Msg);
			String path = fileDialog.open();
			if (path != null) {
				locationField.setText(path);
			}
		});

		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout());
		data = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(data);
		group.setText(SVNUIMessages.ImportPanel_Comment);

		comment = new CommentComposite(group, this);
		data = new GridData(GridData.FILL_BOTH);
		comment.setLayoutData(data);

		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		separator.setVisible(false);

		depthSelector = new DepthSelectionComposite(parent, SWT.NONE, false);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		depthSelector.setLayoutData(data);
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.remote_importDialogContext"; //$NON-NLS-1$
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(525, SWT.DEFAULT);
	}

	@Override
	public void postInit() {
		super.postInit();
		comment.postInit(manager);
	}

	@Override
	protected void saveChangesImpl() {
		location = locationField.getText();
		comment.saveChanges();
	}

	@Override
	protected void cancelChangesImpl() {
		comment.cancelChanges();
	}

	public String getLocation() {
		return location;
	}

	public String getMessage() {
		return comment.getMessage();
	}

	public SVNDepth getDepth() {
		return depthSelector.getDepth();
	}

}
