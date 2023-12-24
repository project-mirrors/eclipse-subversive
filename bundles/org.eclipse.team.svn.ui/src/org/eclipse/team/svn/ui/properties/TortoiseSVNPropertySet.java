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
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * TortoiseSVN properties specification
 *
 * @author Alexander Gurov
 */
public class TortoiseSVNPropertySet extends PredefinedPropertySet {

	@Override
	protected void init() {
		registerProperty(new PredefinedProperty(SVNUIMessages.PropertyEditPanel_tsvn_description,
				PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON));
		registerProperty(new PredefinedProperty("tsvn:logtemplate", SVNUIMessages.Property_TSVN_LogTemplate, "")); //$NON-NLS-1$ //$NON-NLS-2$
		registerProperty(new PredefinedProperty("tsvn:logwidthmarker", SVNUIMessages.Property_TSVN_LogWidthMarker, //$NON-NLS-1$
				"", "(\\d+)")); //$NON-NLS-1$ //$NON-NLS-2$
		registerProperty(
				new PredefinedProperty("tsvn:logminsize", SVNUIMessages.Property_TSVN_LogMinSize, "", "(\\d+)")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		registerProperty(new PredefinedProperty("tsvn:lockmsgminsize", SVNUIMessages.Property_TSVN_LockMsgMinSize, //$NON-NLS-1$
				"", "(\\d+)")); //$NON-NLS-1$ //$NON-NLS-2$
		registerProperty(new PredefinedProperty("tsvn:logfilelistenglish", //$NON-NLS-1$
				SVNUIMessages.Property_TSVN_LogFileListEnglish, "", "((true)|(false))")); //$NON-NLS-1$ //$NON-NLS-2$
		registerProperty(
				new PredefinedProperty("tsvn:projectlanguage", SVNUIMessages.Property_TSVN_ProjectLanguage, "")); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
