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

package org.eclipse.team.svn.ui.wizard;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.ui.verifier.AbstractVerificationKeyListener;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * Verified WizardPage implementation provides validation abilities
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractVerifiedWizardPage extends WizardPage implements IValidationManager {
	private VerificationKeyListener changeListener;

	public AbstractVerifiedWizardPage(String pageName) {
		super(pageName);
		changeListener = new VerificationKeyListener();
	}

	public AbstractVerifiedWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		changeListener = new VerificationKeyListener();
	}

	@Override
	public void createControl(Composite parent) {
		setControl(createControlImpl(parent));
		addListeners();
	}

	@Override
	public boolean isFilledRight() {
		return changeListener.isFilledRight();
	}

	@Override
	public void attachTo(Control cmp, AbstractVerifier verifier) {
		changeListener.attachTo(cmp, verifier);
	}

	public void addListeners() {
		changeListener.addListeners();
		validateContent();
		this.setMessage(getDescription(), IMessageProvider.NONE);
	}

	@Override
	public void detachFrom(Control cmp) {
		changeListener.detachFrom(cmp);
	}

	@Override
	public void detachAll() {
		changeListener.detachAll();
	}

	@Override
	public void validateContent() {
		changeListener.validateContent();
	}

	@Override
	public boolean validateControl(Control cmp) {
		return changeListener.validateControl(cmp);
	}

	@Override
	public void setPageComplete(boolean complete) {
		super.setPageComplete(complete && isFilledRight() && isPageCompleteImpl());
	}

	@Override
	public boolean isPageComplete() {
		if (getContainer().getCurrentPage() == this) {
			return super.isPageComplete();
		}
		return true;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			getControl().setFocus();
		}
	}

	@Override
	public void setMessage(String newMessage, int newType) {
		if (newType == IMessageProvider.WARNING) {
			//NOTE Eclipse workaround: all warnings are rendered as animated but old message does not cleared. So, old error still visible after warning is shown.
			AbstractVerifiedWizardPage.this.setMessage("", IMessageProvider.NONE); //$NON-NLS-1$
			//NOTE Eclipse workaround: clear error message before setting warning message
			AbstractVerifiedWizardPage.this.setErrorMessage(null);
			super.setMessage(newMessage, newType);
		} else if (newType == IMessageProvider.ERROR) {
			//NOTE Eclipse workaround: all warnings are rendered as animated but old message does not cleared. So, old error still visible after warning is shown.
			AbstractVerifiedWizardPage.this.setMessage("", IMessageProvider.NONE); //$NON-NLS-1$
			//NOTE Eclipse workaround: error will be rendered as animated only when setErrorMessage() is used.
			AbstractVerifiedWizardPage.this.setErrorMessage(newMessage);
		} else {
			//NOTE Eclipse workaround: clear error message before setting default message
			AbstractVerifiedWizardPage.this.setErrorMessage(null);
			super.setMessage(newMessage, newType);
		}
	}

	protected abstract Composite createControlImpl(Composite parent);

	protected boolean isPageCompleteImpl() {
		return true;
	}

	protected class VerificationKeyListener extends AbstractVerificationKeyListener {
		public VerificationKeyListener() {
		}

		@Override
		public void hasError(String errorReason) {
			AbstractVerifiedWizardPage.this.setMessage(errorReason, IMessageProvider.ERROR);
			handleButtons();
		}

		@Override
		public void hasWarning(String warningReason) {
			AbstractVerifiedWizardPage.this.setMessage(warningReason, IMessageProvider.WARNING);
			handleButtons();
		}

		@Override
		public void hasNoError() {
			AbstractVerifiedWizardPage.this.setMessage(getDescription(), IMessageProvider.NONE);
			handleButtons();
		}

		protected void handleButtons() {
			setPageComplete(isFilledRight());
		}

	}

}
