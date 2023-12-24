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

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Verifier for custom fields
 *
 * @author Sergiy Logvin
 */
public class NonEmptyFieldVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_MESSAGE;

	public NonEmptyFieldVerifier(String fieldName) {
		super(fieldName);
		NonEmptyFieldVerifier.ERROR_MESSAGE = BaseMessages.format(SVNUIMessages.Verifier_NonEmpty,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
	}

	@Override
	protected String getErrorMessageImpl(Control input) {
		String text = getText(input);
		if (text.trim().length() == 0) {
			return NonEmptyFieldVerifier.ERROR_MESSAGE;
		}
		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		return null;
	}

}
