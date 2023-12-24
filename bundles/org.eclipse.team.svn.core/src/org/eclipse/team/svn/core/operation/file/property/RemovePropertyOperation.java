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

package org.eclipse.team.svn.core.operation.file.property;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.file.AbstractFileOperation;
import org.eclipse.team.svn.core.operation.file.IFileProvider;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Remove resource property operation
 * 
 * @author Alexander Gurov
 */
public class RemovePropertyOperation extends AbstractFileOperation {
	protected boolean isRecursive;

	protected String[] names;

	public RemovePropertyOperation(File[] files, String[] names, boolean isRecursive) {
		super("Operation_RemovePropertiesFile", SVNMessages.class, files); //$NON-NLS-1$
		this.names = names;
		this.isRecursive = isRecursive;
	}

	public RemovePropertyOperation(IFileProvider provider, String[] names, boolean isRecursive) {
		super("Operation_RemovePropertiesFile", SVNMessages.class, provider); //$NON-NLS-1$
		this.names = names;
		this.isRecursive = isRecursive;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] files = operableData();
		if (isRecursive) {
			files = FileUtility.shrinkChildNodes(files, false);
		}

		for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
			final File current = files[i];
			IRepositoryResource remote = SVNFileStorage.instance().asRepositoryResource(files[i], false);
			IRepositoryLocation location = remote.getRepositoryLocation();
			final ISVNConnector proxy = location.acquireSVNProxy();
			this.protectStep(monitor2 -> {
				for (int i1 = 0; i1 < names.length && !monitor2.isCanceled(); i1++) {
					final String name = names[i1];
					RemovePropertyOperation.this.protectStep(monitor1 -> proxy.setPropertyLocal(new String[] { current.getAbsolutePath() },
							new SVNProperty(name), isRecursive ? SVNDepth.INFINITY : SVNDepth.EMPTY,
							ISVNConnector.Options.NONE, null,
							new SVNProgressMonitor(RemovePropertyOperation.this, monitor1, null)), monitor2, names.length);
				}
			}, monitor, files.length);
			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	protected ISchedulingRule getSchedulingRule(File file) {
		return file.isDirectory() ? new LockingRule(file) : super.getSchedulingRule(file);
	}

}
