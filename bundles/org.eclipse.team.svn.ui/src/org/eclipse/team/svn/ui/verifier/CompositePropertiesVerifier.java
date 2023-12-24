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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.verifier;

import java.util.HashMap;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;

/**
 * Composite properties verifier for properties' pages.
 * 
 * @author Alexei Goncharov
 */
public class CompositePropertiesVerifier extends AbstractVerifier {

	private HashMap<String, AbstractFormattedVerifier> verifiers;

	private Combo propNameCombo;

	public CompositePropertiesVerifier(Combo propNameCombo, HashMap<String, AbstractFormattedVerifier> verifierParam) {
		verifiers = verifierParam;
		this.propNameCombo = propNameCombo;
	}

	@Override
	public boolean verify(Control input) {
		AbstractFormattedVerifier current = verifiers.get(propNameCombo.getText());
		if (current == null) {
			return true;
		}
		String msg = current.getErrorMessage(input);
		if (msg != null) {
			fireError(msg);
			return false;
		}
		msg = current.getWarningMessage(input);
		if (msg != null) {
			fireWarning(msg);
		} else {
			fireOk();
		}
		return true;
	}

	@Override
	protected String getErrorMessage(Control input) {
		return null;
	}

	@Override
	protected String getWarningMessage(Control input) {
		return null;
	}

}
