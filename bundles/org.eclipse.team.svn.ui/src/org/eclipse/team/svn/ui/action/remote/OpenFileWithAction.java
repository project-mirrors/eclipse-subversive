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

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.operation.OpenRemoteFileOperation;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * The action will open the selected file with the system external editor if available
 * 
 * @author Alexander Gurov
 */
public class OpenFileWithAction extends AbstractRepositoryTeamAction {
	protected String editorId;

	protected boolean allowsMultiple;

	public OpenFileWithAction() {
		this(EditorsUI.DEFAULT_TEXT_EDITOR_ID, true);
	}

	public OpenFileWithAction(String editorId, boolean allowsMultiple) {
		this.editorId = editorId;
		this.allowsMultiple = allowsMultiple;
	}

	@Override
	public void runImpl(IAction action) {
		RepositoryFile[] resources = this.getAdaptedSelection(RepositoryFile.class);
		IRepositoryFile[] files = new IRepositoryFile[resources.length];
		for (int i = 0; i < resources.length; i++) {
			files[i] = (IRepositoryFile) resources[i].getRepositoryResource();
		}
		runScheduled(new OpenRemoteFileOperation(files, OpenRemoteFileOperation.OPEN_SPECIFIED, editorId));
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		if (action.getImageDescriptor() == null) {
			IEditorDescriptor descriptor = SVNTeamUIPlugin.instance()
					.getWorkbench()
					.getEditorRegistry()
					.findEditor(editorId);
			action.setImageDescriptor(descriptor == null ? null : descriptor.getImageDescriptor());
		}
	}

	@Override
	public boolean isEnabled() {
		Object[] items = this.getAdaptedSelection(RepositoryFile.class);
		return allowsMultiple ? items.length > 0 : items.length == 1;
	}

}
