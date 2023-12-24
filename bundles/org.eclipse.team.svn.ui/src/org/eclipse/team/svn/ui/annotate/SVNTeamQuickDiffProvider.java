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

package org.eclipse.team.svn.ui.annotate;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.GetLocalFileContentOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;

/**
 * Quick diff provider. Uses BASE working copy revision to compare.
 * 
 * @author Alexander Gurov
 */
public class SVNTeamQuickDiffProvider
		implements IQuickDiffReferenceProvider, IResourceStatesListener, IElementStateListener {
	protected String id;

	protected ITextEditor editor;

	protected IDocumentProvider documentProvider;

	protected boolean referenceInitialized;

	protected IDocument reference;

	protected ILocalResource savedState;

	protected Job updateJob;

	@Override
	public void setActiveEditor(ITextEditor editor) {
		IEditorInput editorInput = editor.getEditorInput();
		if (ResourceUtil.getFile(editorInput) != null) {
			this.editor = editor;
			documentProvider = editor.getDocumentProvider();
			SVNRemoteStorage.instance().addResourceStatesListener(ResourceStatesChangedEvent.class, this);
			if (documentProvider != null) {
				documentProvider.addElementStateListener(this);
			}
			referenceInitialized = true;
		}
	}

	@Override
	public void dispose() {
		if (updateJob != null && updateJob.getState() != Job.NONE) {
			updateJob.cancel();
		}
		referenceInitialized = false;

		if (documentProvider != null) {
			documentProvider.removeElementStateListener(this);
		}
		SVNRemoteStorage.instance().removeResourceStatesListener(ResourceStatesChangedEvent.class, this);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public IDocument getReference(IProgressMonitor monitor) throws CoreException {
		if (referenceInitialized) {
			if (reference == null) {
				readDocument(monitor);
			}
			return reference;
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return referenceInitialized && isShared();
	}

	@Override
	public void resourcesStateChanged(ResourceStatesChangedEvent event) {
		IFile file = getFile();
		if (file != null && isEnabled() && event.contains(getFile())) {
			backgroundFetch();
		}
	}

	@Override
	public void elementContentReplaced(Object element) {
		IFile file = getFile();
		if (file != null && isEnabled() && editor.getEditorInput() == element) {
			backgroundFetch();
		}
	}

	@Override
	public void elementContentAboutToBeReplaced(Object element) {
	}

	@Override
	public void elementDeleted(Object element) {
	}

	@Override
	public void elementDirtyStateChanged(Object element, boolean isDirty) {
	}

	@Override
	public void elementMoved(Object originalElement, Object movedElement) {
	}

	protected boolean isShared() {
		ILocalResource local = getLocalResource();
		return IStateFilter.SF_VERSIONED.accept(local);
	}

	protected ILocalResource getLocalResource() {
		return SVNRemoteStorage.instance().asLocalResource(getFile());
	}

	protected IFile getFile() {
		return editor == null ? null : ResourceUtil.getFile(editor.getEditorInput());
	}

	protected void backgroundFetch() {
		if (updateJob != null && updateJob.getState() != Job.NONE) {
			updateJob.cancel();
		}
		updateJob = ProgressMonitorUtility
				.doTaskScheduledDefault(new AbstractActionOperation("Operation_QuickDiff", SVNUIMessages.class) { //$NON-NLS-1$
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						SVNTeamQuickDiffProvider.this.readDocument(monitor);
					}
				});
	}

	protected void readDocument(IProgressMonitor monitor) {
		if (reference == null) {
			reference = new Document();
		}
		if (documentProvider instanceof IStorageDocumentProvider) {
			IStorageDocumentProvider provider = (IStorageDocumentProvider) documentProvider;
			String encodingTmp = provider.getEncoding(editor.getEditorInput());
			String encoding = encodingTmp == null ? provider.getDefaultEncoding() : encodingTmp;

			ILocalResource tmp = getLocalResource();
			if (savedState == null
					|| !IStateFilter.SF_INTERNAL_INVALID.accept(tmp) && savedState.getRevision() != tmp.getRevision()) {
				savedState = tmp;
				final GetLocalFileContentOperation contentOp = new GetLocalFileContentOperation(tmp.getResource(),
						Kind.BASE);
				CompositeOperation op = new CompositeOperation("Operation_PrepareQuickDiff", SVNUIMessages.class); //$NON-NLS-1$
				op.add(contentOp);
				op.add(new InitializeDocumentOperation(encoding) {
					@Override
					public InputStream getInputStream() {
						return contentOp.getContent();
					}
				}, new IActionOperation[] { contentOp });
				ProgressMonitorUtility.doTaskExternalDefault(op, monitor);
			}
		} else if (!monitor.isCanceled()) {
			reference.set(""); //$NON-NLS-1$
		}
	}

	protected abstract class InitializeDocumentOperation extends AbstractActionOperation {
		public String encoding;

		public InitializeDocumentOperation(String encoding) {
			super("Operation_InitializeDocument", SVNUIMessages.class); //$NON-NLS-1$
			this.encoding = encoding;
		}

		public abstract InputStream getInputStream() throws Exception;

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			InputStream content = getInputStream();
			Reader in = null;
			CharArrayWriter store = null;
			try {
				in = new BufferedReader(new InputStreamReader(content, encoding));
				store = new CharArrayWriter();
				char[] buf = new char[2048];
				int len;
				while ((len = in.read(buf)) > 0 && !monitor.isCanceled()) {
					store.write(buf, 0, len);
				}
				if (!monitor.isCanceled()) {
					reference.set(store.toString());
				}
			} finally {
				if (store != null) {
					try {
						store.close();
					} catch (Exception ex) {
					}
				}
				if (in != null) {
					try {
						in.close();
					} catch (Exception ex) {
					}
				}
				try {
					content.close();
				} catch (Exception ex) {
				}
			}
		}
	}

}
