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
 * Proxy port verifier
 *
 * @author Sergiy Logvin
 */
public class ProxyPortVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_RANGE;

	protected static String ERROR_NAN;

	public ProxyPortVerifier(String fieldName) {
		super(fieldName);
		ProxyPortVerifier.ERROR_RANGE = BaseMessages.format(SVNUIMessages.Verifier_ProxyPort_Range,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
		ProxyPortVerifier.ERROR_NAN = BaseMessages.format(SVNUIMessages.Verifier_ProxyPort_NaN,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
	}

	@Override
	protected String getErrorMessageImpl(Control hostField) {
		String portString = getText(hostField);
		if (portString.trim().length() == 0) {
			return null;
		}
		try {
			int port = Integer.parseInt(portString);
			if (port < 0 || port > 65535) {
				return ProxyPortVerifier.ERROR_RANGE;
			}
		} catch (IllegalArgumentException ex) {
			return ProxyPortVerifier.ERROR_NAN;
		}
		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		return null;
	}

}
