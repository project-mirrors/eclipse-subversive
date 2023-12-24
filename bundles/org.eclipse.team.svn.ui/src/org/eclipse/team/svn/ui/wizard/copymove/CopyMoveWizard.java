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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.wizard.copymove;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.wizard.AbstractSVNWizard;

/**
 * Wizard for 'Copy To...' and 'Move To...' refactoring actions on repository.
 * 
 * @author Alexei Goncharov
 */
public class CopyMoveWizard extends AbstractSVNWizard {
	protected SelectDestinationPage destinationPage;

	protected CommentWizardPage commentPage;

	protected IRepositoryResource[] selectedResources;

	protected boolean isMove;

	protected IRepositoryResource destination;

	protected String comment;

	protected String newName;

	public CopyMoveWizard(IRepositoryResource[] selectedResources, boolean isMove) {
		this.selectedResources = selectedResources;
		setWindowTitle((this.isMove = isMove)
				? SVNUIMessages.MoveToAction_Select_Title
				: SVNUIMessages.CopyToAction_Select_Title);
	}

	@Override
	public void addPages() {
		addPage(destinationPage = new SelectDestinationPage(selectedResources));
		addPage(commentPage = new CommentWizardPage(isMove));
	}

	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage instanceof SelectDestinationPage) {
			return false;
		}
		return super.canFinish();
	}

	@Override
	public boolean performFinish() {
		destination = destinationPage.getDestination();
		newName = destinationPage.getNewResourceName();
		comment = commentPage.getComment();
		return true;
	}

	public String getComment() {
		return comment;
	}

	public String getNewName() {
		return newName;
	}

	public IRepositoryResource getDestination() {
		return destination;
	}

}
