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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.console;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * SVN Console implementation
 * 
 * @author Alexander Gurov
 */
public class SVNConsole extends IOConsole implements IPropertyChangeListener {
	public static final String SVN_CONSOLE_TYPE = "org.eclipse.team.svn.ui.console.SVNConsole"; //$NON-NLS-1$

	protected IOConsoleOutputStream cmdStream;

	protected IOConsoleOutputStream okStream;

	protected IOConsoleOutputStream warningStream;

	protected IOConsoleOutputStream errorStream;

	protected boolean enabled;

	public SVNConsole() {
		super(SVNUIMessages.SVNConsole_Name, SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/console.gif")); //$NON-NLS-1$

		setType(SVNConsole.SVN_CONSOLE_TYPE);
	}

	public IConsoleStream getConsoleStream() {
		return new SVNConsoleStream();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().startsWith(SVNTeamPreferences.CONSOLE_BASE)) {
			UIMonitorUtility.getDisplay().asyncExec(SVNConsole.this::loadPreferences);
		}
	}

	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		IOConsolePage page = (IOConsolePage) super.createPage(view);
		page.setReadOnly();
		return page;
	}

	@Override
	protected void init() {
		if (!enabled) {
			super.init();

			setTabWidth(4);

			cmdStream = newOutputStream();
			okStream = newOutputStream();
			warningStream = newOutputStream();
			errorStream = newOutputStream();

			UIMonitorUtility.getDisplay().syncExec(SVNConsole.this::loadPreferences);
			JFaceResources.getFontRegistry().addListener(this);
			SVNTeamUIPlugin.instance().getPreferenceStore().addPropertyChangeListener(this);

			enabled = true;
		}
	}

	@Override
	protected void dispose() {
		if (enabled) {
			enabled = false;
			super.dispose();

			SVNTeamUIPlugin.instance().getPreferenceStore().removePropertyChangeListener(this);
			JFaceResources.getFontRegistry().removeListener(this);

			ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] { this });

			Color tmp1 = cmdStream.getColor();
			Color tmp2 = okStream.getColor();
			Color tmp3 = warningStream.getColor();
			Color tmp4 = errorStream.getColor();

			// unsupported in Eclipse IDE 3.0
			try {
				cmdStream.close();
			} catch (Exception ex) {
			}
			try {
				okStream.close();
			} catch (Exception ex) {
			}
			try {
				warningStream.close();
			} catch (Exception ex) {
			}
			try {
				errorStream.close();
			} catch (Exception ex) {
			}

			if (tmp1 != null) {
				tmp1.dispose();
			}
			if (tmp1 != null) {
				tmp2.dispose();
			}
			if (tmp1 != null) {
				tmp3.dispose();
			}
			if (tmp1 != null) {
				tmp4.dispose();
			}
		}
	}

	protected void loadPreferences() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();

		Color tmp = cmdStream.getColor();
		cmdStream.setColor(new Color(UIMonitorUtility.getDisplay(),
				SVNTeamPreferences.getConsoleRGB(store, SVNTeamPreferences.CONSOLE_CMD_COLOR_NAME)));
		if (tmp != null && !tmp.equals(cmdStream.getColor())) {
			tmp.dispose();
		}
		tmp = okStream.getColor();
		okStream.setColor(new Color(UIMonitorUtility.getDisplay(),
				SVNTeamPreferences.getConsoleRGB(store, SVNTeamPreferences.CONSOLE_OK_COLOR_NAME)));
		if (tmp != null && !tmp.equals(okStream.getColor())) {
			tmp.dispose();
		}
		tmp = warningStream.getColor();
		warningStream.setColor(new Color(UIMonitorUtility.getDisplay(),
				SVNTeamPreferences.getConsoleRGB(store, SVNTeamPreferences.CONSOLE_WRN_COLOR_NAME)));
		if (tmp != null && !tmp.equals(warningStream.getColor())) {
			tmp.dispose();
		}
		tmp = errorStream.getColor();
		errorStream.setColor(new Color(UIMonitorUtility.getDisplay(),
				SVNTeamPreferences.getConsoleRGB(store, SVNTeamPreferences.CONSOLE_ERR_COLOR_NAME)));
		if (tmp != null && !tmp.equals(errorStream.getColor())) {
			tmp.dispose();
		}

		if (SVNTeamPreferences.getConsoleBoolean(store, SVNTeamPreferences.CONSOLE_WRAP_ENABLED_NAME)) {
			setConsoleWidth(SVNTeamPreferences.getConsoleInt(store, SVNTeamPreferences.CONSOLE_WRAP_WIDTH_NAME));
		} else {
			setConsoleWidth(-1);
		}

		if (SVNTeamPreferences.getConsoleBoolean(store, SVNTeamPreferences.CONSOLE_LIMIT_ENABLED_NAME)) {
			int limit = SVNTeamPreferences.getConsoleInt(store, SVNTeamPreferences.CONSOLE_LIMIT_VALUE_NAME);
			setWaterMarks(1000 < limit ? 1000 : limit - 1, limit);
		} else {
			setWaterMarks(-1, 0);
		}

		SVNConsole.this.setFont(PlatformUI.getWorkbench()
				.getThemeManager()
				.getCurrentTheme()
				.getFontRegistry()
				.get(SVNTeamPreferences.fullConsoleName(SVNTeamPreferences.CONSOLE_FONT_NAME)));
	}

	protected boolean canShowConsoleAutomatically(int severity) {
		int autoshow = SVNTeamPreferences.getConsoleInt(SVNTeamUIPlugin.instance().getPreferenceStore(),
				SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_NAME);
		return autoshow == SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_ALWAYS
				|| autoshow == SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_ERROR && severity == IConsoleStream.LEVEL_ERROR
				|| autoshow == SVNTeamPreferences.CONSOLE_AUTOSHOW_TYPE_WARNING_ERROR
						&& (severity == IConsoleStream.LEVEL_ERROR || severity == IConsoleStream.LEVEL_WARNING);
	}

	protected class SVNConsoleStream implements IConsoleStream {
		protected long start;

		protected String buffer;

		protected boolean outputStarted;

		protected boolean hasError;

		protected boolean hasWarning;

		protected boolean activated;

		protected boolean cancelled;

		public SVNConsoleStream() {

		}

		@Override
		public void markEnd() {
			if (outputStarted) {
				write(IConsoleStream.LEVEL_CMD, "*** "); //$NON-NLS-1$
				if (hasError) {
					write(IConsoleStream.LEVEL_ERROR, SVNUIMessages.SVNConsole_Error);
				} else if (hasWarning) {
					write(IConsoleStream.LEVEL_WARNING, SVNUIMessages.SVNConsole_Warning);
				} else {
					write(IConsoleStream.LEVEL_CMD,
							cancelled ? SVNUIMessages.SVNConsole_Cancelled : SVNUIMessages.SVNConsole_Ok);
				}
				write(
						IConsoleStream.LEVEL_CMD, " " //$NON-NLS-1$
								+ BaseMessages
										.format(SVNUIMessages.SVNConsole_Took,
												new String[] { new SimpleDateFormat("mm:ss.SSS") //$NON-NLS-1$
														.format(new Date(System.currentTimeMillis() - start)) })
								+ "\n\n"); //$NON-NLS-1$
			}
		}

		@Override
		public void markStart(String data) {
			start = System.currentTimeMillis();
			buffer = data;
		}

		@Override
		public void doComplexWrite(Runnable runnable) {
			flushBuffer();
			runnable.run();
		}

		@Override
		public void write(int severity, String data) {
			flushBuffer();

			if (!activated && canShowConsoleAutomatically(severity)) {
				if (!enabled) {
					SVNConsoleFactory.showConsole();
				} else {
					ConsolePlugin.getDefault().getConsoleManager().showConsoleView(SVNConsole.this);
				}
				activated = true;
			}

			if (enabled && activated && !cmdStream.isClosed()) {
				switch (severity) {
					case IConsoleStream.LEVEL_CMD: {
						print(cmdStream, data);
						break;
					}
					case IConsoleStream.LEVEL_OK: {
						print(okStream, data);
						break;
					}
					case IConsoleStream.LEVEL_WARNING: {
						hasWarning = true;
						print(warningStream, data);
						break;
					}
					case IConsoleStream.LEVEL_ERROR:
					default: {
						hasError = true;
						print(errorStream, data);
						break;
					}
				}
			}
		}

		@Override
		public void markCancelled() {
			cancelled = true;
		}

		protected void print(final IOConsoleOutputStream stream, final String data) {
			try {
				stream.write(data);
			} catch (IOException ex) {
				UILoggedOperation.reportError(this.getClass().getName(), ex);
			}
		}

		protected void flushBuffer() {
			outputStarted = true;
			if (buffer != null) {
				String tmp = buffer;
				buffer = null;
				write(IConsoleStream.LEVEL_CMD, "*** "); //$NON-NLS-1$
				write(IConsoleStream.LEVEL_CMD, tmp);
				write(IConsoleStream.LEVEL_CMD, "\n"); //$NON-NLS-1$
			}
		}

	}

}
