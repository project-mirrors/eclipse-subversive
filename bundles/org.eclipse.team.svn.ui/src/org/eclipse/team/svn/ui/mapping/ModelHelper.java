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

package org.eclipse.team.svn.ui.mapping;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Simple helper for getting modelSync flag
 * 
 * @author Igor Burilo
 *
 */
public class ModelHelper {

	public static boolean isShowModelSync() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		return SVNTeamPreferences.getSynchronizeBoolean(store, SVNTeamPreferences.ENABLE_MODEL_SYNC_NAME);
	}
}
