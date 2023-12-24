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
 * SVN resource name verifier
 * 
 * @author Alexander Gurov
 */
public class ResourceNameVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_MESSAGE;

	protected boolean allowMultipart;

	public ResourceNameVerifier(String fieldName, boolean allowMultipart) {
		super(fieldName);
		ResourceNameVerifier.ERROR_MESSAGE = SVNUIMessages.format(SVNUIMessages.Verifier_ResourceName,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
		this.allowMultipart = allowMultipart;
	}

	protected String getErrorMessageImpl(Control input) {
		String fileName = this.getText(input);
		if (fileName.length() != 0 && !this.isValidSegment(fileName)) {
			return ResourceNameVerifier.ERROR_MESSAGE;
		}
		return null;
	}

	public boolean isValidSegment(String segment) {
		int size = segment.length();
		boolean nameCharactersFound = false;
		for (int i = 0; i < size; i++) {
			char c = segment.charAt(i);
			if (c == '?' || c == '*' || c == ':' || !this.allowMultipart && (c == '\\' || c == '/')) {
				return false;
			} else if (c != '\\' && c != '/' && c != '.') {
				nameCharactersFound = true;
			}
		}
		return nameCharactersFound;
	}

	protected String getWarningMessageImpl(Control input) {
		return null;
	}

}
