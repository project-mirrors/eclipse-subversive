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

package org.eclipse.team.svn.ui.extension.impl;

import java.util.Collection;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IRevisionProvider;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.extension.factory.ICommentDialogPanel;
import org.eclipse.team.svn.ui.extension.factory.ICommentManager;
import org.eclipse.team.svn.ui.extension.factory.ICommitActionFactory;
import org.eclipse.team.svn.ui.extension.factory.ICommitDialog;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Default team commit components factory
 * 
 * @author Andrej Zachar
 */
public class DefaultCommitActionFactory implements ICommitActionFactory {

	@Override
	public ICommitDialog getCommitDialog(final Shell shell, Collection allFilesToCommit,
			final ICommentDialogPanel commentPanel) {
		return new ICommitDialog() {

			@Override
			public String getMessage() {
				return commentPanel.getMessage();
			}

			@Override
			public int open() {
				DefaultDialog dialog = new DefaultDialog(shell, commentPanel);
				return dialog.open();
			}

		};
	}

	@Override
	public void performAfterCommitTasks(CompositeOperation operation, IRevisionProvider revisionProvider,
			IActionOperation[] dependsOn, IWorkbenchPart part) {

	}

	@Override
	public void initCommentManager(ICommentManager commentManager) {
	}

	@Override
	public void confirmMessage(ICommentManager commentManager) {

	}

	@Override
	public void cancelMessage(ICommentManager commentManager) {

	}

}
