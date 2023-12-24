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

package org.eclipse.team.svn.ui.panel.reporting;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.ReportingComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.extension.factory.IReporter;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Error or cancel panel
 *
 * @author Sergiy Logvin
 */
public class ErrorCancelPanel extends AbstractDialogPanel {
	protected static final int ERROR_PANEL_TYPE = 0;

	protected static final int CANCEL_PANEL_TYPE = 1;

	protected ReportingComposite reportingComposite;

	protected Text errorTextField;

	protected String optionName;

	protected String simpleMessage;

	protected String advancedMessage;

	protected int panelType;

	protected boolean sendMail;

	protected boolean isPluginError;

	protected IStatus errorStatus;

	protected String plugin;

	protected String originalReport;

	public ErrorCancelPanel(String title, int numberOfErrors, String simpleMessage, String advancedMessage,
			boolean sendMail, boolean isPlugInError, String optionName) {
		this(ErrorCancelPanel.ERROR_PANEL_TYPE, numberOfErrors, title, simpleMessage, advancedMessage, sendMail,
				isPlugInError, optionName);
	}

	public ErrorCancelPanel(String title, String simpleMessage, String advancedMessage, boolean sendMail,
			String optionName) {
		this(ErrorCancelPanel.CANCEL_PANEL_TYPE, 0, title, simpleMessage, advancedMessage, sendMail, false, optionName);
	}

	public ErrorCancelPanel(String title, int numberOfErrors, String simpleMessage, String advancedMessage,
			boolean sendMail, boolean isPlugInError, String optionName, IStatus errorStatus, String plugin) {
		this(ErrorCancelPanel.ERROR_PANEL_TYPE, numberOfErrors, title, simpleMessage, advancedMessage, sendMail,
				isPlugInError, optionName);
		this.errorStatus = errorStatus;
		this.plugin = plugin;
		isPluginError = isPlugInError;
	}

	public ErrorCancelPanel(String title, int numberOfErrors, String simpleMessage, String advancedMessage,
			boolean sendMail, boolean isPlugInError, String optionName, IStatus errorStatus, String plugin,
			String originalReport) {
		this(ErrorCancelPanel.ERROR_PANEL_TYPE, numberOfErrors, title, simpleMessage, advancedMessage, sendMail,
				isPlugInError, optionName);
		this.errorStatus = errorStatus;
		this.plugin = plugin;
		this.originalReport = originalReport;
	}

	protected ErrorCancelPanel(int panelType, int numberOfErrors, String title, String simpleMessage,
			String advancedMessage, boolean sendMail, boolean isPlugInError, String optionName) {
		super(sendMail
				? new String[] { SVNUIMessages.ErrorCancelPanel_Send, SVNUIMessages.ErrorCancelPanel_DontSend }
				: new String[] { IDialogConstants.OK_LABEL });
		isPluginError = isPlugInError;
		this.panelType = panelType;
		this.sendMail = sendMail;
		dialogTitle = panelType == ErrorCancelPanel.ERROR_PANEL_TYPE
				? SVNUIMessages.ErrorCancelPanel_Title_Failed
				: SVNUIMessages.ErrorCancelPanel_Title_Cancelled;
		if (title == null || title.length() == 0) {
			dialogDescription = panelType == ErrorCancelPanel.ERROR_PANEL_TYPE
					? SVNUIMessages.ErrorCancelPanel_Description_Failed_Empty
					: SVNUIMessages.ErrorCancelPanel_Description_Cancelled_Empty;
		} else {
			dialogDescription = BaseMessages.format(panelType == ErrorCancelPanel.ERROR_PANEL_TYPE
					? SVNUIMessages.ErrorCancelPanel_Description_Failed
					: SVNUIMessages.ErrorCancelPanel_Description_Cancelled, new String[] { title });
		}
		if (sendMail) {
			defaultMessage = SVNUIMessages.ErrorCancelPanel_Message_Send;
		} else if (panelType == ErrorCancelPanel.ERROR_PANEL_TYPE) {
			if (numberOfErrors == 1) {
				defaultMessage = SVNUIMessages.ErrorCancelPanel_Message_DontSend_Single;
			} else {
				defaultMessage = BaseMessages.format(SVNUIMessages.ErrorCancelPanel_Message_DontSend_Multi,
						new String[] { String.valueOf(numberOfErrors) });
			}
		} else {
			defaultMessage = SVNUIMessages.ErrorCancelPanel_Message_DontSend;
		}

		this.simpleMessage = simpleMessage == null ? SVNUIMessages.ErrorCancelPanel_NoInfo : simpleMessage;
		this.advancedMessage = advancedMessage == null
				? SVNUIMessages.ErrorCancelPanel_NoAdvancedInfo
				: advancedMessage;
		this.optionName = optionName;
	}

	public IReporter getReporter() {
		return reportingComposite == null
				? ReportingComposite.getDefaultReporter(panelType == ErrorCancelPanel.ERROR_PANEL_TYPE, errorStatus)
				: reportingComposite.getReporter();
	}

	public boolean doNotShowAgain() {
		return reportingComposite != null ? reportingComposite.isNotShowAgain() : false;
	}

	@Override
	public void createControlsImpl(Composite parent) {
		GridData data = null;
		errorTextField = new Text(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 100;
		errorTextField.setLayoutData(data);
		errorTextField.setEditable(false);
		errorTextField.setText(simpleMessage + "\n" + advancedMessage); //$NON-NLS-1$

		if (sendMail) {
			Composite mailComposite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginWidth = 0;
			layout.marginHeight = 1;
			mailComposite.setLayout(layout);
			data = new GridData(GridData.FILL_HORIZONTAL);
			mailComposite.setLayoutData(data);

			reportingComposite = new ReportingComposite(parent, dialogTitle, plugin, errorStatus, optionName, true,
					this, true);
			data = new GridData(GridData.FILL_HORIZONTAL);
			reportingComposite.setLayoutData(data);
		} else if (originalReport != null) {
			Button viewButton = new Button(parent, SWT.PUSH);
			viewButton.setText(SVNUIMessages.ErrorCancelPanel_OriginalReport);
			data = new GridData();
			data.widthHint = DefaultDialog.computeButtonWidth(viewButton);
			viewButton.setLayoutData(data);
			viewButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreviewReportPanel panel = new PreviewReportPanel(
							SVNUIMessages.ErrorCancelPanel_OriginalReportPreview, originalReport);
					DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getDisplay().getActiveShell(), panel);
					dialog.open();
				}
			});
		}
	}

	@Override
	public String getHelpId() {
		if (sendMail) {
			return "org.eclipse.team.svn.help.errorDialogContext"; //$NON-NLS-1$
		}
		return "org.eclipse.team.svn.help.cancelDialogContext"; //$NON-NLS-1$
	}

	@Override
	protected void saveChangesImpl() {
		if (sendMail) {
			reportingComposite.saveChanges();
		}
	}

	@Override
	protected void cancelChangesImpl() {
		if (sendMail) {
			reportingComposite.cancelChanges();
		}
	}

	protected void showDetails() {

	}

	@Override
	public void postInit() {
		validateContent();
	}

	@Override
	public Point getPrefferedSizeImpl() {
		return new Point(640, SWT.DEFAULT);
	}

	@Override
	public String getImagePath() {
		return "icons/dialogs/" + (panelType == ErrorCancelPanel.ERROR_PANEL_TYPE && isPluginError //$NON-NLS-1$
				? "operation_error.gif" //$NON-NLS-1$
				: "select_revision.gif"); //$NON-NLS-1$
	}

}
