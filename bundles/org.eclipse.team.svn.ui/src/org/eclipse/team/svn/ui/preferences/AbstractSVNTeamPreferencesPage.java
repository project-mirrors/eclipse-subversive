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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.preferences;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.verifier.AbstractVerificationKeyListener;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;
import org.eclipse.team.svn.ui.verifier.IValidationManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Abstract preferences page provides validation support.
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractSVNTeamPreferencesPage extends PreferencePage
		implements IWorkbenchPreferencePage, IValidationManager {
	private VerificationKeyListener changeListener;

	public AbstractSVNTeamPreferencesPage() {
		changeListener = new VerificationKeyListener();
	}

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	public boolean performOk() {
		saveValues(getPreferenceStore());

		SVNTeamUIPlugin.instance().savePreferences();

		return true;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		loadDefaultValues(getPreferenceStore());
		initializeControls();

		validateContent();
	}

	@Override
	protected Control createContents(Composite parent) {
		Control retVal = createContentsImpl(parent);

		loadValues(getPreferenceStore());
		initializeControls();

		addListeners();

		return retVal;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return SVNTeamUIPlugin.instance().getPreferenceStore();
	}

	protected abstract void loadDefaultValues(IPreferenceStore store);

	protected abstract void loadValues(IPreferenceStore store);

	protected abstract void saveValues(IPreferenceStore store);

	protected abstract void initializeControls();

	protected abstract Control createContentsImpl(Composite parent);

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
		this.setMessage(getTitle(), IMessageProvider.NONE);
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

	protected class VerificationKeyListener extends AbstractVerificationKeyListener {
		public VerificationKeyListener() {
		}

		@Override
		public void hasError(String errorReason) {
			AbstractSVNTeamPreferencesPage.this.setMessage(errorReason, IMessageProvider.ERROR);
			handleButtons();
		}

		@Override
		public void hasWarning(String warningReason) {
			AbstractSVNTeamPreferencesPage.this.setMessage(warningReason, IMessageProvider.WARNING);
			handleButtons();
		}

		@Override
		public void hasNoError() {
			AbstractSVNTeamPreferencesPage.this.setMessage(getTitle(), IMessageProvider.NONE);
			handleButtons();
		}

		protected void handleButtons() {
			setValid(isFilledRight());
		}

	}

}
