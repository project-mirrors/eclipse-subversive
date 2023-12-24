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
 * Bugtraq properties specification
 *
 * @author Alexander Gurov
 */
public class BugtraqPropertySet extends PredefinedPropertySet {

	@Override
	protected void init() {
		registerProperty(new PredefinedProperty(SVNUIMessages.PropertyEditPanel_bugtraq_description,
				PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON));
		registerProperty(new PredefinedProperty("bugtraq:url", SVNUIMessages.Property_Bugtraq_URL, "%BUGID%", //$NON-NLS-1$//$NON-NLS-2$
				"(http:\\/|https:\\/|\\^|\\/|\\.\\.)\\/\\S*(\\%BUGID\\%)\\S*")); //$NON-NLS-1$
		registerProperty(new PredefinedProperty("bugtraq:logregex", SVNUIMessages.Property_Bugtraq_LogRegex, "")); //$NON-NLS-1$ //$NON-NLS-2$
		registerProperty(new PredefinedProperty("bugtraq:label", SVNUIMessages.Property_Bugtraq_Label, "")); //$NON-NLS-1$ //$NON-NLS-2$
		registerProperty(
				new PredefinedProperty("bugtraq:message", SVNUIMessages.Property_Bugtraq_Message, "%BUGID%")); //$NON-NLS-1$ //$NON-NLS-2$
		registerProperty(new PredefinedProperty("bugtraq:number", SVNUIMessages.Property_Bugtraq_Number, "", //$NON-NLS-1$//$NON-NLS-2$
				"((true)|(false))")); //$NON-NLS-1$
		registerProperty(new PredefinedProperty("bugtraq:warnifnoissue", //$NON-NLS-1$
				SVNUIMessages.Property_Bugtraq_WarnIfNoIssue, "", "((true)|(false))")); //$NON-NLS-1$ //$NON-NLS-2$
		registerProperty(new PredefinedProperty("bugtraq:append", SVNUIMessages.Property_Bugtraq_Append, "", //$NON-NLS-1$//$NON-NLS-2$
				"((true)|(false))")); //$NON-NLS-1$
	}

}
