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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.verifier;

import org.eclipse.swt.widgets.Control;

/**
 * Allow to wrap all verifiers in the same way
 * 
 * It can be useful e.g. in case if you want that all verifiers don't start verifying on some particular event.
 * 
 * So instead of wraping all verifiers separately, you can provide here only one verifier wrapper.
 * 
 * @author Igor Burilo
 */
public abstract class AbstractValidationManagerProxy implements IValidationManager {

	protected IValidationManager validationManager;

	public AbstractValidationManagerProxy(IValidationManager validationManager) {
		this.validationManager = validationManager;
	}

	@Override
	public void attachTo(Control cmp, AbstractVerifier verifier) {
		validationManager.attachTo(cmp, wrapVerifier(verifier));
	}

	@Override
	public void detachFrom(Control cmp) {
		validationManager.detachFrom(cmp);
	}

	@Override
	public void detachAll() {
		validationManager.detachAll();
	}

	@Override
	public boolean isFilledRight() {
		return validationManager.isFilledRight();
	}

	@Override
	public void validateContent() {
		validationManager.validateContent();
	}

	@Override
	public boolean validateControl(Control cmp) {
		return validationManager.validateControl(cmp);
	}

	/**
	 * Can be overridden if you want to wrap verifier in some other way
	 * 
	 * @param verifier
	 * @return
	 */
	protected AbstractVerifier wrapVerifier(AbstractVerifier verifier) {
		return new AbstractVerifierProxy(verifier) {
			@Override
			protected boolean isVerificationEnabled(Control input) {
				return AbstractValidationManagerProxy.this.isVerificationEnabled(input);
			}
		};
	}

	protected abstract boolean isVerificationEnabled(Control input);
}
