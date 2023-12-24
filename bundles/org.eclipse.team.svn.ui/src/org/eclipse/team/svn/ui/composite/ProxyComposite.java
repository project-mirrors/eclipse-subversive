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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UserInputHistory;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * Proxy properties editor panel
 *
 * @author Sergiy Logvin
 */
public class ProxyComposite extends AbstractDynamicComposite implements IPropertiesPanel {
	protected static final String USER_HISTORY_NAME = "proxyUser"; //$NON-NLS-1$

	protected String username;

	protected String password;

	protected String host;

	protected boolean callback;

	protected Combo usernameText;

	protected Text passwordText;

	protected IValidationManager validationManager;

	protected UserInputHistory userHistory;

	public ProxyComposite(Composite parent, int style, IValidationManager validationManager) {
		this(parent, style, validationManager, false);
	}

	public ProxyComposite(Composite parent, int style, IValidationManager validationManager, boolean callback) {
		super(parent, style);
		this.validationManager = validationManager;
		this.callback = callback;
	}

	public void setProxySettingsDirect(String username, String password) {
		usernameText.setText(username);
		passwordText.setText(password);
		if (callback) {
			if (this.username != null && this.username.trim().length() > 0) {
				passwordText.setFocus();
				passwordText.selectAll();
			} else {
				usernameText.setFocus();
			}
		}
	}

	@Override
	public void saveChanges() {
		userHistory.addLine(usernameText.getText());
		username = usernameText.getText();
		password = passwordText.getText();
	}

	@Override
	public void resetChanges() {
		usernameText.setText(username != null ? username : ""); //$NON-NLS-1$
		passwordText.setText(password != null ? password : ""); //$NON-NLS-1$
		if (callback) {
			if (username != null && username.trim().length() > 0) {
				passwordText.setFocus();
				passwordText.selectAll();
			} else {
				usernameText.setFocus();
			}
		}
	}

	@Override
	public void cancelChanges() {

	}

	@Override
	public void initialize() {
		GridLayout layout = null;
		GridData data = null;

		layout = new GridLayout();
		layout.verticalSpacing = 12;
		layout.marginHeight = 7;
		setLayout(layout);
		data = new GridData(GridData.FILL_BOTH);
		setLayoutData(data);

		Label description = new Label(this, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		description.setLayoutData(data);
		description.setText(BaseMessages.format(SVNUIMessages.ProxyComposite_Description, new String[] { host }));

		//Authentication group
		Group authGroup = new Group(this, SWT.NULL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		authGroup.setLayoutData(data);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 12;
		authGroup.setLayout(layout);
		authGroup.setText(SVNUIMessages.ProxyComposite_Authentication);

		//Username and password inner group
		Composite inner = new Composite(authGroup, SWT.FILL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		inner.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		inner.setLayoutData(data);

		description = new Label(inner, SWT.NULL);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		description.setLayoutData(data);
		description.setText(SVNUIMessages.ProxyComposite_Username);

		userHistory = new UserInputHistory(ProxyComposite.USER_HISTORY_NAME);

		usernameText = new Combo(inner, SWT.DROP_DOWN);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		usernameText.setLayoutData(data);
		usernameText.setVisibleItemCount(userHistory.getDepth());
		usernameText.setItems(userHistory.getHistory());
		usernameText.setText(getUsername());

		description = new Label(inner, SWT.NULL);
		description.setText(SVNUIMessages.ProxyComposite_Password);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		description.setLayoutData(data);

		passwordText = new Text(inner, SWT.PASSWORD | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		passwordText.setLayoutData(data);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public void revalidateContent() {
		validationManager.validateContent();
	}

	@Override
	public void restoreAppearance() {
	}

	@Override
	public void saveAppearance() {
	}

}
