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

package org.eclipse.team.svn.core.operation.file.management;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.file.AbstractFileOperation;
import org.eclipse.team.svn.core.operation.file.IFileProvider;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Disconnect files from source control by SVN meta-information deletion. Only folders could be disconnected.
 * 
 * @author Alexander Gurov
 */
public class DisconnectOperation extends AbstractFileOperation {
	public DisconnectOperation(File[] files) {
		super("Operation_DisconnectFile", SVNMessages.class, files); //$NON-NLS-1$
	}

	public DisconnectOperation(IFileProvider provider) {
		super("Operation_DisconnectFile", SVNMessages.class, provider); //$NON-NLS-1$
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		disconnect(operableData(), monitor);
	}

	protected void disconnect(File[] files, IProgressMonitor monitor) {
		files = FileUtility.shrinkChildNodes(files, true);
		for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
			File meta = new File(files[i].getAbsolutePath() + "/" + SVNUtility.getSVNFolderName()); //$NON-NLS-1$
			FileUtility.deleteRecursive(meta, monitor);
			if (files[i].isDirectory()) {
				disconnect(files[i].listFiles(), monitor);
			}
		}
	}

}
