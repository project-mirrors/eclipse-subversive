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

package org.eclipse.team.svn.ui.composite;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UserInputHistory;

/**
 * Credentials composite
 *
 * @author Sergiy Logvin
 */
public class CredentialsComposite extends Composite {
	protected static final String USER_HISTORY_NAME = "repositoryUser"; //$NON-NLS-1$

	protected Combo userName;

	protected Text password;

	protected Button savePassword;

	protected String usernameInput;

	protected String passwordInput;

	protected boolean passwordSaved;

	protected UserInputHistory userHistory;

	public CredentialsComposite(Composite parent, int style) {
		super(parent, style);
		createControls();
	}

	public void initialize() {
		if (usernameInput != null && usernameInput.trim().length() > 0) {
			userName.setText(usernameInput);
		} else {
			userName.setFocus();
		}

		if (passwordInput != null) {
			password.setText(passwordInput);
		}
		if (usernameInput != null && usernameInput.trim().length() > 0) {
			password.setFocus();
			password.selectAll();
		}

		savePassword.setSelection(passwordSaved);
	}

	public Text getPassword() {
		return password;
	}

	public Button getSavePassword() {
		return savePassword;
	}

	public UserInputHistory getUserHistory() {
		return userHistory;
	}

	public void setUserHistory(UserInputHistory userHistory) {
		this.userHistory = userHistory;
	}

	public Combo getUsername() {
		return userName;
	}

	public void setPasswordInput(String passwordInput) {
		this.passwordInput = passwordInput;
	}

	public void setPasswordSaved(boolean passwordSaved) {
		this.passwordSaved = passwordSaved;
	}

	public void setUsernameInput(String usernameInput) {
		this.usernameInput = usernameInput;
	}

	private void createControls() {
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);

		GridData data = new GridData(GridData.FILL_BOTH);
		setLayoutData(data);

		Group authGroup = new Group(this, SWT.NONE);
		layout = new GridLayout();
		layout.verticalSpacing = 12;
		authGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		data.horizontalSpan = 2;
		authGroup.setLayoutData(data);
		authGroup.setText(SVNUIMessages.CredentialsComposite_Authentication);

		Composite inner = new Composite(authGroup, SWT.FILL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		inner.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		inner.setLayoutData(data);

		Label description = new Label(inner, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = false;
		data.horizontalIndent = 0;
		description.setLayoutData(data);
		description.setText(SVNUIMessages.CredentialsComposite_User);

		userHistory = new UserInputHistory(CredentialsComposite.USER_HISTORY_NAME);

		userName = new Combo(inner, SWT.DROP_DOWN);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		userName.setLayoutData(data);
		userName.setVisibleItemCount(userHistory.getDepth());
		userName.setItems(userHistory.getHistory());

		description = new Label(inner, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = false;
		data.horizontalIndent = 0;
		description.setLayoutData(data);
		description.setText(SVNUIMessages.CredentialsComposite_Password);

		password = new Text(inner, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		password.setLayoutData(data);

		inner = new Composite(authGroup, SWT.FILL);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		inner.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		inner.setLayoutData(data);

		savePassword = new Button(inner, SWT.CHECK);
		data = new GridData();
		savePassword.setLayoutData(data);
		savePassword.setText(SVNUIMessages.CredentialsComposite_SavePassword);

		new SecurityWarningComposite(inner);
	}

}
