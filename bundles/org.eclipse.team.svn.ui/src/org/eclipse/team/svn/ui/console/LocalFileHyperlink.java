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
 *    Alexey Mikoyan - Initial implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/
package org.eclipse.team.svn.ui.console;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Hyperlink to local file
 *
 * @author Alexey Mikoyan
 *
 */
public class LocalFileHyperlink implements IHyperlink {

	protected String pathString;

	public LocalFileHyperlink(String pathString) {
		this.pathString = pathString;
	}

	@Override
	public void linkActivated() {
		UIMonitorUtility.doTaskBusyDefault(new OpenLocalFileOperation(pathString));
	}

	@Override
	public void linkEntered() {
	}

	@Override
	public void linkExited() {
	}

	protected class OpenLocalFileOperation extends AbstractActionOperation {

		protected String filePath;

		public OpenLocalFileOperation(String filePath) {
			super("Operation_OpenLocalFile", SVNUIMessages.class); //$NON-NLS-1$
			this.filePath = filePath;
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			IWorkbenchWindow window = SVNTeamUIPlugin.instance().getWorkbench().getActiveWorkbenchWindow();
			if (window == null || window.getActivePage() == null) {
				return;
			}
			IWorkbenchPage page = window.getActivePage();
			if (ResourcesPlugin.getWorkspace().getRoot() == null) {
				return;
			}

			IPath path = Path.fromOSString(filePath);
			if (!path.isAbsolute()) {
				path = path.makeAbsolute()
						.setDevice(ResourcesPlugin.getWorkspace().getRoot().getLocation().getDevice());
			}

			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
			if (file == null || !file.exists()) {
				if (path != null && !path.isAbsolute() && path.segmentCount() > 1) {
					path = path.removeFirstSegments(1);
					file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				}
			}
			if (file == null || !file.exists()) {
				return;
			}
			IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
			if (registry == null) {
				return;
			}
			IEditorDescriptor descriptor = null;
			IContentDescription contentDescription = file.getContentDescription();
			descriptor = registry.getDefaultEditor(path.lastSegment(),
					contentDescription != null ? contentDescription.getContentType() : null);

			if (descriptor == null) {
				descriptor = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
			}

			if (descriptor == null) {
				return;
			}

			IEditorInput input = new FileEditorInput(file);
			page.openEditor(input, descriptor.getId());
		}

	}

}
