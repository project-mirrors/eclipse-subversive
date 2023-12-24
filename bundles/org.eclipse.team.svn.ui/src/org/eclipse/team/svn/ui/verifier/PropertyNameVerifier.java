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

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Property name verifier
 * 
 * @author Sergiy Logvin
 */
public class PropertyNameVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_MESSAGE_LETTER;

	protected static String ERROR_MESSAGE_SYMBOLS;

	protected HashSet<String> ignoreStrings;

	public PropertyNameVerifier(String fieldName) {
		super(fieldName);
		PropertyNameVerifier.ERROR_MESSAGE_LETTER = BaseMessages.format(SVNUIMessages.Verifier_PropertyName_Letter,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
		PropertyNameVerifier.ERROR_MESSAGE_SYMBOLS = BaseMessages.format(SVNUIMessages.Verifier_PropertyName_Symbols,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
		ignoreStrings = new HashSet<>();
		ignoreStrings.add(SVNUIMessages.AbstractPropertyEditPanel_svn_description);
		ignoreStrings.add(SVNUIMessages.PropertyEditPanel_tsvn_description);
		ignoreStrings.add(SVNUIMessages.PropertyEditPanel_bugtraq_description);
		ignoreStrings.add(SVNUIMessages.AbstractPropertyEditPanel_custom_description);
		ignoreStrings.add("    " + SVNUIMessages.AbstractPropertyEditPanel_custom_hint); //$NON-NLS-1$
	}

	@Override
	protected String getErrorMessageImpl(Control input) {
		String property = getText(input);
		if (property.trim().length() == 0) {
			return null;
		}
		if (ignoreStrings.contains(property)) {
			return SVNUIMessages.AbstractPropertyEditPanel_Name_Verifier_IgnoreStrings;
		}
		Pattern pattern = Pattern.compile("[a-zA-Z].*"); //$NON-NLS-1$
		Matcher matcher = pattern.matcher(property);
		if (!matcher.matches()) {
			return PropertyNameVerifier.ERROR_MESSAGE_LETTER;
		}
		pattern = Pattern.compile("[a-zA-Z0-9:\\-_.]*"); //$NON-NLS-1$
		if (!pattern.matcher(property).matches()) {
			return PropertyNameVerifier.ERROR_MESSAGE_SYMBOLS;
		}

		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		return null;
	}

}
