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

import org.eclipse.team.svn.ui.SVNTeamUIPlugin;

/**
 * This storage allow us to handle comment, user name etc. histories in one way
 * 
 * @author Alexander Gurov
 */
public class InputHistory {
	public static final int TYPE_BOOLEAN = 0;

	public static final int TYPE_DOUBLE = 1;

	public static final int TYPE_FLOAT = 2;

	public static final int TYPE_INT = 3;

	public static final int TYPE_LONG = 4;

	public static final int TYPE_STRING = 5;

	protected static final String HISTORY_NAME_BASE = "history."; //$NON-NLS-1$

	protected String name;

	protected int type;

	protected Object value;

	public InputHistory(String name, int type, Object defaultValue) {
		this.name = name;
		this.type = type;

		loadHistoryValue(defaultValue);
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	public void clear() {
		value = null;
		saveHistoryValue();
	}

	public void setValue(Object value) {
		this.value = value;
		saveHistoryValue();
	}

	protected void loadHistoryValue(Object defaultValue) {
		String fullName = UserInputHistory.HISTORY_NAME_BASE + name;
		switch (type) {
			case InputHistory.TYPE_BOOLEAN: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setDefault(fullName, defaultValue == null ? false : ((Boolean) defaultValue));
				value = SVNTeamUIPlugin.instance().getPreferenceStore().getBoolean(fullName);
				break;
			}
			case InputHistory.TYPE_DOUBLE: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setDefault(fullName, defaultValue == null ? 0 : ((Double) defaultValue));
				value = SVNTeamUIPlugin.instance().getPreferenceStore().getDouble(fullName);
				break;
			}
			case InputHistory.TYPE_FLOAT: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setDefault(fullName, defaultValue == null ? 0 : ((Float) defaultValue));
				value = SVNTeamUIPlugin.instance().getPreferenceStore().getFloat(fullName);
				break;
			}
			case InputHistory.TYPE_INT: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setDefault(fullName, defaultValue == null ? 0 : ((Integer) defaultValue));
				value = SVNTeamUIPlugin.instance().getPreferenceStore().getInt(fullName);
				break;
			}
			case InputHistory.TYPE_LONG: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setDefault(fullName, defaultValue == null ? 0 : ((Long) defaultValue));
				value = SVNTeamUIPlugin.instance().getPreferenceStore().getLong(fullName);
				break;
			}
			default: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setDefault(fullName, defaultValue == null ? "" : (String) defaultValue); //$NON-NLS-1$
				value = SVNTeamUIPlugin.instance().getPreferenceStore().getString(fullName);
			}
		}
	}

	protected void saveHistoryValue() {
		Object value = this.value;
		String fullName = UserInputHistory.HISTORY_NAME_BASE + name;
		switch (type) {
			case InputHistory.TYPE_BOOLEAN: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setValue(fullName, value == null ? false : ((Boolean) value));
				break;
			}
			case InputHistory.TYPE_DOUBLE: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setValue(fullName, value == null ? 0 : ((Double) value));
				break;
			}
			case InputHistory.TYPE_FLOAT: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setValue(fullName, value == null ? 0 : ((Float) value));
				break;
			}
			case InputHistory.TYPE_INT: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setValue(fullName, value == null ? 0 : ((Integer) value));
				break;
			}
			case InputHistory.TYPE_LONG: {
				SVNTeamUIPlugin.instance()
						.getPreferenceStore()
						.setValue(fullName, value == null ? 0 : ((Long) value));
				break;
			}
			default: {
				SVNTeamUIPlugin.instance().getPreferenceStore().setValue(fullName, value == null ? "" : (String) value); //$NON-NLS-1$
			}
		}
	}

}
