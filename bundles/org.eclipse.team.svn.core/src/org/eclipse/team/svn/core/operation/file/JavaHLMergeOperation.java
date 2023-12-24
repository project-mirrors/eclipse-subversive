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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * JavaHL-mode merge implementation
 * 
 * @author Alexander Gurov
 */
public class JavaHLMergeOperation extends AbstractFileOperation {
	protected IRepositoryResource from1;

	protected IRepositoryResource from2;

	protected long options;

	protected ISVNNotificationCallback notify;

	public JavaHLMergeOperation(File localTo, IRepositoryResource from1, IRepositoryResource from2, boolean dryRun,
			ISVNNotificationCallback notify) {
		this(localTo, from1, from2, (dryRun ? ISVNConnector.Options.SIMULATE : ISVNConnector.Options.NONE)
				| ISVNConnector.Options.ALLOW_MIXED_REVISIONS, notify);
	}

	public JavaHLMergeOperation(File localTo, IRepositoryResource from1, IRepositoryResource from2, long options,
			ISVNNotificationCallback notify) {
		super("Operation_JavaHLMergeFile", SVNMessages.class, new File[] { localTo }); //$NON-NLS-1$
		this.from1 = from1;
		this.from2 = from2;
		this.options = options & ISVNConnector.CommandMasks.MERGE;
		this.notify = notify;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File file = operableData()[0];

		IRepositoryLocation location = from1.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();

		if (notify != null) {
			SVNUtility.addSVNNotifyListener(proxy, notify);
		}
		try {
			proxy.mergeTwo(
					SVNUtility.getEntryRevisionReference(from1), SVNUtility.getEntryRevisionReference(from2),
					file.getAbsolutePath(), SVNDepth.INFINITY, options, new SVNProgressMonitor(this, monitor, null));
		} finally {
			if (notify != null) {
				SVNUtility.removeSVNNotifyListener(proxy, notify);
			}
			location.releaseSVNProxy(proxy);
		}
	}

}
