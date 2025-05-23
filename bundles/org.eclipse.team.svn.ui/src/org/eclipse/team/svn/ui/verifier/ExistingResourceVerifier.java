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

package org.eclipse.team.svn.ui.verifier;

import java.io.File;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Existing file verifier
 * 
 * @author Sergiy Logvin
 */
public class ExistingResourceVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_MESSAGE_DOES_NOT_EXIST;

	protected static String ERROR_MESSAGE_IS_NOT_A_FILE;

	protected static String ERROR_MESSAGE_IS_NOT_A_DIRECTORY;

	protected boolean checkNodeType;

	protected boolean files;

	public ExistingResourceVerifier(String fieldName) {
		super(fieldName);
		init();
		checkNodeType = false;
	}

	public ExistingResourceVerifier(String fieldName, boolean files) {
		super(fieldName);
		init();
		this.files = files;
		checkNodeType = true;
	}

	@Override
	protected String getErrorMessageImpl(Control input) {
		File currentFile = new File(getText(input));
		if (!currentFile.exists()) {
			return ExistingResourceVerifier.ERROR_MESSAGE_DOES_NOT_EXIST;
		} else if (checkNodeType && files && !currentFile.isFile()) {
			return ExistingResourceVerifier.ERROR_MESSAGE_IS_NOT_A_FILE;
		} else if (checkNodeType && !files && !currentFile.isDirectory()) {
			return ExistingResourceVerifier.ERROR_MESSAGE_IS_NOT_A_DIRECTORY;
		}
		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		return null;
	}

	private void init() {
		ExistingResourceVerifier.ERROR_MESSAGE_DOES_NOT_EXIST = BaseMessages.format(
				SVNUIMessages.Verifier_ExistingResource_NotExists,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
		ExistingResourceVerifier.ERROR_MESSAGE_IS_NOT_A_FILE = BaseMessages.format(
				SVNUIMessages.Verifier_ExistingResource_IsNotAFile,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
		ExistingResourceVerifier.ERROR_MESSAGE_IS_NOT_A_DIRECTORY = BaseMessages.format(
				SVNUIMessages.Verifier_ExistingResource_IsNotADir,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
	}
}
