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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * Performance options
 * 
 * @author Alexander Gurov
 */
public class SVNTeamPerformancePage extends AbstractSVNTeamPreferencesPage {
	protected Button computeDeepButton;

	protected Button preciseEnablementsButton;

	protected Button enableCacheButton;

	protected Button enablePersistentSSHConnectionButton;
//	protected Button enableFileReplacementAutoundoButton;

	protected boolean computeDeep;

	protected boolean preciseEnablements;

	protected boolean enableCache;

	protected boolean enablePersistentSSHConnection;
//	protected boolean enableFileReplacementAutoundo;

	public SVNTeamPerformancePage() {
	}

	@Override
	public void init(IWorkbench workbench) {
		setDescription(SVNUIMessages.PerformancePreferencePage_optionsDesc);
	}

	@Override
	protected void saveValues(IPreferenceStore store) {
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_COMPUTE_DEEP_NAME, computeDeep);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_PRECISE_ENABLEMENTS_NAME,
				preciseEnablements);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_CACHE_NAME,
				computeDeep | enableCache);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_PERSISTENT_SSH_NAME,
				enablePersistentSSHConnection);
//		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_FILE_REPLACEMENT_AUTOUNDO_NAME, this.enableFileReplacementAutoundo);
	}

	@Override
	protected void loadDefaultValues(IPreferenceStore store) {
		computeDeep = SVNTeamPreferences.DECORATION_COMPUTE_DEEP_DEFAULT;
		preciseEnablements = SVNTeamPreferences.DECORATION_PRECISE_ENABLEMENTS_DEFAULT;
		enableCache = SVNTeamPreferences.DECORATION_ENABLE_CACHE_DEFAULT;
		enablePersistentSSHConnection = SVNTeamPreferences.DECORATION_ENABLE_PERSISTENT_SSH_DEFAULT;
//		this.enableFileReplacementAutoundo = SVNTeamPreferences.DECORATION_ENABLE_FILE_REPLACEMENT_AUTOUNDO_DEFAULT;
	}

	@Override
	protected void loadValues(IPreferenceStore store) {
		computeDeep = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_COMPUTE_DEEP_NAME);
		preciseEnablements = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_PRECISE_ENABLEMENTS_NAME);
		enableCache = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_CACHE_NAME);
		enablePersistentSSHConnection = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_ENABLE_PERSISTENT_SSH_NAME);
//		this.enableFileReplacementAutoundo = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_FILE_REPLACEMENT_AUTOUNDO_NAME);
	}

	@Override
	protected void initializeControls() {
		computeDeepButton.setSelection(computeDeep);
		preciseEnablementsButton.setSelection(preciseEnablements);
		enableCacheButton.setSelection(enableCache);
		enablePersistentSSHConnectionButton.setSelection(enablePersistentSSHConnection);
//		this.enableFileReplacementAutoundoButton.setSelection(this.enableFileReplacementAutoundo);
		if (computeDeep || preciseEnablements) {
			enableCacheButton.setEnabled(false);
		} else if (!enableCache) {
			computeDeepButton.setEnabled(false);
			preciseEnablementsButton.setEnabled(false);
		}
	}

	@Override
	protected Control createContentsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.FILL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = false;
		composite.setLayoutData(data);

		Composite noteComposite = new Composite(composite, SWT.FILL);
		layout = new GridLayout();
		layout.marginWidth = 0;
		noteComposite.setLayout(layout);
		noteComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label separator = new Label(noteComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		separator.setLayoutData(data);

		computeDeepButton = new Button(composite, SWT.CHECK);
		computeDeepButton.setLayoutData(new GridData());
		computeDeepButton.setText(SVNUIMessages.PerformancePreferencePage_computeDeep);
		computeDeepButton.addListener(SWT.Selection, event -> {
			computeDeep = computeDeepButton.getSelection();
			enableCacheButton.setEnabled(
					!(computeDeep | preciseEnablements));
		});

		preciseEnablementsButton = new Button(composite, SWT.CHECK);
		preciseEnablementsButton.setLayoutData(new GridData());
		preciseEnablementsButton.setText(SVNUIMessages.PerformancePreferencePage_preciseEnablements);
		preciseEnablementsButton.addListener(SWT.Selection, event -> {
			preciseEnablements = preciseEnablementsButton.getSelection();
			enableCacheButton.setEnabled(
					!(computeDeep | preciseEnablements));
		});

		enableCacheButton = new Button(composite, SWT.CHECK);
		enableCacheButton.setLayoutData(new GridData());
		enableCacheButton.setText(SVNUIMessages.PerformancePreferencePage_enableCache);
		enableCacheButton.addListener(SWT.Selection, event -> {
			enableCache = enableCacheButton.getSelection();
			computeDeepButton.setEnabled(enableCache);
			preciseEnablementsButton.setEnabled(enableCache);
		});

		enablePersistentSSHConnectionButton = new Button(composite, SWT.CHECK);
		enablePersistentSSHConnectionButton.setLayoutData(new GridData());
		enablePersistentSSHConnectionButton
				.setText(SVNUIMessages.PerformancePreferencePage_enablePersistentSSHConnection);
		enablePersistentSSHConnectionButton.addListener(SWT.Selection, event -> enablePersistentSSHConnection = enablePersistentSSHConnectionButton.getSelection());

//		this.enableFileReplacementAutoundoButton = new Button(composite, SWT.CHECK);
//		this.enableFileReplacementAutoundoButton.setLayoutData(new GridData());
//		this.enableFileReplacementAutoundoButton.setText(SVNUIMessages.PerformancePreferencePage_enableFileReplacementAutoundo);
//		this.enableFileReplacementAutoundoButton.addListener(SWT.Selection, new Listener() {
//			public void handleEvent (Event event) {
//				SVNTeamPerformancePage.this.enableFileReplacementAutoundo = SVNTeamPerformancePage.this.enableFileReplacementAutoundoButton.getSelection();
//			}
//		});

//		Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(parent, "org.eclipse.team.svn.help.performancePreferencesContext"); //$NON-NLS-1$

		return composite;
	}

}
