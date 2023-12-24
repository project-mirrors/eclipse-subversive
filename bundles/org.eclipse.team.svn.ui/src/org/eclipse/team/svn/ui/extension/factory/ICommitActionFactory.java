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

import java.util.Collection;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IRevisionProvider;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Visual and operation components factory. Used to extend Subversive without direct dependencies.
 * 
 * @author Andrej Zachar
 */
public interface ICommitActionFactory {

	/**
	 * Called upon the CommentComposite initialization.
	 * 
	 * @param commentManager
	 */
	void initCommentManager(ICommentManager commentManager);

	/**
	 * Called each time "Ok" button is pressed.
	 * 
	 * @param commentManager
	 */
	void confirmMessage(ICommentManager commentManager);

	/**
	 * Called each time "Cancel" button is pressed.
	 * 
	 * @param commentManager
	 */
	void cancelMessage(ICommentManager commentManager);

	/**
	 * The method provide abilities in extending of the standard Subversive Commit Dialog to more powerful
	 * 
	 * @param shell
	 *            Shell instance which will be used to interact with user
	 * @param allFilesToCommit
	 *            full set of files which will be committed
	 * @param panel
	 *            the default Subversive Commit Panel implementation
	 * @return enahanced Commit Dialog
	 */
	ICommitDialog getCommitDialog(Shell shell, Collection allFilesToCommit, ICommentDialogPanel panel);

	/**
	 * The method allows customizing of the Commit Operation
	 * 
	 * @param operation
	 *            prepared Commit operation
	 * @param revisionProvider
	 *            committed revision provider
	 * @param dependsOn
	 *            dependencies which can prevent commit operation execution in case of failure
	 * @param part
	 *            workbench part which will be used to interact with user
	 */
	void performAfterCommitTasks(CompositeOperation operation, IRevisionProvider revisionProvider,
			IActionOperation[] dependsOn, IWorkbenchPart part);

}
