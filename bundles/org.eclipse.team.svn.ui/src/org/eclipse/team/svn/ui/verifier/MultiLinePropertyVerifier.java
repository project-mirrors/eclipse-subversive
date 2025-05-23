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

import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Verify multiple properties separated by line separator
 *
 * @author Alexey Mikoyan
 *
 */
public class MultiLinePropertyVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_MESSAGE_INVALID_FORMAT = "Field '" + AbstractFormattedVerifier.FIELD_NAME
			+ "' contains malformed property in line ";

	public MultiLinePropertyVerifier(String fieldName) {
		super(fieldName);
		MultiLinePropertyVerifier.ERROR_MESSAGE_INVALID_FORMAT = SVNUIMessages.Verifier_MultiLineProperty_Main;
	}

	@Override
	protected String getErrorMessageImpl(Control input) {
		String[] properties = getText(input).split(System.lineSeparator());
		for (int i = 0; i < properties.length; i++) {
			if (properties[i].length() == 0) {
				continue;
			}

			String retVal = validateProperty(properties[i], i);
			if (retVal != null) {
				return retVal;
			}
		}

		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		return null;
	}

	protected String validateProperty(String property, int line) {
		Pattern pattern;

		String[] propNameValue = property.split("=", 2); //$NON-NLS-1$

		if (propNameValue.length == 0 || propNameValue[0].length() == 0) {
			return formatMainMessage(line) + " " + SVNUIMessages.Verifier_MultiLineProperty_NameIsEmpty; //$NON-NLS-1$
		}

		pattern = Pattern.compile("[a-zA-Z:_].*"); //$NON-NLS-1$
		if (!pattern.matcher(propNameValue[0]).matches()) {
			return formatMainMessage(line) + " " + SVNUIMessages.Verifier_MultiLineProperty_NotALetter; //$NON-NLS-1$
		}

		pattern = Pattern.compile("[a-zA-Z0-9:\\-_\\.]+"); //$NON-NLS-1$
		if (!pattern.matcher(propNameValue[0]).matches()) {
			return formatMainMessage(line) + " " + SVNUIMessages.Verifier_MultiLineProperty_InvalidNameChar; //$NON-NLS-1$
		}

		if (propNameValue.length < 2 || propNameValue[1].length() == 0) {
			return formatMainMessage(line) + " " + SVNUIMessages.Verifier_MultiLineProperty_EmptyValue; //$NON-NLS-1$
		}

		return null;
	}

	protected String formatMainMessage(int line) {
		return BaseMessages.format(MultiLinePropertyVerifier.ERROR_MESSAGE_INVALID_FORMAT,
				new Object[] { AbstractFormattedVerifier.FIELD_NAME, String.valueOf(line + 1) });
	}

}
