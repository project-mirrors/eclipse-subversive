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

package org.eclipse.team.svn.core.operation.file;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Get all children for the specified folder (including non-committed deletions)
 * 
 * @author Alexander Gurov
 */
public class GetAllFilesOperation extends AbstractFileOperation implements IFileProvider {
	protected File[] children;

	public GetAllFilesOperation(File file) {
		super("Operation_GetAllFiles", SVNMessages.class, new File[] { file }); //$NON-NLS-1$
	}

	@Override
	public File[] getFiles() {
		return children;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		children = new File[0];

		File file = operableData()[0];
		if (!file.exists() || file.isFile()) {
			return;
		}

		final HashSet<File> allFiles = new HashSet<>();

		File[] children = file.listFiles((FileFilter) arg0 -> !arg0.getName().equals(SVNUtility.getSVNFolderName()));
		if (children != null) {
			allFiles.addAll(Arrays.asList(children));
		}

		IRepositoryResource remote = SVNFileStorage.instance().asRepositoryResource(file, true);
		if (remote != null) {
			IRepositoryLocation location = remote.getRepositoryLocation();
			ISVNConnector proxy = location.acquireSVNProxy();
			try {
				proxy.status(
						file.getAbsolutePath(), SVNDepth.IMMEDIATES, ISVNConnector.Options.INCLUDE_UNCHANGED
								| ISVNConnector.Options.INCLUDE_IGNORED | ISVNConnector.Options.LOCAL_SIDE,
						null, status -> allFiles.add(new File(status.path)), new SVNProgressMonitor(this, monitor, null));
			} finally {
				location.releaseSVNProxy(proxy);
			}
		}

		this.children = allFiles.toArray(new File[allFiles.size()]);
	}

}
