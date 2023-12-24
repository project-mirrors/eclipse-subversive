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
 *    Igor Burilo (Polarion Software) - initial API and implementation
 *    Yann Andenmatten - [patch] add default preference initializer
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/
package org.eclipse.team.svn.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;

/**
 * Default preferences initializer
 * 
 * @author Yann Andenmatten
 */
public class SVNTeamPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		SVNTeamPreferences.setDefaultValues(SVNTeamUIPlugin.instance().getPreferenceStore());
	}
}
