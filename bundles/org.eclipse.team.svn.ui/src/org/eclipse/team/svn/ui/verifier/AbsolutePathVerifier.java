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

import java.util.StringTokenizer;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Allows only absolute paths
 * 
 * @author Alexander Gurov
 */
public class AbsolutePathVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_MESSAGE;

	public AbsolutePathVerifier(String fieldName) {
		super(fieldName);
		AbsolutePathVerifier.ERROR_MESSAGE = BaseMessages.format(SVNUIMessages.Verifier_AbsolutePath,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
	}

	@Override
	protected String getErrorMessageImpl(Control input) {
		String text = getText(input);
		if (isRealtive(text)) {
			return AbsolutePathVerifier.ERROR_MESSAGE;
		}
		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		return null;
	}

	protected boolean isRealtive(String path) {
		StringTokenizer tok = new StringTokenizer(path, "/\\", false); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (token.matches("(\\.)+")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}
}
