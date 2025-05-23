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

package org.eclipse.team.svn.ui.properties;

import org.eclipse.team.svn.core.extension.properties.PredefinedProperty;
import org.eclipse.team.svn.core.extension.properties.PredefinedPropertySet;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.preferences.SVNTeamPropsPreferencePage;

/**
 * Custom properties provider
 *
 * @author Alexander Gurov
 */
public class CustomPropertySet extends PredefinedPropertySet {

	@Override
	protected void init() {
		registerProperty(new PredefinedProperty(SVNUIMessages.AbstractPropertyEditPanel_custom_description,
				PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON));
		PredefinedProperty[] customProps = SVNTeamPropsPreferencePage.loadCustomProperties(
				SVNTeamPreferences.getCustomPropertiesList(SVNTeamUIPlugin.instance().getPreferenceStore(),
						SVNTeamPreferences.CUSTOM_PROPERTIES_LIST_NAME));
		if (customProps.length > 0) {
			this.registerProperties(customProps);
		} else {
			registerProperty(new PredefinedProperty("    " + SVNUIMessages.AbstractPropertyEditPanel_custom_hint, //$NON-NLS-1$
					PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON));
		}
	}

}
