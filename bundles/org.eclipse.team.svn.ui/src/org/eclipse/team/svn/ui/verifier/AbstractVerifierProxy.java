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

/**
 * Abstract verifier proxy implementation
 * 
 * @author Sergiy Logvin
 */
public abstract class AbstractVerifierProxy extends AbstractVerifier {
	protected AbstractVerifier verifier;

	public AbstractVerifierProxy(AbstractVerifier verifier) {
		this.verifier = verifier;
	}

	@Override
	public void addVerifierListener(IVerifierListener listener) {
		verifier.addVerifierListener(listener);
		super.addVerifierListener(listener);
	}

	@Override
	public void removeVerifierListener(IVerifierListener listener) {
		verifier.removeVerifierListener(listener);
		super.removeVerifierListener(listener);
	}

	@Override
	public boolean verify(Control input) {
		if (isVerificationEnabled(input)) {
			return verifier.verify(input);
		}
		if (!(hasWarning = verifier.hasWarning())) {
			fireOk();
		}
		return true;
	}

	protected abstract boolean isVerificationEnabled(Control input);

	@Override
	protected String getErrorMessage(Control input) {
		return null;
	}

	@Override
	protected String getWarningMessage(Control input) {
		return null;
	}

}
