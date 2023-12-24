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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Control;

/**
 * Composite verifier allow us to compose simple verifiers into one more complex
 * 
 * @author Alexander Gurov
 */
public class CompositeVerifier extends AbstractVerifier implements IVerifierListener {
	protected List<AbstractVerifier> verifiers;

	public CompositeVerifier() {
		verifiers = new ArrayList<>();
	}

	public List<AbstractVerifier> getVerifiers() {
		return verifiers;
	}

	public void add(AbstractVerifier verifier) {
		if (!verifiers.contains(verifier)) {
			verifier.addVerifierListener(this);
			verifiers.add(verifier);
		}
	}

	public void remove(AbstractVerifier verifier) {
		if (verifiers.remove(verifier)) {
			verifier.removeVerifierListener(this);
		}
	}

	public void removeAll() {
		for (AbstractVerifier verifier : verifiers) {
			verifier.removeVerifierListener(this);
		}
		verifiers.clear();
	}

	@Override
	public boolean verify(Control input) {
		hasWarning = false;
		for (AbstractVerifier verifier : verifiers) {
			if (!verifier.verify(input)) {
				return false;
			}
		}
		if (!hasWarning) {
			fireOk();
		}
		return true;
	}

	@Override
	public void hasError(String errorReason) {
		fireError(errorReason);
	}

	@Override
	public void hasWarning(String warningReason) {
		fireWarning(warningReason);
	}

	@Override
	public void hasNoError() {

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
