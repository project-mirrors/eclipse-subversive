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

package org.eclipse.team.svn.ui.wizard.createpatch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.SavePatchInWorkspacePanel;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;
import org.eclipse.team.svn.ui.verifier.ResourcePathVerifier;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.team.svn.ui.wizard.CreatePatchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Select patch file wizard page
 * 
 * @author Alexander Gurov
 */
public class SelectPatchFilePage extends AbstractVerifiedWizardPage {
	protected Text fileNameField;

	protected Text wsPathField;

	protected Button browseButton;

	protected Button browseWSButton;

	protected Combo charsetField;

	protected String proposedName;

	protected String charset;

	protected String fileName;

	protected IFile file;

	protected int writeMode;

	protected CheckboxTreeViewer changeViewer;

	protected IResource[] roots;

	protected Object[] initialSelection;

	protected Object[] realSelection;

	public SelectPatchFilePage(String proposedName, IResource[] roots) {
		super(
				SelectPatchFilePage.class.getName(), SVNUIMessages.SelectPatchFilePage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		setDescription(SVNUIMessages.SelectPatchFilePage_Description);
		this.proposedName = proposedName + ".patch"; //$NON-NLS-1$
		writeMode = CreatePatchWizard.WRITE_TO_CLIPBOARD;
		// filter out nested project's resources if there are more than 1 entry for the physical resource
		if (roots != null) {
			LinkedHashMap<IResource, Boolean> resourceSet = new LinkedHashMap<>();
			for (IResource resource : roots) {
				resourceSet.put(resource, Boolean.FALSE);
			}
			for (IResource resource : roots) {
				IPath path = FileUtility.getResourcePath(resource);
				for (IResource checkAgainst : roots) {
					if (resource != checkAgainst && path.isPrefixOf(FileUtility.getResourcePath(checkAgainst))) {
						resourceSet.put(checkAgainst, Boolean.TRUE);
					}
				}
			}
			ArrayList<IResource> filteredRoots = new ArrayList<>();
			for (Map.Entry<IResource, Boolean> entry : resourceSet.entrySet()) {
				if (!entry.getValue().booleanValue()) {
					filteredRoots.add(entry.getKey());
				}
			}
			this.roots = filteredRoots.toArray(new IResource[filteredRoots.size()]);
		}
		try {
			File tmp = File.createTempFile("patch", "tmp"); //$NON-NLS-1$ //$NON-NLS-2$
			tmp.delete();
			SelectPatchFilePage.this.fileName = tmp.getAbsolutePath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String getCharset() {
		return charset;
	}

	public boolean isRecursive() {
		if (initialSelection == null) {
			return true;
		}
		HashSet result = new HashSet(Arrays.asList(initialSelection));
		result.removeAll(Arrays.asList(realSelection));
		return result.isEmpty();
	}

	public IResource[] getSelection() {
		return Arrays.asList(realSelection).toArray(new IResource[realSelection.length]);
	}

	public IFile getFile() {
		return file;
	}

	public String getFileName() {
		return fileName;
	}

	public int getWriteMode() {
		return writeMode;
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Composite saveTo = composite;

		if (roots != null) {
			Group saveToImpl = new Group(composite, SWT.NONE);
			layout = new GridLayout();
			saveToImpl.setLayout(layout);
			data = new GridData(GridData.FILL_HORIZONTAL);
			saveToImpl.setLayoutData(data);
			saveToImpl.setText(SVNUIMessages.SelectPatchFilePage_SaveTo);
			saveTo = saveToImpl;
		}

		Button saveToClipboard = new Button(saveTo, SWT.RADIO);
		saveToClipboard.setText(SVNUIMessages.SelectPatchFilePage_SaveToClipboard);
		data = new GridData(GridData.FILL_HORIZONTAL);
		saveToClipboard.setLayoutData(data);
		saveToClipboard.addListener(SWT.Selection, event -> {
			Button button = (Button) event.widget;
			if (button.getSelection()) {
				fileNameField.setEnabled(false);
				browseButton.setEnabled(false);
				browseWSButton.setEnabled(false);
				charsetField.setEnabled(true);
				try {
					fileName = File.createTempFile("patch", ".tmp").getAbsolutePath(); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				writeMode = CreatePatchWizard.WRITE_TO_CLIPBOARD;
				SelectPatchFilePage.this.validateContent();
			}
		});

		charsetField = new Combo(saveTo, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		charsetField.setLayoutData(data);
		String sysEnc = Charset.defaultCharset().displayName();
		String[] comboItems = "UTF-8".equalsIgnoreCase(sysEnc) //$NON-NLS-1$
				? new String[] { sysEnc }
				: new String[] { sysEnc, "UTF-8" }; //$NON-NLS-1$
		charset = comboItems[0];
		charsetField.setItems(comboItems);
		charsetField.select(0);
		charsetField.setVisibleItemCount(comboItems.length);
		charsetField.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				charset = charsetField.getText();
			}
		});
		charsetField.addModifyListener(e -> charset = charsetField.getText());

		final Button saveOnFileSystem = new Button(saveTo, SWT.RADIO);
		saveOnFileSystem.setText(SVNUIMessages.SelectPatchFilePage_SaveInFS);
		data = new GridData(GridData.FILL_HORIZONTAL);
		saveOnFileSystem.setLayoutData(data);
		saveOnFileSystem.addListener(SWT.Selection, event -> {
			Button button = (Button) event.widget;
			if (button.getSelection()) {
				fileNameField.setEnabled(true);
				browseButton.setEnabled(true);
				browseWSButton.setEnabled(false);
				charsetField.setEnabled(false);
				fileName = fileNameField.getText();
				writeMode = CreatePatchWizard.WRITE_TO_EXTERNAL_FILE;
				SelectPatchFilePage.this.validateContent();
			}
		});

		Composite fsComposite = new Composite(saveTo, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		fsComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fsComposite.setLayoutData(data);

		fileNameField = new Text(fsComposite, SWT.BORDER | SWT.SINGLE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fileNameField.setLayoutData(data);
		fileNameField.addModifyListener(e -> fileName = fileNameField.getText());
		CompositeVerifier cVerifier = new CompositeVerifier();
		String name = SVNUIMessages.SelectPatchFilePage_SaveInFS_Verifier;
		cVerifier.add(new NonEmptyFieldVerifier(name));
		cVerifier.add(new ResourcePathVerifier(name));
		cVerifier.add(new AbstractFormattedVerifier(name) {
			@Override
			protected String getErrorMessageImpl(Control input) {
				return null;
			}

			@Override
			protected String getWarningMessageImpl(Control input) {
				String text = getText(input);
				if (new File(text).exists()) {
					return BaseMessages.format(SVNUIMessages.SelectPatchFilePage_SaveInFS_Verifier_Warning,
							new String[] { AbstractFormattedVerifier.FIELD_NAME });
				}
				return null;
			}
		});
		attachTo(fileNameField, new AbstractVerifierProxy(cVerifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return saveOnFileSystem.getSelection();
			}
		});

		browseButton = new Button(fsComposite, SWT.PUSH);
		browseButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browseButton);
		browseButton.setLayoutData(data);
		browseButton.addListener(SWT.Selection, event -> {
			FileDialog dlg = new FileDialog(SelectPatchFilePage.this.getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
			dlg.setText(SVNUIMessages.SelectPatchFilePage_SavePatchAs);
			dlg.setFileName(proposedName);
			dlg.setFilterExtensions(new String[] { "*.patch", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
			String file = dlg.open();
			if (file != null) {
				fileName = file;
				fileNameField.setText(file);
				SelectPatchFilePage.this.validateContent();
			}
		});

		final Button saveInWorkspace = new Button(saveTo, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		saveInWorkspace.setLayoutData(data);
		saveInWorkspace.setText(SVNUIMessages.SelectPatchFilePage_SaveInWS);
		saveInWorkspace.addListener(SWT.Selection, event -> {
			Button button = (Button) event.widget;
			if (button.getSelection()) {
				fileNameField.setEnabled(false);
				browseButton.setEnabled(false);
				browseWSButton.setEnabled(true);
				charsetField.setEnabled(false);
				fileName = file == null ? null : FileUtility.getWorkingCopyPath(file);
				writeMode = CreatePatchWizard.WRITE_TO_WORKSPACE_FILE;
				SelectPatchFilePage.this.validateContent();
			}
		});

		Composite wsComposite = new Composite(saveTo, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		wsComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		wsComposite.setLayoutData(data);

		wsPathField = new Text(wsComposite, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		wsPathField.setLayoutData(data);
		cVerifier = new CompositeVerifier();
		name = SVNUIMessages.SelectPatchFilePage_SaveInWS_Verifier;
		cVerifier.add(new NonEmptyFieldVerifier(name));
		cVerifier.add(new AbstractFormattedVerifier(name) {
			@Override
			protected String getErrorMessageImpl(Control input) {
				return null;
			}

			@Override
			protected String getWarningMessageImpl(Control input) {
				if (file != null && file.isAccessible()) {
					return BaseMessages.format(SVNUIMessages.SelectPatchFilePage_SaveInWS_Verifier_Warning,
							new String[] { AbstractFormattedVerifier.FIELD_NAME });
				}
				return null;
			}
		});
		attachTo(wsPathField, new AbstractVerifierProxy(cVerifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return saveInWorkspace.getSelection();
			}
		});

		browseWSButton = new Button(wsComposite, SWT.PUSH);
		browseWSButton.setText(SVNUIMessages.Button_Browse);
		data = new GridData();
		data.widthHint = DefaultDialog.computeButtonWidth(browseWSButton);
		browseWSButton.setLayoutData(data);
		browseWSButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IProject proposedDestination = roots.length > 0 ? roots[0].getProject() : null;
				SavePatchInWorkspacePanel panel = new SavePatchInWorkspacePanel(proposedName, proposedDestination);
				DefaultDialog dlg = new DefaultDialog(SelectPatchFilePage.this.getShell(), panel);
				if (dlg.open() == 0) {
					file = panel.getFile();
					fileName = FileUtility.getWorkingCopyPath(file);
					wsPathField.setText(file.getFullPath().toString());
					SelectPatchFilePage.this.validateContent();
				}
			}
		});

		if (roots != null) {
			Label label = new Label(composite, SWT.NONE);
			data = new GridData();
			label.setLayoutData(data);
			label.setText(SVNUIMessages.SelectPatchFilePage_Changes);

			changeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);
			data = new GridData(GridData.FILL_BOTH);
			data.heightHint = 200;
			data.widthHint = 600;
			changeViewer.getControl().setLayoutData(data);
			changeViewer.setContentProvider(new WorkbenchContentProvider() {
				@Override
				public Object[] getChildren(Object element) {
					if (element instanceof IProject || element instanceof IFolder) {
						try {
							Object[] result = SVNRemoteStorage.instance().getRegisteredChildren((IContainer) element);
							return result != null ? result : new Object[0];
						} catch (Exception e) {
							// do nothing
						}
					}
					Object[] result = super.getChildren(element);
					return result != null ? result : new Object[0];
				}
			});
			changeViewer.setLabelProvider(new WorkbenchLabelProvider());
			changeViewer.addCheckStateListener(event -> {
				changeViewer.getControl().setRedraw(false);

				IResource resource = (IResource) event.getElement();
				HashSet grayed = new HashSet(Arrays.asList(changeViewer.getGrayedElements()));
				if (event.getChecked()) {
					if (resource.getType() != IResource.FILE) {
						changeViewer.setSubtreeChecked(resource, true);
						IPath path = resource.getFullPath();
						for (Object element : initialSelection) {
							IResource current = (IResource) element;
							if (path.isPrefixOf(current.getFullPath())) {
								grayed.remove(current);
							}
						}
					}
					while ((resource = resource.getParent()).getType() != IResource.ROOT) {
						boolean hasUnchecked = false;
						IPath path = resource.getFullPath();
						for (Object element : initialSelection) {
							IResource current = (IResource) element;
							if (path.isPrefixOf(current.getFullPath()) && !current.equals(resource)) {
								hasUnchecked |= !changeViewer.getChecked(current);
							}
						}
						if (!hasUnchecked) {
							grayed.remove(resource);
							changeViewer.setChecked(resource, true);
						}
					}
				} else {
					if (resource.getType() != IResource.FILE) {
						changeViewer.setSubtreeChecked(resource, false);
					}
					grayed.addAll(Arrays.asList(FileUtility.getPathNodes(resource)));
				}
				changeViewer.setGrayedElements(grayed.toArray());
				realSelection = changeViewer.getCheckedElements();

				changeViewer.getControl().setRedraw(true);
			});
			changeViewer.addFilter(new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (element instanceof IResource) {
						IResource resource = (IResource) element;
						IPath resourcePath = resource.getFullPath();
						for (IResource root : roots) {
							IPath rootPath = root.getFullPath();
							if ((rootPath.isPrefixOf(resourcePath) || resourcePath.isPrefixOf(rootPath))
									&& FileUtility.checkForResourcesPresenceRecursive(new IResource[] { resource },
											IStateFilter.SF_ANY_CHANGE)) {
								return true;
							}
						}
					}
					return false;
				}
			});
			changeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
			changeViewer.expandAll();
			TreeItem[] items = changeViewer.getTree().getItems();
			for (TreeItem item : items) {
				changeViewer.setSubtreeChecked(item.getData(), true);
			}
			realSelection = initialSelection = changeViewer.getCheckedElements();
		}

		fileNameField.setEnabled(false);
		browseButton.setEnabled(false);
		browseWSButton.setEnabled(false);
		saveToClipboard.setSelection(true);

//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, "org.eclipse.team.svn.help.patchFileContext"); //$NON-NLS-1$

		return composite;
	}

}
