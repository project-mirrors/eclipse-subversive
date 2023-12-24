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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.CommentComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.ExistingResourceVerifier;

/**
 * create Remote File Panel
 * 
 * @author Sergiy Logvin
 */
public class CreateFilePanel extends AbstractDialogPanel {
	protected Text locationField;

	protected String location;

	protected CommentComposite comment;

	protected String[] fileNames;

	public CreateFilePanel(String importToUrl) {
		dialogTitle = SVNUIMessages.CreateFilePanel_Title;
		dialogDescription = SVNUIMessages.CreateFilePanel_Description;
		defaultMessage = BaseMessages.format(SVNUIMessages.CreateFilePanel_Message, new String[] { importToUrl });
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite fileGroup = new Composite(parent, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		fileGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fileGroup.setLayoutData(data);

		Label fileLabel = new Label(fileGroup, SWT.NONE);
		data = new GridData();
		fileLabel.setLayoutData(data);
		fileLabel.setText(SVNUIMessages.CreateFilePanel_FilePath);

		locationField = new Text(fileGroup, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		locationField.setLayoutData(data);
		locationField.addModifyListener(e -> {
			String text = locationField.getText();
			if (text.indexOf("\"") == -1) { //$NON-NLS-1$
				location = new Path(text).removeLastSegments(1).toString();
			}
		});
		attachTo(locationField, new ExistingResourceMultiVerifier(SVNUIMessages.CreateFilePanel_FilePath_Verifier));
		Button browseButton = new Button(fileGroup, SWT.PUSH);
		browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
		browseButton.setLayoutData(data);
		browseButton.addListener(SWT.Selection, event -> {
			FileDialog fileDialog = new FileDialog(CreateFilePanel.this.manager.getShell(), SWT.MULTI);
			fileDialog.setText(SVNUIMessages.CreateFilePanel_ImportFile);
			String path = fileDialog.open();
			if (path != null) {
				String[] fileNames = fileDialog.getFileNames();
				location = new Path(path).removeLastSegments(1).toString();
				CreateFilePanel.this.fileNames = fileNames;
				if (fileNames.length > 1) {
					String text = ""; //$NON-NLS-1$
					for (String fileName : fileNames) {
						text += text.length() > 0 ? " \"" + fileName + "\"" : "\"" + fileName + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					}
					locationField.setText(text);
				} else {
					locationField.setText(path);
				}
			}
		});

		Group group = new Group(parent, SWT.NULL);
		group.setLayout(new GridLayout());
		data = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(data);
		group.setText(SVNUIMessages.CreateFilePanel_Comment);

		comment = new CommentComposite(group, this);
		data = new GridData(GridData.FILL_BOTH);
		comment.setLayoutData(data);
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
		comment.saveChanges();
		String text = CreateFilePanel.this.locationField.getText();
		if (text.indexOf("\"") > -1) { //$NON-NLS-1$
			fileNames = parseFileNames(text);
		} else {
			Path path = new Path(text);
			location = path.removeLastSegments(1).toString();
			fileNames = new String[] { path.lastSegment() };
		}
	}

	protected String[] parseFileNames(String text) {
		Reader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes())));
		final StreamTokenizer tokenizer = new StreamTokenizer(r);
		tokenizer.resetSyntax();
		tokenizer.quoteChar('\"');
		tokenizer.whitespaceChars(32, 32);
		final Set<String> fileNames = new HashSet<>();

		new AbstractActionOperation("Operation_ParseFile", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
					fileNames.add(tokenizer.sval);
				}
			}

			@Override
			public ISchedulingRule getSchedulingRule() {
				return null;
			}
		}.run(new NullProgressMonitor());

		return fileNames.toArray(new String[fileNames.size()]);
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.remote_createFileDialogContext"; //$NON-NLS-1$
	}

	@Override
	protected void cancelChangesImpl() {
		comment.cancelChanges();
	}

	public String getLocation() {
		return location;
	}

	public String[] getFileNames() {
		return fileNames;
	}

	public String getMessage() {
		return comment.getMessage();
	}

	protected class ExistingResourceMultiVerifier extends ExistingResourceVerifier {
		protected String ERROR_MESSAGE_DOES_NOT_EXIST_MULTIPLE;

		protected String ERROR_MESSAGE_IS_NOT_A_FILE_MULTIPLE;

		public ExistingResourceMultiVerifier(String fieldName) {
			super(fieldName, true);
			ERROR_MESSAGE_DOES_NOT_EXIST_MULTIPLE = BaseMessages.format(
					SVNUIMessages.CreateFilePanel_FilePath_Verifier_Error_Exists,
					new String[] { AbstractFormattedVerifier.FIELD_NAME });
			ERROR_MESSAGE_IS_NOT_A_FILE_MULTIPLE = BaseMessages.format(
					SVNUIMessages.CreateFilePanel_FilePath_Verifier_Error_NotAFile,
					new String[] { AbstractFormattedVerifier.FIELD_NAME });
		}

		@Override
		protected String getErrorMessageImpl(Control input) {
			if (location == null) {
				return ExistingResourceVerifier.ERROR_MESSAGE_DOES_NOT_EXIST;
			}
			boolean existAll = true;
			boolean allFiles = true;
			String text = getText(input);
			if (text.indexOf("\"") > -1) { //$NON-NLS-1$
				String[] fileNames = parseFileNames(text);
				for (String fileName : fileNames) {
					if (fileName == null || fileName.trim().length() == 0) {
						existAll = false;
						break;
					}
					String pathToFile = new Path(location).append(fileName).toString();
					File file = new File(pathToFile);
					existAll = existAll && file.exists();
					allFiles = allFiles && file.isFile();
					if (!existAll || !allFiles) {
						break;
					}
				}
				if (!existAll) {
					return ERROR_MESSAGE_DOES_NOT_EXIST_MULTIPLE;
				} else if (!allFiles) {
					return ERROR_MESSAGE_IS_NOT_A_FILE_MULTIPLE;
				}
			} else {
				return super.getErrorMessageImpl(input);
			}
			return null;
		}

	}

}
