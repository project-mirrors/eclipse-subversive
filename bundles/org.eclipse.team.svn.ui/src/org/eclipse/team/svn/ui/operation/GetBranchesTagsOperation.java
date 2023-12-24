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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.svn.core.connector.SVNConnectorAuthenticationException;
import org.eclipse.team.svn.core.connector.SVNConnectorCancelException;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Get children for passed resource. If there're no children or problem happens during their retrieving then show dialog which says that
 * there're no branches/tags.
 * 
 * @author Igor Burilo
 */
public class GetBranchesTagsOperation extends AbstractActionOperation {

	protected IRepositoryContainer parent;

	protected boolean isBranch;

	protected IRepositoryResource[] children;

	public GetBranchesTagsOperation(IRepositoryContainer parent, boolean isBranch) {
		super("Operation_GetBranchesTags", SVNUIMessages.class); //$NON-NLS-1$
		this.parent = parent;
		this.isBranch = isBranch;
	}

	public IRepositoryResource[] getChildren() {
		return children == null ? new IRepositoryResource[0] : children;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		boolean hasError = false;
		try {
			children = parent.getChildren();
		} catch (SVNConnectorException se) {
			if (!(se instanceof SVNConnectorAuthenticationException || se instanceof SVNConnectorCancelException)) {
				hasError = true;
			}
		}
		if (hasError || getChildren().length == 0) {
			UIMonitorUtility.getDisplay().syncExec(() -> {
				MessageDialog dialog = new MessageDialog(
						UIMonitorUtility.getShell(), SVNUIMessages.ComparePropsNoDiff_Title, null,
						isBranch
								? SVNUIMessages.BranchTagSelectionComposite_NoBranches
								: SVNUIMessages.BranchTagSelectionComposite_NoTags,
						MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0);
				dialog.open();
			});
		}
	}
}
