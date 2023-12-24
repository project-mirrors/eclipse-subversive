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
 *    Alexey Mikoyan - Initial implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.verifier;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.utility.StringMatcher;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * File Name Template Verifier
 *
 * @author Alexey Mikoyan
 *
 */
public class FileNameTemplateVerifier extends AbstractFormattedVerifier {

	public static String ERROR_MESSAGE;

	public FileNameTemplateVerifier(String fieldName) {
		super(fieldName);
		FileNameTemplateVerifier.ERROR_MESSAGE = BaseMessages.format(SVNUIMessages.Verifier_FileNameTemplate,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
	}

	@Override
	protected String getErrorMessageImpl(Control input) {
		try {
			new StringMatcher(getText(input));
		} catch (Exception e) {
			return FileNameTemplateVerifier.ERROR_MESSAGE;
		}
		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		return null;
	}

}
