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

package org.eclipse.team.svn.ui.panel.common;

import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Select tag folder panel
 * 
 * @author Alexander Gurov
 */
public class TagPanel extends AbstractBranchTagPanel {

	public TagPanel(IRepositoryRoot tagTo, boolean showStartsWith, Set existingNames,
			IRepositoryResource[] selectedRemoteResources) {
		this(tagTo, showStartsWith, existingNames, new IResource[0], selectedRemoteResources);
	}

	public TagPanel(IRepositoryRoot tagTo, boolean showStartsWith, Set existingNames, IResource[] resources,
			IRepositoryResource[] selectedRemoteResources) {
		super(tagTo, showStartsWith, existingNames, "TagPanel", "tag", resources, selectedRemoteResources);
	}

	@Override
	public void createControlsImpl(Composite parent) {
		super.createControlsImpl(parent);
		if (startsWith && SVNTeamPreferences.getRepositoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.REPOSITORY_FORCE_EXTERNALS_FREEZE_NAME)) {
			freezeExternalsCheck.setSelection(true);
			freezeExternalsCheck.setEnabled(false);
		}
	}

	@Override
	protected void creationModeChanged(int creationMode) {
		freezeExternalsCheck.setSelection(creationMode != SVNTeamPreferences.CREATION_MODE_REPOSITORY);
		revisionComposite.setEnabled(creationMode == SVNTeamPreferences.CREATION_MODE_REPOSITORY);
	}

	@Override
	public String getHelpId() {
		return "org.eclipse.team.svn.help.tagDialogContext"; //$NON-NLS-1$
	}

}
