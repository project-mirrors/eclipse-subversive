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
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.composite;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Security warning composite
 * 
 * @author Sergiy Logvin
 */
public class SecurityWarningComposite extends Composite {

	public SecurityWarningComposite(Composite parent) {
		super(parent, SWT.NONE);
		init();
	}

	protected void init() {
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);
		setLayoutData(new GridData(GridData.FILL_BOTH));

		Link link = new Link(this, SWT.WRAP);
		link.setText(SVNUIMessages.SecurityWarningComposite_Message);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String pageId = "org.eclipse.equinox.security.ui.storage"; //$NON-NLS-1$
				PreferencesUtil.createPreferenceDialogOn(null, pageId, new String[] { pageId }, null).open();
			}
		});

		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		Dialog.applyDialogFont(link);
		data.heightHint = DefaultDialog.convertHeightInCharsToPixels(link, 2);
		link.setLayoutData(data);
	}

}
