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
 *    Alexey Mikoyan - Initial implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/
package org.eclipse.team.svn.ui.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

/**
 * Listen to SVN console. Finds lines that contains local resources paths. Adds hyperlinks to local resources.
 *
 * @author Alexey Mikoyan
 *
 */

public class LocalPathMatcher implements IPatternMatchListenerDelegate, IPropertyChangeListener {

	protected Pattern pattern;

	protected TextConsole console;

	protected boolean enabled;

	public void createPattern() {
		String regExp = "(?:\\s|\")(?:[A-Z]\\:)?(?:[\\\\/][^\\\\/\\:\\?\\*\r\n\"]+)+"; //$NON-NLS-1$
		pattern = Pattern.compile(regExp);
	}

	@Override
	public void connect(TextConsole console) {
		this.console = console;
		createPattern();
		loadPreferences();
		SVNTeamUIPlugin.instance().getPreferenceStore().addPropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().startsWith(SVNTeamPreferences.CONSOLE_BASE)) {
			loadPreferences();
		}
	}

	@Override
	public void disconnect() {
		console = null;
		SVNTeamUIPlugin.instance().getPreferenceStore().removePropertyChangeListener(this);
	}

	@Override
	public void matchFound(PatternMatchEvent event) {
		if (console == null || !enabled) {
			return;
		}
		UIMonitorUtility.doTaskBusyDefault(new AddConsoleHyperlinkOperation(event));
	}

	protected void loadPreferences() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		enabled = SVNTeamPreferences.getConsoleBoolean(store, SVNTeamPreferences.CONSOLE_HYPERLINKS_ENABLED_NAME);
	}

	protected class AddConsoleHyperlinkOperation extends AbstractActionOperation {
		protected PatternMatchEvent event;

		public AddConsoleHyperlinkOperation(PatternMatchEvent event) {
			super("Operation_AddConsoleHyperlink", SVNUIMessages.class); //$NON-NLS-1$
			this.event = event;
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			int offset = event.getOffset();
			int length = event.getLength();
			String path = console.getDocument().get(offset, length);
			if (path == null) {
				return;
			}

			Matcher matcher = pattern.matcher(path);
			if (matcher.find(0)) {
				String link = matcher.group(matcher.groupCount()).trim();
				console.addHyperlink(new LocalFileHyperlink(link), offset + matcher.start(matcher.groupCount()) + 1,
						matcher.group(matcher.groupCount()).length() - 1);
			}
		}

	}

}
