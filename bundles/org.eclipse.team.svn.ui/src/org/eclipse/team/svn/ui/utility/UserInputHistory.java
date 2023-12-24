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
 *    Alexander Gurov - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.utility;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * This storage allow us to handle comment, user name etc. histories in one way
 * 
 * @author Alexander Gurov
 */
public class UserInputHistory extends InputHistory {

	protected static final String HISTORY_NAME_BASE = "history."; //$NON-NLS-1$

	protected final static String ENCODING = "UTF-8";

	protected int depth;

	protected List history;

	public UserInputHistory(String name) {
		this(name, SVNTeamPreferences.getCommentTemplatesInt(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.COMMENT_SAVED_PATHS_COUNT_NAME));
	}

	public UserInputHistory(String name, int depth) {
		super(name, InputHistory.TYPE_STRING, null);
		this.depth = depth;

		loadHistoryLines();

		if (history.size() > this.depth) {
			ListIterator iter = history.listIterator(this.depth);
			while (iter.hasNext()) {
				iter.next();
				iter.remove();
			}
			saveHistoryLines();
		}
	}

	public int getDepth() {
		return depth;
	}

	public String[] getHistory() {
		return (String[]) history.toArray(new String[history.size()]);
	}

	public void addLine(String line) {
		if (line == null || line.trim().length() == 0) {
			return;
		}
		history.remove(line);
		history.add(0, line);
		if (history.size() > depth) {
			history.remove(history.size() - 1);
		}
		saveHistoryLines();
	}

	@Override
	public void clear() {
		history.clear();
		super.clear();
	}

	protected void loadHistoryLines() {
		history = new ArrayList();
		String historyData = (String) value;
		if (historyData != null && historyData.length() > 0) {
			String[] historyArray = historyData.split(";"); //$NON-NLS-1$
			for (int i = 0; i < historyArray.length; i++) {
				try {
					historyArray[i] = new String(Base64.decode(historyArray[i].getBytes(UserInputHistory.ENCODING)),
							UserInputHistory.ENCODING);
				} catch (UnsupportedEncodingException e) {
					historyArray[i] = new String(Base64.decode(historyArray[i].getBytes()));
				}
			}
			history.addAll(Arrays.asList(historyArray));
		}
	}

	protected void saveHistoryLines() {
		String result = ""; //$NON-NLS-1$
		for (Iterator it = history.iterator(); it.hasNext();) {
			String str = (String) it.next();
			try {
				str = new String(Base64.encode(str.getBytes(UserInputHistory.ENCODING)), UserInputHistory.ENCODING);
			} catch (UnsupportedEncodingException e) {
				str = new String(Base64.encode(str.getBytes()));
			}
			result += result.length() == 0 ? str : ";" + str; //$NON-NLS-1$
		}
		setValue(result);
	}

}
