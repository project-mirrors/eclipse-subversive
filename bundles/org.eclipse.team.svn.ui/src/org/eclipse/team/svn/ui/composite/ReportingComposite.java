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

package org.eclipse.team.svn.ui.composite;

import java.util.Arrays;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.extension.factory.IReporter;
import org.eclipse.team.svn.ui.extension.factory.IReporterFactory.ReportType;
import org.eclipse.team.svn.ui.extension.factory.IReportingDescriptor;
import org.eclipse.team.svn.ui.panel.reporting.PreviewErrorReportPanel;
import org.eclipse.team.svn.ui.panel.reporting.PreviewReportPanel;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;

/**
 * Reporting form composite
 * 
 * @author Alexander Gurov
 */
public class ReportingComposite extends Composite {
	public static final String MAIL_HISTORY = "mailHistory"; //$NON-NLS-1$

	public static final String USER_NAME_HISTORY = "userNameHistory"; //$NON-NLS-1$

	protected UserInputHistory mailHistory;

	protected UserInputHistory userNameHistory;

	protected Combo providersCombo;

	protected Text emailText;

	protected Text nameText;

	protected Text commentText;

	protected Button doNotShowAgainButton;

	protected Button previewButton;

	protected boolean doNotShowAgain;

	protected String reportType;

	protected boolean isError;

	protected String pluginId;

	protected IStatus status;

	protected IReportingDescriptor[] providers;

	protected IReporter reporter;

	public ReportingComposite(Composite parent, String reportType, String pluginId, IStatus status, String optionName,
			boolean isError, IValidationManager manager) {
		this(parent, reportType, pluginId, status, optionName, isError, manager, false);
	}

	public ReportingComposite(Composite parent, String reportType, String pluginId, IStatus status, String optionName,
			boolean isError, IValidationManager manager, boolean doNotValidateComment) {
		super(parent, SWT.NONE);
		this.isError = isError;
		this.reportType = reportType;
		this.pluginId = pluginId;
		this.status = status;
		providers = ExtensionsManager.getInstance().getReportingDescriptors();
		reporter = ReportingComposite.getDefaultReporter(isError, status);
		createControls(optionName, manager, doNotValidateComment);
	}

	public static IReporter getDefaultReporter(boolean isError, IStatus status) {
		IReportingDescriptor[] providers = ExtensionsManager.getInstance().getReportingDescriptors();
		if (providers.length > 0) {
			IReporter reporter = ExtensionsManager.getInstance()
					.getReporter(providers[0], isError ? ReportType.BUG : ReportType.TIP);
			if (reporter != null) {
				reporter.setProblemStatus(status);
				return reporter;
			}
		}
		return null;
	}

	public IReporter getReporter() {
		return reporter;
	}

	public boolean isNotShowAgain() {
		return doNotShowAgain;
	}

	public void saveChanges() {
		if (commentText != null) {
			reporter.setUserComment(commentText.getText().trim());
		}

		if (emailText != null) {
			String email = emailText.getText().trim();
			reporter.setUserEMail(email);
			if (email.length() > 0) {
				mailHistory.addLine(email);
			} else {
				mailHistory.clear();
			}
		}

		if (nameText != null) {
			String name = nameText.getText().trim();
			reporter.setUserName(name);
			if (name.length() > 0) {
				userNameHistory.addLine(name);
			} else {
				userNameHistory.clear();
			}
		}

		doNotShowAgain = doNotShowAgainButton.getSelection();
	}

	public void cancelChanges() {
		doNotShowAgain = doNotShowAgainButton.getSelection();
	}

	private void createControls(String optionName, IValidationManager manager, boolean doNotValidateComment) {
		GridLayout layout = null;
		GridData data = null;

		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		setLayout(layout);

		if (providers.length > 1 || reporter == null) {
			Label description1 = new Label(this, SWT.NONE);
			data = new GridData();
			description1.setLayoutData(data);
			description1.setText(SVNUIMessages.ReportingComposite_Product);

			providersCombo = new Combo(this, SWT.BORDER | SWT.READ_ONLY);
			data = new GridData(GridData.FILL_HORIZONTAL);
			providersCombo.setLayoutData(data);
			Arrays.sort(providers, (arg0, arg1) -> {
				IReportingDescriptor first = (IReportingDescriptor) arg0;
				IReportingDescriptor second = (IReportingDescriptor) arg1;
				return first.getProductName().compareTo(second.getProductName());
			});
			String[] names = new String[providers.length];
			for (int i = 0; i < providers.length; i++) {
				names[i] = providers[i].getProductName();
			}
			providersCombo.setItems(names);
			providersCombo.select(0);
			providersCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int selectedProviderIdx = providersCombo.getSelectionIndex();
					ReportingComposite.this.setReporter(selectedProviderIdx);
				}
			});
		}

		mailHistory = new UserInputHistory(ReportingComposite.MAIL_HISTORY, 1);
		userNameHistory = new UserInputHistory(ReportingComposite.USER_NAME_HISTORY, 1);
		String[] mailName = mailHistory.getHistory();

		if (providers.length > 1 || reporter != null && !reporter.isCustomEditorSupported() || reporter == null) {
			Label description = new Label(this, SWT.WRAP);
			data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
			data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
			data.heightHint = DefaultDialog.convertHeightInCharsToPixels(this, isError ? 4 : 3);
			data.horizontalSpan = 2;
			description.setLayoutData(data);
			description.setText(
					isError ? SVNUIMessages.ReportingComposite_ErrorHint : SVNUIMessages.ReportingComposite_Hint);

			Label description2 = new Label(this, SWT.NONE);
			data = new GridData();
			description2.setLayoutData(data);
			description2.setText(SVNUIMessages.ReportingComposite_EMail);

			emailText = new Text(this, SWT.BORDER);
			data = new GridData(GridData.FILL_HORIZONTAL);
			emailText.setLayoutData(data);
			emailText.setFocus();
			if (mailName != null && mailName.length > 0) {
				emailText.setText(mailName[0]);
			}

			Label description3 = new Label(this, SWT.NONE);
			data = new GridData();
			description3.setLayoutData(data);
			description3.setText(SVNUIMessages.ReportingComposite_Name);

			nameText = new Text(this, SWT.BORDER);
			data = new GridData(GridData.FILL_HORIZONTAL);
			nameText.setLayoutData(data);
			String[] userName = userNameHistory.getHistory();
			if (userName != null && userName.length > 0) {
				nameText.setText(userName[0]);
			}

			Label commentLabel = new Label(this, SWT.LEFT);
			data = new GridData();
			data.horizontalSpan = 2;
			commentLabel.setLayoutData(data);
			commentLabel.setText(SVNUIMessages.ReportingComposite_Comment);

			commentText = new Text(this, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);
			data = new GridData(GridData.FILL_BOTH);
			data.heightHint = 100;
			data.horizontalSpan = 2;
			commentText.setLayoutData(data);
			if (mailName != null && mailName.length > 0) {
				commentText.setFocus();
			}
			if (manager != null) {
				manager.attachTo(commentText, new AbstractVerifier() {
					@Override
					protected String getWarningMessage(Control input) {
						return null;
					}

					@Override
					protected String getErrorMessage(Control input) {
						if (ReportingComposite.this.getReporter() == null) {
							return SVNUIMessages.ReportingComposite_Product_Verifier;
						}
						return null;
					}
				});
				if (!doNotValidateComment) {
					manager.attachTo(commentText,
							new NonEmptyFieldVerifier(SVNUIMessages.ReportingComposite_Comment_Verifier));
				}
			}
		}

		Composite buttonsComposite = new Composite(this, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		layout.numColumns = 2;
		buttonsComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		buttonsComposite.setLayoutData(data);

		doNotShowAgainButton = new Button(buttonsComposite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		doNotShowAgainButton.setLayoutData(data);
		doNotShowAgainButton.setSelection(false);
		if (optionName != null) {
			doNotShowAgainButton.setText(optionName);
		} else {
			doNotShowAgainButton.setVisible(false);
		}

		if (providers.length > 1 || reporter != null && !reporter.isCustomEditorSupported() || reporter == null) {
			previewButton = new Button(buttonsComposite, SWT.PUSH);
			data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_HORIZONTAL);
			previewButton.setText(SVNUIMessages.ReportingComposite_Preview);
			data.widthHint = DefaultDialog.computeButtonWidth(previewButton);
			previewButton.setLayoutData(data);

			previewButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String name = nameText.getText().trim();
					String email = emailText.getText().trim();
					reporter.setUserName(name);
					reporter.setUserEMail(email);
					reporter.setUserComment(commentText.getText().trim());
					reporter.buildReport();

					PreviewReportPanel panel = null;
					if (isError) {
						panel = new PreviewErrorReportPanel(reporter.buildReport());
					} else {
						String msg = BaseMessages.format(SVNUIMessages.ReportingComposite_Preview_Title,
								new String[] { reportType });
						panel = new PreviewReportPanel(msg, reporter.buildReport());
					}
					DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getDisplay().getActiveShell(), panel);
					dialog.open();
				}
			});
		}
		setEnablement();
	}

	protected void setReporter(int selectedProviderIdx) {
		if (providers.length > selectedProviderIdx) {
			reporter = ExtensionsManager.getInstance()
					.getReporter(providers[selectedProviderIdx], isError ? ReportType.BUG : ReportType.TIP);
			if (reporter != null) {
				reporter.setProblemStatus(status);
			}
		}
		setEnablement();
	}

	protected void setEnablement() {
		boolean enabled = reporter != null && !reporter.isCustomEditorSupported();
		if (previewButton != null) {
			previewButton.setEnabled(enabled);
		}
		if (nameText != null) {
			nameText.setEnabled(enabled);
		}
		if (emailText != null) {
			emailText.setEnabled(enabled);
		}
	}

}
