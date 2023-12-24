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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.widgets.Control;

/**
 * Abstract verifier implementation, that provides formatted message support
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractFormattedVerifier extends AbstractVerifier {
	public static final String FIELD_NAME = "$FIELD_NAME$"; //$NON-NLS-1$

	protected Map<String, String> placeHolders;

	public AbstractFormattedVerifier(String fieldName) {
		placeHolders = new HashMap<>();
		setPlaceHolder(AbstractFormattedVerifier.FIELD_NAME, fieldName.replace(":", "")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void setPlaceHolder(String placeHolder, String value) {
		placeHolders.put(placeHolder, value);
	}

	public String getPlaceHolder(String placeHolder) {
		return placeHolders.get(placeHolder);
	}

	@Override
	protected String getErrorMessage(Control input) {
		return getFormattedMessage(getErrorMessageImpl(input));
	}

	@Override
	protected String getWarningMessage(Control input) {
		return getFormattedMessage(getWarningMessageImpl(input));
	}

	protected abstract String getErrorMessageImpl(Control input);

	protected abstract String getWarningMessageImpl(Control input);

	protected String getFormattedMessage(String message) {
		if (message != null) {
			for (Entry<String, String> entry : placeHolders.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue() == null ? "" : entry.getValue().toString(); //$NON-NLS-1$
				int idx = message.indexOf(key);
				if (idx != -1) {
					message = message.substring(0, idx) + value + message.substring(idx + key.length());
				}
			}
		}
		return message;
	}

}
