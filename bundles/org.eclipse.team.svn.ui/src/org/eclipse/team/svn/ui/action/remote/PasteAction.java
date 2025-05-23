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

package org.eclipse.team.svn.ui.action.remote;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.AbstractCopyMoveResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.CopyResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.MoveResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.SetRevisionAuthorNameOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.RemoteResourceTransfer;
import org.eclipse.team.svn.ui.RemoteResourceTransferrable;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.panel.common.CommentPanel;

/**
 * Paste remote resource from clipboard action implementation
 * 
 * @author Alexander Gurov
 */
public class PasteAction extends AbstractRepositoryTeamAction {

	public PasteAction() {
	}

	@Override
	public void runImpl(IAction action) {
		final RemoteResourceTransferrable transferrable = getTransferrable();
		CommentPanel commentPanel = new CommentPanel(SVNUIMessages.PasteAction_Comment_Title);
		DefaultDialog dialog = new DefaultDialog(getShell(), commentPanel);
		if (dialog.open() == 0) {
			clearClipboard(transferrable);

			final IRepositoryResource[] resources = getSelectedRepositoryResources();
			resources[0] = resources[0] instanceof IRepositoryContainer ? resources[0] : resources[0].getParent();

			final AbstractCopyMoveResourcesOperation pasteOp = transferrable.operation == RemoteResourceTransferrable.OP_COPY
					? (AbstractCopyMoveResourcesOperation) new CopyResourcesOperation(resources[0],
							transferrable.resources, commentPanel.getMessage(), null)
					: new MoveResourcesOperation(resources[0], transferrable.resources, commentPanel.getMessage(),
							null);

			CompositeOperation op = new CompositeOperation(pasteOp.getId(), pasteOp.getMessagesClass());

			op.add(pasteOp);
			op.add(new RefreshRemoteResourcesOperation(
					() -> {
						if (transferrable.operation == RemoteResourceTransferrable.OP_COPY) {
							return resources;
						}
						HashSet<IRepositoryResource> fullSet = new HashSet<>(
								Arrays.asList(resources));
						fullSet.addAll(Arrays.asList(SVNUtility.getCommonParents(transferrable.resources)));
						return fullSet.toArray(new IRepositoryResource[fullSet.size()]);
					}));
			op.add(new SetRevisionAuthorNameOperation(pasteOp, Options.FORCE), new IActionOperation[] { pasteOp });

			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		IRepositoryResource[] selected = getSelectedRepositoryResources();
		if (selected.length != 1 || selected[0].getSelectedRevision().getKind() != Kind.HEAD) {
			return false;
		}
		RemoteResourceTransferrable transferrable = getTransferrable();
		if (transferrable == null) {
			return false;
		}
		IRepositoryResource target = selected[0] instanceof IRepositoryContainer
				? selected[0]
				: selected[0].getParent();
		for (IRepositoryResource element : transferrable.resources) {
			if (isSource(target, element)) {
				return false;
			}
		}
		return selected[0].getRepositoryLocation() == transferrable.resources[0].getRepositoryLocation();
	}

	protected boolean isSource(IRepositoryResource selectedResource, IRepositoryResource resource) {
		IPath selectedUrl = SVNUtility.createPathForSVNUrl(selectedResource.getUrl());
		return SVNUtility.createPathForSVNUrl(resource.getUrl()).isPrefixOf(selectedUrl) || resource.getParent() != null
				&& SVNUtility.createPathForSVNUrl(resource.getParent().getUrl()).equals(selectedUrl);
	}

	protected RemoteResourceTransferrable getTransferrable() {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try {
			RemoteResourceTransferrable transferrable = (RemoteResourceTransferrable) clipboard
					.getContents(RemoteResourceTransfer.getInstance());
			if (transferrable == null || transferrable.operation == RemoteResourceTransferrable.OP_NONE
					|| transferrable.resources == null || transferrable.resources.length == 0) {
				return null;
			}
			return transferrable;
		} finally {
			clipboard.dispose();
		}
	}

	protected void clearClipboard(RemoteResourceTransferrable transferrable) {
		if (transferrable.operation == RemoteResourceTransferrable.OP_CUT) {
			Clipboard clipboard = new Clipboard(getShell().getDisplay());
			try {
				// Eclipse 3.1.0 API incompatibility fix instead of clipboard.setContents(new Object[0], new Transfer[0]);
				//clipboard.clearContents(); - does not work for unknown reasons (when MS Office clipboard features are enabled)
				//COM.OleSetClipboard(0); - incompatible with UNIX'like
				clipboard.setContents(
						new Object[] { new RemoteResourceTransferrable(null, RemoteResourceTransferrable.OP_NONE) },
						new Transfer[] { RemoteResourceTransfer.getInstance() });
			} finally {
				clipboard.dispose();
			}
		}
	}

}
