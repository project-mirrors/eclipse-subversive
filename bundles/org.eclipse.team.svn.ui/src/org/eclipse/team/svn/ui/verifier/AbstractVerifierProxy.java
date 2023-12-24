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
		super();
		this.verifier = verifier;
	}

	public void addVerifierListener(IVerifierListener listener) {
		this.verifier.addVerifierListener(listener);
		super.addVerifierListener(listener);
	}

	public void removeVerifierListener(IVerifierListener listener) {
		this.verifier.removeVerifierListener(listener);
		super.removeVerifierListener(listener);
	}

	public boolean verify(Control input) {
		if (this.isVerificationEnabled(input)) {
			return this.verifier.verify(input);
		}
		if (!(this.hasWarning = this.verifier.hasWarning())) {
			this.fireOk();
		}
		return true;
	}

	protected abstract boolean isVerificationEnabled(Control input);

	protected String getErrorMessage(Control input) {
		return null;
	}

	protected String getWarningMessage(Control input) {
		return null;
	}

}
