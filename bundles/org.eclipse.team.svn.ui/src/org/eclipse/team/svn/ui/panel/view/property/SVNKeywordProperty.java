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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.view.property;

import java.util.StringTokenizer;

import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * SVN keyword property
 * 
 * @author Sergiy Logvin
 */
public class SVNKeywordProperty {

	// names:
	public static final String[] DATE_NAMES = { "LastChangedDate", "Date" }; //$NON-NLS-1$ //$NON-NLS-2$

	public static final String[] REVISION_NAMES = { "LastChangedRevision", "Rev" }; //$NON-NLS-1$ //$NON-NLS-2$

	public static final String[] AUTHOR_NAMES = { "LastChangedBy", "Author" }; //$NON-NLS-1$ //$NON-NLS-2$

	public static final String[] HEAD_URL_NAMES = { "HeadURL", "URL" }; //$NON-NLS-1$ //$NON-NLS-2$

	public static final String[] ID_NAMES = { "Id" }; //$NON-NLS-1$

	public static final String[] HEADER_NAMES = { "Header" }; //$NON-NLS-1$

	// descriptions:
	public static String DATE_DESCR() {
		return SVNUIMessages.SVNKeywordProperty_DATE_DESCR;
	}

	public static String REVISION_DESCR() {
		return SVNUIMessages.SVNKeywordProperty_REVISION_DESCR;
	}

	public static String AUTHOR_DESCR() {
		return SVNUIMessages.SVNKeywordProperty_AUTHOR_DESCR;
	}

	public static String HEAD_URL_DESCR() {
		return SVNUIMessages.SVNKeywordProperty_HEAD_URL_DESCR;
	}

	public static String ID_DESCR() {
		return SVNUIMessages.SVNKeywordProperty_ID_DESCR;
	}

	public static String HEADER_DESCR() {
		return SVNUIMessages.SVNKeywordProperty_HEADER_DESCR;
	}

	// samples:
	public static final String DATE_SAMPLE = "$LastChangedDate: 2006-08-07 15:40:37 +0000 (Mon, 08 Aug 2006) $"; //$NON-NLS-1$

	public static final String REVISION_SAMPLE = "$LastChangedRevision: 7206 $"; //$NON-NLS-1$

	public static final String AUTHOR_SAMPLE = "$LastChangedBy: J.M.Wade $"; //$NON-NLS-1$

	public static final String HEAD_URL_SAMPLE = "$HeadURL: http://svn.eclipse.org/community/Subversive/src/ui/PropertyKeywordEditPanel.java $"; //$NON-NLS-1$

	public static final String ID_SAMPLE = "$Id: PropertyKeywordEditPanel.java 7206 2006-08-07 15:40:37 J.M.Wade $"; //$NON-NLS-1$

	public static final String HEADER_SAMPLE = "$Header: http://svn.example.com/repos/trunk/calc.c 148 2006-07-28 21:30:43Z sally $"; //$NON-NLS-1$

	protected boolean dateEnabled;

	protected boolean revisionEnabled;

	protected boolean lastChangedByEnabled;

	protected boolean headURLEnabled;

	protected boolean idEnabled;

	protected boolean headerEnabled;

	public SVNKeywordProperty(String keywordsValue) {
		if (keywordsValue != null) {
			parsePropertyValue(keywordsValue);
		}
	}

	public boolean isHeadUrlEnabled() {
		return headURLEnabled;
	}

	public boolean isIdEnabled() {
		return idEnabled;
	}

	public boolean isHeaderEnabled() {
		return headerEnabled;
	}

	public boolean isLastChangedByEnabled() {
		return lastChangedByEnabled;
	}

	public boolean isDateEnabled() {
		return dateEnabled;
	}

	public boolean isRevisionEnabled() {
		return revisionEnabled;
	}

	public void setHeadUrlEnabled(boolean headURL) {
		headURLEnabled = headURL;
	}

	public void setIdEnabled(boolean id) {
		idEnabled = id;
	}

	public void setHeaderEnabled(boolean header) {
		headerEnabled = header;
	}

	public void setLastChangedByEnabled(boolean lastChangedBy) {
		lastChangedByEnabled = lastChangedBy;
	}

	public void setDateEnabled(boolean lastChangedDate) {
		dateEnabled = lastChangedDate;
	}

	public void setRevisionEnabled(boolean lastChangedRev) {
		revisionEnabled = lastChangedRev;
	}

	@Override
	public String toString() {
		String result = ""; //$NON-NLS-1$
		if (dateEnabled) {
			result = addKeyword(result, SVNKeywordProperty.DATE_NAMES[0]);
		}
		if (revisionEnabled) {
			result = addKeyword(result, SVNKeywordProperty.REVISION_NAMES[0]);
		}
		if (lastChangedByEnabled) {
			result = addKeyword(result, SVNKeywordProperty.AUTHOR_NAMES[0]);
		}
		if (headURLEnabled) {
			result = addKeyword(result, SVNKeywordProperty.HEAD_URL_NAMES[0]);
		}
		if (idEnabled) {
			result = addKeyword(result, SVNKeywordProperty.ID_NAMES[0]);
		}
		if (headerEnabled) {
			result = addKeyword(result, SVNKeywordProperty.HEADER_NAMES[0]);
		}

		return result;
	}

	protected String addKeyword(String result, String keyword) {
		if (result.trim().length() > 0) {
			result += ' ';
		}
		result += keyword;

		return result;
	}

	protected void parsePropertyValue(String keywordValue) {
		StringTokenizer st = new StringTokenizer(keywordValue, " "); //$NON-NLS-1$
		while (st.hasMoreTokens()) {
			String name = st.nextToken();
			if (name.equals(SVNKeywordProperty.DATE_NAMES[0]) || name.equals(SVNKeywordProperty.DATE_NAMES[1])) {
				dateEnabled = true;
			} else if (name.equals(SVNKeywordProperty.REVISION_NAMES[0])
					|| name.equals(SVNKeywordProperty.REVISION_NAMES[1])) {
				revisionEnabled = true;
			} else if (name.equals(SVNKeywordProperty.AUTHOR_NAMES[0])
					|| name.equals(SVNKeywordProperty.AUTHOR_NAMES[1])) {
				lastChangedByEnabled = true;
			} else if (name.equals(SVNKeywordProperty.HEAD_URL_NAMES[0])
					|| name.equals(SVNKeywordProperty.HEAD_URL_NAMES[1])) {
				headURLEnabled = true;
			} else if (name.equals(SVNKeywordProperty.ID_NAMES[0])) {
				idEnabled = true;
			} else if (name.equals(SVNKeywordProperty.HEADER_NAMES[0])) {
				headerEnabled = true;
			}
		}
	}

}
