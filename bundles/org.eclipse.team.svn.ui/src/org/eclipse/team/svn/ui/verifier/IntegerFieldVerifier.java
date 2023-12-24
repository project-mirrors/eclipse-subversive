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
 *    Alexander Gurov - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.verifier;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Integer field verifier
 * 
 * @author Alexander Gurov
 */
public class IntegerFieldVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_NAN;

	protected static String ERROR_NEGATIVE;

	protected boolean positive;

	public IntegerFieldVerifier(String fieldName, boolean positive) {
		super(fieldName);
		this.positive = positive;
		IntegerFieldVerifier.ERROR_NAN = SVNUIMessages.format(SVNUIMessages.Verifier_IntegerField_NaN,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
		IntegerFieldVerifier.ERROR_NEGATIVE = SVNUIMessages.format(SVNUIMessages.Verifier_IntegerField_Negative,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
	}

	protected String getErrorMessageImpl(Control input) {
		String text = this.getText(input);
		try {
			long i = Long.parseLong(text);
			if (this.positive && i < 0) {
				return IntegerFieldVerifier.ERROR_NEGATIVE;
			}
		} catch (Exception ex) {
			return IntegerFieldVerifier.ERROR_NAN;
		}
		return null;
	}

	protected String getWarningMessageImpl(Control input) {
		return null;
	}

}
