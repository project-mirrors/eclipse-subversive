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

package org.eclipse.team.svn.core.history;

import java.io.InputStream;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.AbstractGetFileContentOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.GetFileContentOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Resource content storage
 * 
 * @author Alexander Gurov
 */
public class ResourceContentStorage implements IEncodedStorage {
	protected AbstractGetFileContentOperation op;

	protected String charSet;

	protected IRepositoryResource remote;

	public ResourceContentStorage(IRepositoryResource remote) {
		this.remote = remote;
	}

	@Override
	public InputStream getContents() {
		fetchContents(null);
		return op.getContent();
	}

	public synchronized void fetchContents(IProgressMonitor monitor) {
		if (op == null) {
			op = getLoadContentOperation();
			CompositeOperation composite = new CompositeOperation(op.getId(), op.getMessagesClass());
			composite.add(op);
			composite.add(new AbstractActionOperation("Operation_DetectCharset", SVNMessages.class) { //$NON-NLS-1$
				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					ResourceContentStorage.this.detectCharset(op.getContent());
				}
			}, new IActionOperation[] { op });
			if (monitor == null) {
				monitor = new NullProgressMonitor();
			}
			ProgressMonitorUtility.doTaskExternalDefault(composite, monitor);
		}
	}

	@Override
	public IPath getFullPath() {
		return SVNUtility.createPathForSVNUrl(remote.getUrl());
	}

	public IPath getTemporaryPath() {
		fetchContents(null);
		return new Path(op.getTemporaryPath());
	}

	@Override
	public String getName() {
		return remote.getName();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	@Override
	public String getCharset() {
		return charSet;
	}

	public IRepositoryResource getRepositoryResource() {
		return remote;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ResourceContentStorage)) {
			return false;
		}
		IRepositoryResource other = ((ResourceContentStorage) obj).getRepositoryResource();
		return remote.equals(other);
	}

	protected AbstractGetFileContentOperation getLoadContentOperation() {
		return new GetFileContentOperation(remote);
	}

	protected void detectCharset(InputStream stream) throws Exception {
		try {
			IContentDescription description = Platform.getContentTypeManager()
					.getDescriptionFor(stream, getName(), IContentDescription.ALL);
			charSet = description == null ? null : description.getCharset();
		} finally {
			try {
				stream.close();
			} catch (Exception ex) {
			}
		}
	}

}
