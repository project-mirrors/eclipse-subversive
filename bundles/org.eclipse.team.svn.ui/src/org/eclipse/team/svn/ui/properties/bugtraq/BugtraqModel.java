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
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *    Gabor Liptak - Speedup Pattern's usage
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.properties.bugtraq;

import org.eclipse.team.svn.core.utility.PatternProvider;

/**
 * The model which represents bugtraq properties
 * 
 * @author Sergiy Logvin
 */
public class BugtraqModel {
	public static final String BUG_ID = "%BUGID%"; //$NON-NLS-1$

	protected String url;

	protected String label;

	protected String message;

	protected String[] logregex;

	protected boolean warnIfNoIssue;

	protected boolean append = true;

	protected boolean number = true;

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

	public String getLabel() {
		return label == null ? "Bug-ID:" : label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message == null || message.trim().length() == 0 ? null : message;
	}

	public boolean isNumber() {
		return number;
	}

	public void setNumber(boolean number) {
		this.number = number;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isWarnIfNoIssue() {
		return warnIfNoIssue;
	}

	public void setWarnIfNoIssue(boolean warnifnoissue) {
		warnIfNoIssue = warnifnoissue;
	}

	public String[] getLogregex() {
		return logregex;
	}

	public void setLogregex(String logregex) {
		this.logregex = logregex.split("\r\n|\r|\n"); //$NON-NLS-1$
	}

	public boolean isDoubleLogRegexp() {
		if (logregex != null) {
			return logregex.length == 2;
		}
		return false;
	}

	public String getResultingURL(LinkList.LinkPlacement issue) {
		if (url != null && issue != null && url.indexOf(BugtraqModel.BUG_ID) != -1) {
			return PatternProvider.replaceAll(url, BugtraqModel.BUG_ID, issue.getURL());
		}
		return null;
	}

}
