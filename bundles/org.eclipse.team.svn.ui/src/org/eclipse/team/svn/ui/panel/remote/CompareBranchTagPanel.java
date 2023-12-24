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

package org.eclipse.team.svn.ui.panel.remote;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.BranchTagSelectionComposite;
import org.eclipse.team.svn.ui.composite.DiffFormatComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.InputHistory;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;

/**
 * Panel for the Compare With Branch/Tag dialog
 * 
 * @author Alexei Goncharov
 */
public class CompareBranchTagPanel extends AbstractDialogPanel {
	protected IRepositoryResource baseResource;

	protected int type;

	protected IRepositoryResource[] branchTagResources;

	protected long currentRevision;

	protected String historyKey;

	protected BranchTagSelectionComposite selectionComposite;

	protected DiffFormatComposite diffFormatComposite;

	protected Label resultText;

	protected long options;

	protected Button ignoreAncestryButton;

	protected InputHistory ignoreHistory;

	protected IRepositoryResource resourceToCompareWith;

	public CompareBranchTagPanel(IRepositoryResource baseResource, int type, IRepositoryResource[] branchTagResources) {
		this.baseResource = baseResource;
		this.type = type;
		this.branchTagResources = branchTagResources;
		if (type == BranchTagSelectionComposite.BRANCH_OPERATED) {
			dialogTitle = SVNUIMessages.Compare_Branch_Title;
			dialogDescription = SVNUIMessages.Compare_Branch_Description;
			defaultMessage = SVNUIMessages.Compare_Branch_Message;
			historyKey = "branchCompare"; //$NON-NLS-1$
		} else {
			dialogTitle = SVNUIMessages.Compare_Tag_Title;
			dialogDescription = SVNUIMessages.Compare_Tag_Description;
			defaultMessage = SVNUIMessages.Compare_Tag_Message;
			historyKey = "tagCompare"; //$NON-NLS-1$
		}
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		GridData data = null;
		selectionComposite = new BranchTagSelectionComposite(parent, SWT.NONE, baseResource, historyKey, this, type,
				branchTagResources);
		data = new GridData(GridData.FILL_HORIZONTAL);
		selectionComposite.setLayoutData(data);
		selectionComposite.setCurrentRevision(currentRevision);

		data = new GridData();
		ignoreAncestryButton = new Button(parent, SWT.CHECK);
		ignoreAncestryButton.setLayoutData(data);
		ignoreAncestryButton.setText(SVNUIMessages.MergePanel_Button_IgnoreAncestry);
		ignoreHistory = new InputHistory("ignoreAncestry", InputHistory.TYPE_BOOLEAN, //$NON-NLS-1$
				(options & ISVNConnector.Options.IGNORE_ANCESTRY) != 0);
		ignoreAncestryButton.setSelection((Boolean) ignoreHistory.getValue());

		diffFormatComposite = new DiffFormatComposite(parent, this);

		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(parent, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(data);
		label.setText(SVNUIMessages.CompareBranchTagPanel_ResultDescription);

		Composite resultComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 2;
		resultComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		resultComposite.setLayoutData(data);
		resultComposite.setBackground(UIMonitorUtility.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		resultText = new Label(resultComposite, SWT.SINGLE | SWT.WRAP);
		resultText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		resultText.setBackground(UIMonitorUtility.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		selectionComposite.addUrlModifyListener(event -> CompareBranchTagPanel.this.setResultLabel());
		selectionComposite.addUrlVerifier(new AbstractVerifier() {
			@Override
			protected String getErrorMessage(Control input) {
				/*
				 * As resourceToCompareWith may be not yet re-calculated, we do it explicitly here
				 */
				if (BranchTagSelectionComposite.getResourceToCompareWith(baseResource,
						CompareBranchTagPanel.this.getSelectedResource()) == null) {
					return SVNUIMessages.CompareBranchTagPanel_ConstructResultVerifierError;
				}
				return null;
			}

			@Override
			protected String getWarningMessage(Control input) {
				return null;
			}
		});

		setResultLabel();
	}

	protected void setResultLabel() {
		String text = SVNUIMessages.CompareBranchTagPanel_ResultNone;
		resourceToCompareWith = null;

		if (getSelectedResource() != null) {
			resourceToCompareWith = BranchTagSelectionComposite.getResourceToCompareWith(baseResource,
					getSelectedResource());
			if (resourceToCompareWith != null) {
				text = resourceToCompareWith.getUrl();
			}
		}
		resultText.setText(text);
	}

	public String getDiffFile() {
		return diffFormatComposite.getDiffFile();
	}

	public IRepositoryResource getResourceToCompareWith() {
		return resourceToCompareWith;
	}

	private IRepositoryResource getSelectedResource() {
		return selectionComposite.getSelectedResource();
	}

	public long getDiffOptions() {
		return options;
	}

	@Override
	protected void saveChangesImpl() {
		selectionComposite.saveChanges();
		options |= ignoreAncestryButton.getSelection()
				? ISVNConnector.Options.IGNORE_ANCESTRY
				: ISVNConnector.Options.NONE;
		ignoreHistory.setValue(ignoreAncestryButton.getSelection());
	}

	@Override
	protected void cancelChangesImpl() {
	}

}
