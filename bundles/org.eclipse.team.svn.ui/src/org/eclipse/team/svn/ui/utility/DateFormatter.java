/*******************************************************************************
 * Copyright (c) 2008, 2023 Thomas Champagne and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Champagne - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.utility;

import java.util.Date;

import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Utility class for format date with the good preference. 
 * @author Thomas Champagne
 */
public final class DateFormatter {	
	
	private DateFormatter() {
	}
	
	public static String formatDate(long date) {
		return DateFormatter.formatDate(new Date(date));
	}

	public static String formatDate(Date date) {
		return SVNTeamPreferences.getDateFormat(SVNTeamUIPlugin.instance().getPreferenceStore()).format(date);
	}
}
