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

package org.eclipse.team.svn.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ResourceSpecificParameters;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.DiffViewerFileAssociationsComposite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * Diff Viewer preference page
 * 
 * @author Igor Burilo
 */
public class SVNTeamDiffViewerPage extends AbstractSVNTeamPreferencesPage {

	protected DiffViewerFileAssociationsComposite fileAssociationsComposite;

	protected DiffViewerSettings diffSettings;

	@Override
	public void init(IWorkbench workbench) {
		setDescription(SVNUIMessages.SVNTeamDiffViewerPage_Description);
	}

	@Override
	protected Control createContentsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 1;
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		composite.setLayout(layout);

		fileAssociationsComposite = new DiffViewerFileAssociationsComposite(composite, this);

		//Setting context help
		PlatformUI.getWorkbench()
				.getHelpSystem()
				.setHelp(parent, "org.eclipse.team.svn.help.diffViewerPreferencesContext"); //$NON-NLS-1$

		return composite;
	}

	@Override
	protected void initializeControls() {
		fileAssociationsComposite.initializeControls(diffSettings);
	}

	@Override
	protected void loadDefaultValues(IPreferenceStore store) {
		diffSettings = DiffViewerSettings.getDefaultDiffViewerSettings();
	}

	@Override
	protected void loadValues(IPreferenceStore store) {
		diffSettings = SVNTeamDiffViewerPage.loadDiffViewerSettings(store);
	}

	@Override
	protected void saveValues(IPreferenceStore store) {
		SVNTeamDiffViewerPage.saveDiffViewerSettings(diffSettings, store);
	}

	public static DiffViewerSettings loadDiffViewerSettings() {
		return SVNTeamDiffViewerPage.loadDiffViewerSettings(SVNTeamUIPlugin.instance().getPreferenceStore());
	}

	public static DiffViewerSettings loadDiffViewerSettings(IPreferenceStore store) {
		DiffViewerSettings diffSettings = new DiffViewerSettings();

		String encodedString = SVNTeamPreferences.getDiffViewerString(store,
				SVNTeamPreferences.DIFF_VIEWER_RESOURCES_SPECIFIC_PARAMETERS);
		String[] stringArray = FileUtility.decodeStringToArray(encodedString);
		if (stringArray.length > 0 && stringArray.length % ResourceSpecificParameters.FIELDS_COUNT == 0) {
			int paramsCount = stringArray.length / ResourceSpecificParameters.FIELDS_COUNT;
			for (int i = 0; i < paramsCount; i++) {
				String[] strings = new String[ResourceSpecificParameters.FIELDS_COUNT];
				for (int j = 0; j < ResourceSpecificParameters.FIELDS_COUNT; j++) {
					strings[j] = stringArray[i * ResourceSpecificParameters.FIELDS_COUNT + j];
				}
				ResourceSpecificParameters param = ResourceSpecificParameters.createFromStrings(strings);
				if (param != null) {
					diffSettings.addResourceSpecificParameters(param);
				}
			}
		}

		return diffSettings;
	}

	public static void saveDiffViewerSettings(DiffViewerSettings diffSettings, IPreferenceStore store) {
		SVNTeamDiffViewerPage.saveDiffViewerSettings(diffSettings, store, false);
	}

	public static void saveDiffViewerSettings(DiffViewerSettings diffSettings, IPreferenceStore store,
			boolean isDefault) {
		ResourceSpecificParameters[] resourceParams = diffSettings.getResourceSpecificParameters();
		if (resourceParams.length > 0) {
			String[] stringArray = new String[ResourceSpecificParameters.FIELDS_COUNT * resourceParams.length];
			for (int i = 0; i < resourceParams.length; i++) {
				ResourceSpecificParameters resourceParam = resourceParams[i];
				String[] strings = resourceParam.getAsStrings();
				System.arraycopy(strings, 0, stringArray, ResourceSpecificParameters.FIELDS_COUNT * i, strings.length);
			}
			String encodedString = FileUtility.encodeArrayToString(stringArray);
			SVNTeamPreferences.setDiffViewerString(store, SVNTeamPreferences.DIFF_VIEWER_RESOURCES_SPECIFIC_PARAMETERS,
					encodedString, isDefault);
		} else {
			SVNTeamPreferences.setDiffViewerString(store, SVNTeamPreferences.DIFF_VIEWER_RESOURCES_SPECIFIC_PARAMETERS,
					"", isDefault); //$NON-NLS-1$
		}
	}
}
