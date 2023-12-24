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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.text.StringMatcher;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Pattern verifier
 *
 * @author Sergiy Logvin
 */
public class PatternVerifier extends AbstractFormattedVerifier {
	protected IResource[] resources;

	protected static String message;

	public PatternVerifier(String fieldName, IResource[] resources) {
		super(fieldName);
		PatternVerifier.message = SVNUIMessages.Verifier_Pattern;
		this.resources = resources;
	}

	protected String getErrorMessageImpl(Control input) {
		String pattern = this.getText(input);
		StringMatcher matcher = new StringMatcher(pattern, true, false);
		for (int i = 0; i < this.resources.length; i++) {
			if (!matcher.match(this.resources[i].getName())) {
				return BaseMessages.format(PatternVerifier.message, new Object[] { this.resources[i].getName() });
			}
		}
		return null;
	}

	protected String getWarningMessageImpl(Control input) {
		return null;
	}

}
