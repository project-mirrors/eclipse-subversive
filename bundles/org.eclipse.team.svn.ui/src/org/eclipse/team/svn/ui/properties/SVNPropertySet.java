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

/**
 * SVN properties specification
 *
 * @author Alexander Gurov
 */
public class SVNPropertySet extends PredefinedPropertySet {

	protected void init() {
		this.registerProperty(new PredefinedProperty(SVNUIMessages.AbstractPropertyEditPanel_svn_description,
				PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON));
		this.registerProperty(new PredefinedProperty("svn:eol-style", SVNUIMessages.Property_SVN_EOL, "", //$NON-NLS-1$//$NON-NLS-2$
				"((native)|(LF)|(CR)|(CRLF))", PredefinedProperty.TYPE_FILE)); //$NON-NLS-1$
		this.registerProperty(new PredefinedProperty("svn:executable", SVNUIMessages.Property_SVN_Executable, "", null, //$NON-NLS-1$//$NON-NLS-2$
				PredefinedProperty.TYPE_FILE));
		this.registerProperty(new PredefinedProperty("svn:externals", SVNUIMessages.Property_SVN_Externals, "", null, //$NON-NLS-1$//$NON-NLS-2$
				PredefinedProperty.TYPE_FOLDER));
		if (SVNTeamPreferences.getPropertiesBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.IGNORE_MASK_VALIDATION_ENABLED_NAME)) {
			this.registerProperty(new PredefinedProperty("svn:ignore", SVNUIMessages.Property_SVN_Ignore, "", //$NON-NLS-1$//$NON-NLS-2$
					"([^\\\\/\\:])+", PredefinedProperty.TYPE_FOLDER)); //$NON-NLS-1$
		} else {
			this.registerProperty(new PredefinedProperty("svn:ignore", SVNUIMessages.Property_SVN_Ignore, "", null, //$NON-NLS-1$//$NON-NLS-2$
					PredefinedProperty.TYPE_FOLDER));
		}
		this.registerProperty(new PredefinedProperty("svn:keywords", SVNUIMessages.Property_SVN_Keywords, "", //$NON-NLS-1$//$NON-NLS-2$
				"((Date)|(Revision)|(Author)|(HeadURL)|(Id)|(LastChangedDate)|(Rev)|(LastChangedRevision)|(LastChangedBy)|(URL)|(\\s))+", //$NON-NLS-1$
				PredefinedProperty.TYPE_FILE));
		this.registerProperty(new PredefinedProperty("svn:mime-type", SVNUIMessages.Property_SVN_Mimetype, "", null, //$NON-NLS-1$//$NON-NLS-2$
				PredefinedProperty.TYPE_FILE));
		this.registerProperty(new PredefinedProperty("svn:mergeinfo", SVNUIMessages.Property_SVN_Mergeinfo, "")); //$NON-NLS-1$ //$NON-NLS-2$
		this.registerProperty(new PredefinedProperty("svn:needs-lock", SVNUIMessages.Property_SVN_NeedsLock, "", null, //$NON-NLS-1$//$NON-NLS-2$
				PredefinedProperty.TYPE_FILE));

		this.registerProperty(new PredefinedProperty(SVNUIMessages.AbstractPropertyEditPanel_revprop_description,
				PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_REVISION | PredefinedProperty.TYPE_COMMON));
		this.registerProperty(new PredefinedProperty("svn:log", SVNUIMessages.Property_SVN_Log, "", null, //$NON-NLS-1$//$NON-NLS-2$
				PredefinedProperty.TYPE_REVISION));
		this.registerProperty(new PredefinedProperty("svn:author", SVNUIMessages.Property_SVN_Author, "", null, //$NON-NLS-1$//$NON-NLS-2$
				PredefinedProperty.TYPE_REVISION));
		this.registerProperty(new PredefinedProperty("svn:date", SVNUIMessages.Property_SVN_Date, "", null, //$NON-NLS-1$//$NON-NLS-2$
				PredefinedProperty.TYPE_REVISION));
		this.registerProperty(new PredefinedProperty("svn:autoversioned", SVNUIMessages.Property_SVN_Autoversioned, "", //$NON-NLS-1$//$NON-NLS-2$
				null, PredefinedProperty.TYPE_REVISION));
	}

}
