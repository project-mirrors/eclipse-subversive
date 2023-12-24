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

package org.eclipse.team.svn.core.operation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Abstract implementation of get file content operation
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractGetFileContentOperation extends AbstractActionOperation {
	protected File tmpFile;

	public AbstractGetFileContentOperation(String getOperationType) {
		super("Operation_GetFileContent_" + getOperationType, SVNMessages.class); //$NON-NLS-1$
	}

	public String getTemporaryPath() {
		return tmpFile == null ? null : tmpFile.getAbsolutePath();
	}

	public InputStream getContent() {
		final InputStream[] retVal = { new ByteArrayInputStream(new byte[0]) };
		if (tmpFile != null && tmpFile.exists()) {
			ProgressMonitorUtility.doTaskExternal(
					new AbstractActionOperation("Operation_GetFileContent_CreateStream", SVNMessages.class) { //$NON-NLS-1$
						@Override
						protected void runImpl(IProgressMonitor monitor) throws Exception {
							retVal[0] = new FileInputStream(tmpFile);
						}
					}, new NullProgressMonitor());
		}
		return retVal[0];
	}

	public void setContent(final byte[] data) {
		ProgressMonitorUtility
				.doTaskExternal(new AbstractActionOperation("Operation_GetFileContent_SetContent", SVNMessages.class) { //$NON-NLS-1$
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						if (tmpFile == null) {
							tmpFile = AbstractGetFileContentOperation.this.createTempFile();
						}
						File parent = tmpFile.getParentFile();
						if (parent != null && !parent.exists()) {
							parent.mkdirs();
						}
						FileOutputStream stream = new FileOutputStream(tmpFile);
						try {
							stream.write(data);
						} finally {
							try {
								stream.close();
							} catch (Exception ex) {
							}
						}
					}
				}, new NullProgressMonitor());
	}

	protected File createTempFile() throws IOException {
		String extension = getExtension();
		return SVNTeamPlugin.instance()
				.getTemporaryFile(null,
						"getfilecontent" + (extension != null && extension.length() > 0 ? "." + extension : ".tmp")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	protected String getExtension() {
		return ""; //$NON-NLS-1$
	}

}
