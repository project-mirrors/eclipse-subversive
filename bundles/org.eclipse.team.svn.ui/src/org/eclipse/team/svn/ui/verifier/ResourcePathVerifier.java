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

import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Correct file path verifier
 * 
 * @author Sergiy Logvin
 */
public class ResourcePathVerifier extends AbstractFormattedVerifier {
	protected static String ERROR_MESSAGE;

	public ResourcePathVerifier(String fieldName) {
		super(fieldName);
		ResourcePathVerifier.ERROR_MESSAGE = BaseMessages.format(SVNUIMessages.Verifier_ResourcePath,
				new String[] { AbstractFormattedVerifier.FIELD_NAME });
	}

	@Override
	protected String getErrorMessageImpl(Control input) {
		String text = getText(input);
		Path path = new Path(text);
		if (!path.isValidPath(text)) {
			return ResourcePathVerifier.ERROR_MESSAGE;
		}
		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		return null;
	}

}
