/*******************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *   See git history
 *******************************************************************************/

package org.eclipse.team.svn.ui.console;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Console helper that allows contributing actions to the console view when the SVN console is visible. Added to the console from
 * org.eclipse.ui.console.consolePageParticipants extension point.
 * 
 * @author Igor Burilo
 */
public class SVNConsolePageParticipant implements IConsolePageParticipant {

	private SVNConsoleRemoveAction consoleRemoveAction;

	@Override
	public void init(IPageBookViewPage page, IConsole console) {
		consoleRemoveAction = new SVNConsoleRemoveAction();
		IActionBars bars = page.getSite().getActionBars();
		bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleRemoveAction);
	}

	@Override
	public void dispose() {
		consoleRemoveAction = null;
	}

	@Override
	public void activated() {

	}

	@Override
	public void deactivated() {

	}

	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

}
