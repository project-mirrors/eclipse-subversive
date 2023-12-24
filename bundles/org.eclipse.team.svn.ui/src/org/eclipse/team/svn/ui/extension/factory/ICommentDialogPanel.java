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
 *    Andrej Zachar - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.extension.factory;

import org.eclipse.team.svn.ui.event.IResourceSelectionChangeListener;
import org.eclipse.team.svn.ui.panel.IDialogPanel;

/**
 * Interface for communication with comment module in commit office.
 * 
 * @author Andrej Zachar
 */
public interface ICommentDialogPanel extends IDialogPanel {

	/**
	 * Returns the comment message from the editor. The call returns valid data outside of the UI thread context too.
	 * 
	 * @return
	 */
	String getMessage();

	/**
	 * Adds resources selection change listener
	 */
	void addResourcesSelectionChangedListener(IResourceSelectionChangeListener listener);

	/**
	 * Removes resources selection change listener
	 */
	void removeResourcesSelectionChangedListener(IResourceSelectionChangeListener listener);

}
