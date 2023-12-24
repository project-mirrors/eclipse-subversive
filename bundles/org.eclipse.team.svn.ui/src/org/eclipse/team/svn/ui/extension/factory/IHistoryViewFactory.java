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

/**
 * Add additional function to a commit message history view Such as click on a link.
 * 
 * @author Andrej Zachar
 */
public interface IHistoryViewFactory {
	/**
	 * Returns project-specific multi-line comment view implementation
	 * 
	 * @return project-specific multi-line comment view implementation
	 */
	public ICommentView getCommentView();
}
