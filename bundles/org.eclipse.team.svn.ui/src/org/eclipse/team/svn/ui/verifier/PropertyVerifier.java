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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.verifier;

import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Property verifier. Checks the property value to match property regExp
 * 
 * @author Alexei Goncharov
 */
public class PropertyVerifier extends AbstractFormattedVerifier {
	private IRepositoryResource base;

	private Pattern pattern;

	private String propName;

	private boolean toValidate;

	public PropertyVerifier(String fieldName, String regExp, String propName, IRepositoryResource base) {
		super(fieldName);
		if (regExp == null) {
			toValidate = false;
		} else {
			toValidate = true;
			pattern = Pattern.compile(regExp);
		}
		this.base = base;
		String[] parts = propName.split(":"); //$NON-NLS-1$
		this.propName = ""; //$NON-NLS-1$
		for (String part : parts) {
			this.propName += part;
		}
	}

	@Override
	protected String getErrorMessageImpl(Control input) {
		if (!toValidate || propName.equals("svnlog") || propName.equals("svnauthor")) { //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		String inputText = getText(input);
		if (propName.equals("bugtraqlogregex")) { //$NON-NLS-1$
			try {
				String[] logs = inputText.split("\r\n"); //$NON-NLS-1$
				for (String log : logs) {
					Pattern.compile(log);
				}
			} catch (Exception ex) {
				return SVNUIMessages.getString("PropertyEditPanel_Verifier_" + propName); //$NON-NLS-1$
			}
			return null;
		}
		if (propName.equals("bugtraqmessage")) { //$NON-NLS-1$
			if (!inputText.contains("%BUGID%")) { //$NON-NLS-1$
				return SVNUIMessages.getString("PropertyEditPanel_Verifier_" + propName); //$NON-NLS-1$
			}
			return null;
		}
		if (propName.equals("svnexternals")) { //$NON-NLS-1$
			try {
				SVNUtility.parseSVNExternalsProperty(inputText, base);
			} catch (Exception ex) {
				return SVNUIMessages.getString("PropertyEditPanel_Verifier_" + propName); //$NON-NLS-1$
			}
			return null;
		}
		if (propName.equals("svndate")) { //$NON-NLS-1$
			try {
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(inputText); //$NON-NLS-1$
			} catch (Exception ex) {
				return BaseMessages.format("PropertyEditPanel_Verifier_" + propName, //$NON-NLS-1$
						new String[] { "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" }); //$NON-NLS-1$
			}
			return null;
		}
		Matcher matcher = pattern.matcher(inputText);
		if (!matcher.matches()) {
			String retVal = SVNUIMessages.getString("PropertyEditPanel_Verifier_" + propName); //$NON-NLS-1$
			if (retVal.equals("PropertyEditPanel_Verifier_" + propName)) { //$NON-NLS-1$
				return BaseMessages.format(SVNUIMessages.PropertyEditPanel_regExp_Verifier,
						new String[] { pattern.pattern() });
			}
			return SVNUIMessages.getString("PropertyEditPanel_Verifier_" + propName); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	protected String getWarningMessageImpl(Control input) {
		if (!toValidate) {
			return null;
		}
		if (propName.equals("svnauthor") //$NON-NLS-1$
				|| propName.equals("svnlog") //$NON-NLS-1$
				|| propName.equals("svndate")) { //$NON-NLS-1$
			return SVNUIMessages.getString("PropertyEditPanel_Verifier_Warning_" + propName); //$NON-NLS-1$
		}
		return null;
	}

}
