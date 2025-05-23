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

package org.eclipse.team.svn.ui.verifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Revision ranges input field verifier
 * 
 * @author Alexander Gurov
 */
public class RevisionRangesVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_MESSAGE;

	protected Pattern pattern;

	public RevisionRangesVerifier(String fieldName) {
		super(fieldName);
		RevisionRangesVerifier.ERROR_MESSAGE = BaseMessages.format(SVNUIMessages.Verifier_RevisionRanges,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
		pattern = Pattern.compile("\\d+(-\\d+)?(\\s*,\\s*\\d+(-\\d+)?)*"); //$NON-NLS-1$
	}

	@Override
	protected String getErrorMessageImpl(Control input) {
		String text = getText(input);
		Matcher matcher = pattern.matcher(text);
		if (!matcher.matches()) {
			return RevisionRangesVerifier.ERROR_MESSAGE;
		}
		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		return null;
	}

}
