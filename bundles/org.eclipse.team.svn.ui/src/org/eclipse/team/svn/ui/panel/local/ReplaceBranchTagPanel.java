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

package org.eclipse.team.svn.ui.panel.local;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.BranchTagSelectionComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;

/**
 * Panel for the Replace With Branch/Tag dialog
 * 
 * @author Alexei Goncharov
 */
public class ReplaceBranchTagPanel extends AbstractDialogPanel {
	protected IRepositoryResource baseResource;

	protected int type;

	protected IRepositoryResource[] branchTagResources;

	protected long currentRevision;

	protected String historyKey;

	protected BranchTagSelectionComposite selectionComposite;

	protected Label resultText;

	protected IRepositoryResource resourceToReplaceWith;

	public ReplaceBranchTagPanel(IRepositoryResource baseResource, long currentRevision, int type,
			IRepositoryResource[] branchTagResources) {
		this.baseResource = baseResource;
		this.type = type;
		this.branchTagResources = branchTagResources;
		if (type == BranchTagSelectionComposite.BRANCH_OPERATED) {
			dialogTitle = SVNUIMessages.Replace_Branch_Title;
			dialogDescription = SVNUIMessages.Replace_Branch_Description;
			defaultMessage = SVNUIMessages.Replace_Branch_Message;
			historyKey = "branchReplace"; //$NON-NLS-1$
		} else {
			dialogTitle = SVNUIMessages.Replace_Tag_Title;
			dialogDescription = SVNUIMessages.Replace_Tag_Description;
			defaultMessage = SVNUIMessages.Replace_Tag_Message;
			historyKey = "tagReplace"; //$NON-NLS-1$
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

		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(parent, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(data);
		label.setText(SVNUIMessages.ReplaceBranchTagPanel_ResultDescription);

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

		selectionComposite.addUrlModifyListener(event -> ReplaceBranchTagPanel.this.setResultLabel());
		selectionComposite.addUrlVerifier(new AbstractVerifier() {
			@Override
			protected String getErrorMessage(Control input) {
				/*
				 * As resourceToReplaceWith may be not yet re-calculated, we do it explicitly here
				 */
				if (BranchTagSelectionComposite.getResourceToCompareWith(baseResource,
						ReplaceBranchTagPanel.this.getSelectedResource()) == null) {
					return SVNUIMessages.ReplaceBranchTagPanel_ConstructResultVerifierError;
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
		String text = ""; //$NON-NLS-1$
		resourceToReplaceWith = null;

		if (getSelectedResource() != null) {
			resourceToReplaceWith = BranchTagSelectionComposite.getResourceToCompareWith(baseResource,
					getSelectedResource());
			if (resourceToReplaceWith != null) {
				text = resourceToReplaceWith.getUrl();
			} else {
				text = SVNUIMessages.ReplaceBranchTagPanel_ResultNone;
			}
		}
		resultText.setText(text);
	}

	private IRepositoryResource getSelectedResource() {
		return selectionComposite.getSelectedResource();
	}

	public IRepositoryResource getResourceToReplaceWith() {
		return resourceToReplaceWith;
	}

	@Override
	protected void saveChangesImpl() {
		selectionComposite.saveChanges();
	}

	@Override
	protected void cancelChangesImpl() {
	}

}