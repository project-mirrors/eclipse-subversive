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

package org.eclipse.team.svn.ui.operation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.repository.IRepositoryEditorInput;
import org.eclipse.team.svn.ui.repository.RepositoryFileEditorInput;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * Open remote file operation implementation
 * 
 * @author Alexander Gurov
 */
public class OpenRemoteFileOperation extends AbstractActionOperation {
	public static final int OPEN_DEFAULT = 0;

	public static final int OPEN_EXTERNAL = 1;

	public static final int OPEN_INPLACE = 2;

	public static final int OPEN_SPECIFIED = 3;

	protected IRepositoryEditorInput[] inputs;

	protected IEditorPart[] editors;

	protected IRepositoryResourceProvider provider;

	protected int openType;

	protected String openWith;

	protected Class requiredDefaultEditorKind;

	public OpenRemoteFileOperation(IRepositoryResourceProvider provider, int openType) {
		this(provider, openType,
				openType == OpenRemoteFileOperation.OPEN_SPECIFIED ? EditorsUI.DEFAULT_TEXT_EDITOR_ID : null);
	}

	public OpenRemoteFileOperation(IRepositoryResourceProvider provider, int openType, String openWith) {
		this((IRepositoryEditorInput[]) null, openType, openWith);
		this.provider = provider;
	}

	public OpenRemoteFileOperation(IRepositoryFile[] resources, int openType) {
		this(resources, openType,
				openType == OpenRemoteFileOperation.OPEN_SPECIFIED ? EditorsUI.DEFAULT_TEXT_EDITOR_ID : null);
	}

	public OpenRemoteFileOperation(IRepositoryFile[] resources, int openType, String openWith) {
		this(OpenRemoteFileOperation.asEditorInput(resources), openType, openWith);
	}

	public OpenRemoteFileOperation(IRepositoryEditorInput[] inputs, int openType) {
		this(inputs, openType,
				openType == OpenRemoteFileOperation.OPEN_SPECIFIED ? EditorsUI.DEFAULT_TEXT_EDITOR_ID : null);
	}

	public OpenRemoteFileOperation(IRepositoryEditorInput[] inputs, int openType, String openWith) {
		super("Operation_OpenFile", SVNUIMessages.class); //$NON-NLS-1$
		this.inputs = inputs;
		this.openType = openType;
		this.openWith = openWith;
	}

	public Class getRequiredDefaultEditorKind() {
		return requiredDefaultEditorKind;
	}

	public void setRequiredDefaultEditorKind(Class requiredDefaultEditorKind) {
		this.requiredDefaultEditorKind = requiredDefaultEditorKind;
	}

	public IEditorPart[] getEditors() {
		return editors;
	}

	public IRepositoryEditorInput[] getRepositoryEditorInputs() {
		return inputs;
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		if (provider != null) {
			if (!(provider.getRepositoryResources()[0] instanceof IRepositoryFile)) {
				return;
			}
			inputs = OpenRemoteFileOperation.asEditorInput(
					new IRepositoryFile[] { (IRepositoryFile) provider.getRepositoryResources()[0] });
		}
		editors = new IEditorPart[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			final int current = i;

			this.protectStep(monitor1 -> inputs[current].fetchContents(monitor1), monitor, OpenRemoteFileOperation.this.inputs.length * 2);

			UIMonitorUtility.getDisplay().syncExec(() -> OpenRemoteFileOperation.this.protectStep(monitor1 -> OpenRemoteFileOperation.this.openFile(current), monitor, inputs.length * 2));
		}
	}

	protected void openFile(int current) throws Exception {
		IRepositoryEditorInput input = inputs[current];

		IWorkbench workbench = SVNTeamUIPlugin.instance().getWorkbench();
		IEditorRegistry registry = workbench.getEditorRegistry();
		IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();

		if (openType == OpenRemoteFileOperation.OPEN_EXTERNAL) {
			IEditorDescriptor descriptor = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
			if (descriptor != null) {
				editors[current] = openEditor(page, descriptor.getId(), input);
			}
		} else if (openType == OpenRemoteFileOperation.OPEN_INPLACE) {
			IEditorDescriptor descriptor = registry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
			if (descriptor != null) {
				editors[current] = openEditor(page, descriptor.getId(), input);
			}
		} else if (openType == OpenRemoteFileOperation.OPEN_SPECIFIED) {
			editors[current] = openEditor(page, openWith, input);
		} else {
			String fileName = input.getRepositoryResource().getName();
			IContentType type = Platform.getContentTypeManager()
					.findContentTypeFor(input.getStorage().getContents(), fileName);
			IEditorDescriptor descriptor = registry.getDefaultEditor(fileName, type);
			if (descriptor == null) {
				if (registry.isSystemInPlaceEditorAvailable(fileName)) {
					descriptor = registry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
				} else if (registry.isSystemExternalEditorAvailable(fileName)) {
					descriptor = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
				}
			}
			String editorId = descriptor == null ? EditorsUI.DEFAULT_TEXT_EDITOR_ID : descriptor.getId();

			try {
				editors[current] = openEditor(page, editorId, input);
				if (editors[current] != null
						&& editors[current].getClass().getName().toLowerCase().indexOf("error") != -1 || //$NON-NLS-1$
						requiredDefaultEditorKind != null && (editors[current] == null
								|| !requiredDefaultEditorKind.isAssignableFrom(editors[current].getClass()))) {
					editors[current] = openEditor(page, EditorsUI.DEFAULT_TEXT_EDITOR_ID, input);
				}
			} catch (Throwable e) {
				// Cannot open file with correct editor. Trying default editor.
				editors[current] = openEditor(page, EditorsUI.DEFAULT_TEXT_EDITOR_ID, input);
			}
		}
	}

	// the method is created in order to disallow reporting of Eclipse IDE or external tool errors
	private final IEditorPart openEditor(IWorkbenchPage page, String editorId, IRepositoryEditorInput input)
			throws Exception {
		return page.openEditor(input, editorId, true, IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID);
	}

	protected static IRepositoryEditorInput[] asEditorInput(IRepositoryFile[] resources) {
		IRepositoryEditorInput[] inputs = new IRepositoryEditorInput[resources.length];
		for (int i = 0; i < resources.length; i++) {
			inputs[i] = new RepositoryFileEditorInput(resources[i]);
		}
		return inputs;
	}

}
