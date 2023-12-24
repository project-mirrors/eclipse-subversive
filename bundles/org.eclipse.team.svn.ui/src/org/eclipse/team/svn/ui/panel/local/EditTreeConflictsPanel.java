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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.local;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor.Operation;
import org.eclipse.team.svn.core.connector.SVNConflictVersion;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.GetLogMessagesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.panel.common.SVNHistoryPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Edit tree conflicts panel
 * 
 * @author Igor Burilo
 */
public class EditTreeConflictsPanel extends AbstractDialogPanel {

	protected ILocalResource local;

	protected EditTreeConflictsHelper helper;

	protected IActionOperation operation;

	protected Button localResolutionButton;

	protected Button remoteResolutionButton;

	protected Button manualResolutionButton;

	protected Button markAsMergedButton;

	public EditTreeConflictsPanel(ILocalResource local) {
		this.local = local;
		helper = new EditTreeConflictsHelper(this.local);

		dialogTitle = SVNUIMessages.EditTreeConflictsPanel_Title;
		dialogDescription = SVNUIMessages.EditTreeConflictsPanel_Description;
		defaultMessage = SVNUIMessages.EditTreeConflictsPanel_DefaultMessage;
	}

	@Override
	protected void createControlsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 1;
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayout(layout);
		composite.setLayoutData(data);

		createConflictInfoControls(composite);
		createConflictResolutionControls(composite);
	}

	protected void createConflictInfoControls(Composite parent) {
		Group composite = new Group(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayout(layout);
		composite.setLayoutData(data);
		composite.setText(SVNUIMessages.EditTreeConflictsPanel_ConlictInfo_Group);

		//operation
		Label label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.EditTreeConflictsPanel_Operation_Label);

		label = new Label(composite, SWT.NULL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(helper.getOperationAsString());

		//local status
		label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.EditTreeConflictsPanel_LocalStatus_Label);

		label = new Label(composite, SWT.NULL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(helper.getReasonAsString());

		//remote action
		label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.EditTreeConflictsPanel_IncomingAction_Label);

		label = new Label(composite, SWT.NULL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(helper.getActionAsString());

		//srcLeft
		label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.EditTreeConflictsPanel_StartVersion_Label);

		label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		SVNConflictVersion cVersionLeft = local.getTreeConflictDescriptor().srcLeftVersion;
		SVNConflictVersion cVersionRight = local.getTreeConflictDescriptor().srcRightVersion;
		label.setText(BaseMessages.format(SVNUIMessages.EditTreeConflictsPanel_revision,
				String.valueOf(cVersionLeft != null ? cVersionLeft.pegRevision : SVNRevision.INVALID_REVISION_NUMBER)));

		String leftUrl = helper.getSrcUrl(true);
		if (local.getTreeConflictDescriptor().operation == Operation.MERGE
				|| local.getTreeConflictDescriptor().operation == Operation.SWITCHED) {
			Link leftLink = new Link(composite, SWT.NULL);
			leftLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			leftLink.setText("<a>" + leftUrl + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			leftLink.addListener(SWT.Selection, event -> EditTreeConflictsPanel.this.showHistoryPage(true));
		} else {
			label = new Label(composite, SWT.NULL);
			label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			label.setText(leftUrl);
		}

		//srcRight
		label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.EditTreeConflictsPanel_EndRevision_Label);

		label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(BaseMessages.format(SVNUIMessages.EditTreeConflictsPanel_revision, String
				.valueOf(cVersionRight != null ? cVersionRight.pegRevision : SVNRevision.INVALID_REVISION_NUMBER)));

		Link rightLink = new Link(composite, SWT.NULL);
		rightLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		rightLink.setText("<a>" + helper.getSrcUrl(false) + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		rightLink.addListener(SWT.Selection, event -> EditTreeConflictsPanel.this.showHistoryPage(false));
	}

	protected void showHistoryPage(boolean isLeft) {
		boolean stopOnCopy = true;
		IRepositoryResource rr = helper.getRepositoryResourceForHistory(isLeft);

		SVNConflictVersion cVersionRight = local.getTreeConflictDescriptor().srcRightVersion;
		long currentRevision = cVersionRight != null ? cVersionRight.pegRevision : SVNRevision.INVALID_REVISION_NUMBER;
		GetLogMessagesOperation msgsOp = SVNHistoryPanel.getMsgsOp(rr, stopOnCopy);

		if (!UIMonitorUtility.doTaskNowDefault(UIMonitorUtility.getShell(), msgsOp, true).isCancelled()
				&& msgsOp.getExecutionState() == IActionOperation.OK) {
			SVNHistoryPanel historyPanel = new SVNHistoryPanel(SVNUIMessages.SVNHistoryPanel_Title,
					SVNUIMessages.SVNHistoryPanel_Description, SVNUIMessages.SVNHistoryPanel_Message, msgsOp, true,
					false, currentRevision);
			DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getShell(), historyPanel);
			dialog.open();
		}
	}

	protected void createConflictResolutionControls(Composite parent) {
		Group composite = new Group(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		//layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 1;
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayout(layout);
		composite.setLayoutData(data);
		composite.setText(SVNUIMessages.EditTreeConflictsPanel_Conflict_Resolution_Group);

		//tips section
		String tip = helper.getTip();
		if (tip != null) {
			Label tipLabel = new Label(composite, SWT.NONE);
			tipLabel.setLayoutData(new GridData());
			tipLabel.setText(SVNUIMessages.EditTreeConflictsPanel_Tips_Label);
			tipLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));

			Label tipValue = new Label(composite, SWT.WRAP);
			tipValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			tipValue.setText(tip);
		}

		localResolutionButton = new Button(composite, SWT.RADIO);
		localResolutionButton.setLayoutData(new GridData());
		localResolutionButton.setText(SVNUIMessages.EditTreeConflictsPanel_ApplyLocalChanges_Resolution);
		localResolutionButton.addListener(SWT.Selection, event -> EditTreeConflictsPanel.this.changeResolutionSelection());

		remoteResolutionButton = new Button(composite, SWT.RADIO);
		remoteResolutionButton.setLayoutData(new GridData());
		remoteResolutionButton.setText(SVNUIMessages.EditTreeConflictsPanel_ApplyIncomigChanges_Resolution);
		remoteResolutionButton.addListener(SWT.Selection, event -> EditTreeConflictsPanel.this.changeResolutionSelection());

		manualResolutionButton = new Button(composite, SWT.RADIO);
		manualResolutionButton.setLayoutData(new GridData());
		manualResolutionButton.setText(SVNUIMessages.EditTreeConflictsPanel_ManualResolution);
		manualResolutionButton.addListener(SWT.Selection, event -> EditTreeConflictsPanel.this.changeResolutionSelection());

		markAsMergedButton = new Button(composite, SWT.CHECK);
		markAsMergedButton.setLayoutData(new GridData());
		markAsMergedButton.setText(SVNUIMessages.EditTreeConflictsPanel_MarkAsMerged_Button);
	}

	@Override
	public void postInit() {
		super.postInit();

		manualResolutionButton.setSelection(true);
		changeResolutionSelection();
	}

	protected void changeResolutionSelection() {
		if (localResolutionButton.getSelection()) {
			markAsMergedButton.setSelection(true);
			markAsMergedButton.setEnabled(false);
		} else if (manualResolutionButton.getSelection()) {
			markAsMergedButton.setSelection(false);
			markAsMergedButton.setEnabled(true);
		} else if (remoteResolutionButton.getSelection()) {
			markAsMergedButton.setSelection(true);
			markAsMergedButton.setEnabled(!helper.isRemoteOperationResolveTheConflict());
		}
	}

	@Override
	protected void saveChangesImpl() {
		operation = helper.getOperation(remoteResolutionButton.getSelection(), localResolutionButton.getSelection(),
				markAsMergedButton.getSelection());
	}

	@Override
	protected void cancelChangesImpl() {
		operation = null;
	}

	public IActionOperation getOperation() {
		return operation;
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.editTreeConflictsContext"; //$NON-NLS-1$
	}
}
