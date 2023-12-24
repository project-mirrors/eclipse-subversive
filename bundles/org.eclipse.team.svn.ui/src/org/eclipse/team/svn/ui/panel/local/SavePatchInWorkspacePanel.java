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

package org.eclipse.team.svn.ui.panel.local;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.panel.IDialogManager;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourceNameVerifier;
import org.eclipse.ui.internal.ide.misc.ContainerContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * The panel allows to select where new file will be placed in workspace
 * 
 * @author Alexander Gurov
 */
public class SavePatchInWorkspacePanel extends AbstractDialogPanel {
	protected TreeViewer treeViewer;

	protected Text workspaceFilenameField;

	protected String proposedName;

	protected IFile file;

	protected IProject proposedDestination;

	public SavePatchInWorkspacePanel(String proposedName, IProject proposedDestination) {
		dialogTitle = SVNUIMessages.SavePatchInWorkspace_Title;
		dialogDescription = SVNUIMessages.SavePatchInWorkspace_Description;
		defaultMessage = SVNUIMessages.SavePatchInWorkspace_Message;
		this.proposedName = proposedName;
		this.proposedDestination = proposedDestination;
	}

	public IFile getFile() {
		return file;
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.savePatchInWorkspaceContext"; //$NON-NLS-1$
	}

	@Override
	public void initPanel(IDialogManager manager) {
		super.initPanel(manager);

		treeViewer.setSelection(new StructuredSelection(proposedDestination));
		workspaceFilenameField.setText(proposedName);
	}

	@Override
	protected Point getPrefferedSizeImpl() {
		return new Point(640, 300);
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		GridData data = null;
		GridLayout layout = null;

		treeViewer = new TreeViewer(parent, SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		treeViewer.getTree().setLayoutData(data);
		ContainerContentProvider cp = new ContainerContentProvider();
		cp.showClosedProjects(false);
		treeViewer.setContentProvider(cp);
		treeViewer.setLabelProvider(new WorkbenchLabelProvider());
		treeViewer.setInput(ResourcesPlugin.getWorkspace());
		//new TreeItem(parent, style)
		//this.treeViewer.getTree().select()
		treeViewer.addSelectionChangedListener(event -> SavePatchInWorkspacePanel.this.validateContent());
		AbstractFormattedVerifier verifier = new AbstractFormattedVerifier(
				SVNUIMessages.SavePatchInWorkspace_WorkspaceTree) {
			@Override
			protected String getWarningMessageImpl(Control input) {
				return null;
			}

			@Override
			protected String getErrorMessageImpl(Control input) {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				return selection != null && !selection.isEmpty()
						? null
						: SVNUIMessages.SavePatchInWorkspace_WorkspaceTree_Verifier_Error;
			}
		};
		attachTo(treeViewer.getTree(), verifier);

		Composite wsComposite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		wsComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		wsComposite.setLayoutData(data);

		Label description = new Label(wsComposite, SWT.NONE);
		data = new GridData();
		description.setLayoutData(data);
		description.setText(SVNUIMessages.SavePatchInWorkspace_FileName);

		workspaceFilenameField = new Text(wsComposite, SWT.BORDER | SWT.SINGLE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		workspaceFilenameField.setLayoutData(data);
		CompositeVerifier cVerifier = new CompositeVerifier();
		String name = SVNUIMessages.SavePatchInWorkspace_FileName_Verifier;
		cVerifier.add(new NonEmptyFieldVerifier(name));
		cVerifier.add(new ResourceNameVerifier(name, false));
		cVerifier.add(new AbstractFormattedVerifier(name) {
			@Override
			protected String getErrorMessageImpl(Control input) {
				return null;
			}

			@Override
			protected String getWarningMessageImpl(Control input) {
				IFile file = SavePatchInWorkspacePanel.this.makeFile();
				if (file != null && file.isAccessible()) {
					return BaseMessages.format(SVNUIMessages.SavePatchInWorkspace_FileName_Verifier_Warning,
							new String[] { AbstractFormattedVerifier.FIELD_NAME });
				}
				return null;
			}
		});
		attachTo(workspaceFilenameField, cVerifier);
	}

	@Override
	protected void cancelChangesImpl() {

	}

	@Override
	protected void saveChangesImpl() {
		file = makeFile();
	}

	protected IFile makeFile() {
		String fileName = workspaceFilenameField.getText();
		IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			IContainer treeNode = (IContainer) selection.getFirstElement();
			return treeNode.getProject().getFile(treeNode.getFullPath().append(fileName).removeFirstSegments(1));
		}
		return null;
	}

}
