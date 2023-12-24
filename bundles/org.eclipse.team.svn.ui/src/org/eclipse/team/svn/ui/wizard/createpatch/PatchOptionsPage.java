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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.local.CreatePatchOperation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.verifier.AbstractFormattedVerifier;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.wizard.AbstractVerifiedWizardPage;
import org.eclipse.ui.PlatformUI;

/**
 * Create patch options page
 * 
 * @author Alexander Gurov
 */
public class PatchOptionsPage extends AbstractVerifiedWizardPage {
	protected boolean recursive;

	protected boolean ignoreDeleted;

	protected boolean processBinary;

	protected boolean processUnversioned;

	protected boolean localMode;

	protected boolean showIgnoreAncestry;

	protected boolean ignoreAncestry;

	protected int rootPoint;

	protected boolean multiSelect;

	protected long diffOutputOptions;

	protected Button rootSelection;

	protected Button rootProject;

	protected Button rootWorkspace;

	public PatchOptionsPage(boolean localMode) {
		this(localMode, false);
	}

	public PatchOptionsPage(boolean localMode, boolean showIgnoreAncestry) {
		super(PatchOptionsPage.class.getName(), SVNUIMessages.PatchOptionsPage_Title,
				SVNTeamUIPlugin.instance().getImageDescriptor("icons/wizards/newconnect.gif")); //$NON-NLS-1$
		setDescription(SVNUIMessages.PatchOptionsPage_Description);
		this.localMode = localMode;
		this.showIgnoreAncestry = showIgnoreAncestry;
		ignoreAncestry = true;
	}

	public void setMultiSelect(boolean multiSelect) {
		if (localMode && this.multiSelect != multiSelect) {
			rootWorkspace.setSelection(multiSelect);
			rootProject.setSelection(!multiSelect);
			rootProject.setEnabled(!multiSelect);
			rootSelection.setSelection(false);
			rootSelection.setEnabled(!multiSelect);
		}
		rootPoint = multiSelect ? CreatePatchOperation.WORKSPACE : CreatePatchOperation.PROJECT;
		this.multiSelect = multiSelect;
	}

	public int getRootPoint() {
		return rootPoint;
	}

	public boolean isIgnoreDeleted() {
		return ignoreDeleted;
	}

	public boolean isProcessBinary() {
		return processBinary;
	}

	public boolean isProcessUnversioned() {
		return processUnversioned;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public boolean isIgnoreAncestry() {
		return showIgnoreAncestry ? ignoreAncestry : true;
	}

	public long getDiffOutputOptions() {
		return diffOutputOptions;
	}

	@Override
	protected Composite createControlImpl(Composite parent) {
		GridLayout layout = null;
		GridData data = null;

		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 4;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);

		Group options = new Group(composite, SWT.NONE);
		options.setText(SVNUIMessages.PatchOptionsPage_Options);
		layout = new GridLayout();
		options.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		options.setLayoutData(data);

		Button recursiveButton = new Button(options, SWT.CHECK);
		data = new GridData();
		recursiveButton.setLayoutData(data);
		recursiveButton.setText(SVNUIMessages.PatchOptionsPage_Recurse);
		recursiveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				recursive = ((Button) e.widget).getSelection();
			}
		});
		recursiveButton.setSelection(recursive = true);

		Button processBinaryButton = new Button(options, SWT.CHECK);
		data = new GridData();
		processBinaryButton.setLayoutData(data);
		processBinaryButton.setText(SVNUIMessages.PatchOptionsPage_Binary);
		processBinaryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				processBinary = ((Button) e.widget).getSelection();
				PatchOptionsPage.this.validateContent();
			}
		});
		processBinaryButton.setSelection(processBinary = false);
		AbstractVerifier verifier = new AbstractFormattedVerifier(
				SVNUIMessages.PatchOptionsPage_ProcessBinary_Verifier) {
			@Override
			protected String getErrorMessageImpl(Control input) {
				return null;
			}

			@Override
			protected String getWarningMessageImpl(Control input) {
				if (((Button) input).getSelection()) {
					return BaseMessages.format(SVNUIMessages.PatchOptionsPage_ProcessBinary_Verifier_Warning,
							new String[] { AbstractFormattedVerifier.FIELD_NAME });
				}
				return null;
			}
		};
		attachTo(processBinaryButton, verifier);

		Button processDeletedButton = new Button(options, SWT.CHECK);
		data = new GridData();
		processDeletedButton.setLayoutData(data);
		processDeletedButton.setText(SVNUIMessages.PatchOptionsPage_Deleted);
		processDeletedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ignoreDeleted = !((Button) e.widget).getSelection();
			}
		});
		processDeletedButton.setSelection(!(ignoreDeleted = false));

		if (localMode) {
			Button processUnversionedButton = new Button(options, SWT.CHECK);
			data = new GridData();
			processUnversionedButton.setLayoutData(data);
			processUnversionedButton.setText(SVNUIMessages.PatchOptionsPage_New);
			processUnversionedButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					processUnversioned = ((Button) e.widget).getSelection();
				}
			});
			processUnversionedButton.setSelection(processUnversioned = true);
		}

		if (showIgnoreAncestry) {
			final Button showIgnoreAncestryButton = new Button(options, SWT.CHECK);
			data = new GridData();
			showIgnoreAncestryButton.setLayoutData(data);
			showIgnoreAncestryButton.setText(SVNUIMessages.PatchOptionsPage_Ancestry);
			showIgnoreAncestryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					ignoreAncestry = showIgnoreAncestryButton.getSelection();
				}
			});
			showIgnoreAncestryButton.setSelection(ignoreAncestry);
		}

		if (CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_8_x) {
			Group outputOptions = new Group(composite, SWT.NONE);
			outputOptions.setText(SVNUIMessages.PatchOptionsPage_DiffOutputOptions);
			layout = new GridLayout();
			outputOptions.setLayout(layout);
			data = new GridData(GridData.FILL_HORIZONTAL);
			outputOptions.setLayoutData(data);

			Button ignoreWhitespaceButton = new Button(outputOptions, SWT.CHECK);
			data = new GridData();
			ignoreWhitespaceButton.setLayoutData(data);
			ignoreWhitespaceButton.setText(SVNUIMessages.PatchOptionsPage_IgnoreWhitespace);
			ignoreWhitespaceButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (((Button) e.widget).getSelection()) {
						diffOutputOptions |= ISVNConnector.DiffOptions.IGNORE_WHITESPACE;
					} else {
						diffOutputOptions &= ~ISVNConnector.DiffOptions.IGNORE_WHITESPACE;
					}
				}
			});
			ignoreWhitespaceButton.setSelection((diffOutputOptions & ISVNConnector.DiffOptions.IGNORE_WHITESPACE) != 0);

			Button ignoreSpaceChangeButton = new Button(outputOptions, SWT.CHECK);
			data = new GridData();
			ignoreSpaceChangeButton.setLayoutData(data);
			ignoreSpaceChangeButton.setText(SVNUIMessages.PatchOptionsPage_IgnoreSpaceChange);
			ignoreSpaceChangeButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (((Button) e.widget).getSelection()) {
						diffOutputOptions |= ISVNConnector.DiffOptions.IGNORE_SPACE_CHANGE;
					} else {
						diffOutputOptions &= ~ISVNConnector.DiffOptions.IGNORE_SPACE_CHANGE;
					}
				}
			});
			ignoreSpaceChangeButton
					.setSelection((diffOutputOptions & ISVNConnector.DiffOptions.IGNORE_SPACE_CHANGE) != 0);

			Button ignoreEOLStyleButton = new Button(outputOptions, SWT.CHECK);
			data = new GridData();
			ignoreEOLStyleButton.setLayoutData(data);
			ignoreEOLStyleButton.setText(SVNUIMessages.PatchOptionsPage_IgnoreEOLStyle);
			ignoreEOLStyleButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (((Button) e.widget).getSelection()) {
						diffOutputOptions |= ISVNConnector.DiffOptions.IGNORE_EOL_STYLE;
					} else {
						diffOutputOptions &= ~ISVNConnector.DiffOptions.IGNORE_EOL_STYLE;
					}
				}
			});
			ignoreEOLStyleButton.setSelection((diffOutputOptions & ISVNConnector.DiffOptions.IGNORE_EOL_STYLE) != 0);

			Button showCFunctionButton = new Button(outputOptions, SWT.CHECK);
			data = new GridData();
			showCFunctionButton.setLayoutData(data);
			showCFunctionButton.setText(SVNUIMessages.PatchOptionsPage_ShowCFunction);
			showCFunctionButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (((Button) e.widget).getSelection()) {
						diffOutputOptions |= ISVNConnector.DiffOptions.SHOW_FUNCTION;
					} else {
						diffOutputOptions &= ~ISVNConnector.DiffOptions.SHOW_FUNCTION;
					}
				}
			});
			showCFunctionButton.setSelection((diffOutputOptions & ISVNConnector.DiffOptions.SHOW_FUNCTION) != 0);

			Button useGITFormatButton = new Button(outputOptions, SWT.CHECK);
			data = new GridData();
			useGITFormatButton.setLayoutData(data);
			useGITFormatButton.setText(SVNUIMessages.PatchOptionsPage_GITFormat);
			useGITFormatButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (((Button) e.widget).getSelection()) {
						diffOutputOptions |= ISVNConnector.DiffOptions.GIT_FORMAT;
					} else {
						diffOutputOptions &= ~ISVNConnector.DiffOptions.GIT_FORMAT;
					}
				}
			});
			useGITFormatButton.setSelection((diffOutputOptions & ISVNConnector.DiffOptions.GIT_FORMAT) != 0);
		}

		if (localMode) {
			Group patchRoot = new Group(composite, SWT.NONE);
			patchRoot.setText(SVNUIMessages.PatchOptionsPage_PatchRoot);
			layout = new GridLayout();
			patchRoot.setLayout(layout);
			data = new GridData(GridData.FILL_HORIZONTAL);
			patchRoot.setLayoutData(data);

			rootWorkspace = new Button(patchRoot, SWT.RADIO);
			data = new GridData();
			rootWorkspace.setLayoutData(data);
			rootWorkspace.setText(SVNUIMessages.PatchOptionsPage_PatchRootWorkspace);
			rootWorkspace.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					rootPoint = CreatePatchOperation.WORKSPACE;
				}
			});

			rootProject = new Button(patchRoot, SWT.RADIO);
			data = new GridData();
			rootProject.setLayoutData(data);
			rootProject.setText(SVNUIMessages.PatchOptionsPage_PatchRootProject);
			rootProject.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					rootPoint = CreatePatchOperation.PROJECT;
				}
			});

			rootSelection = new Button(patchRoot, SWT.RADIO);
			data = new GridData();
			rootSelection.setLayoutData(data);
			rootSelection.setText(SVNUIMessages.PatchOptionsPage_PatchRootSelection);
			rootSelection.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					rootPoint = CreatePatchOperation.SELECTION;
				}
			});
			rootWorkspace.setSelection(multiSelect);
			rootProject.setSelection(!multiSelect);
			rootProject.setEnabled(!multiSelect);
			rootSelection.setSelection(false);
			rootSelection.setEnabled(!multiSelect);
		}

//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, "org.eclipse.team.svn.help.patchOptionsContext"); //$NON-NLS-1$

		return composite;
	}

}
