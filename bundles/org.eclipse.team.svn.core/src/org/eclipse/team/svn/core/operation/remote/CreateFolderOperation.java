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

package org.eclipse.team.svn.core.operation.remote;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.IRevisionProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Create remote folder operation
 * 
 * @author Alexander Gurov
 */
public class CreateFolderOperation extends AbstractRepositoryOperation implements IRevisionProvider {
	protected String[] names;

	protected String comment;

	protected RevisionPair[] revisionPair;

	public CreateFolderOperation(IRepositoryResource parent, String name, String comment) {
		this(parent, new String[] { name }, comment);
	}

	public CreateFolderOperation(IRepositoryResource parent, String[] names, String comment) {
		super("Operation_CreateFolder", SVNMessages.class, new IRepositoryResource[] { parent }); //$NON-NLS-1$
		this.names = names;
		this.comment = comment;
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		revisionPair = new RevisionPair[1];
		IRepositoryResource parent = operableData()[0];
		final IRepositoryLocation location = parent.getRepositoryLocation();
		ProgressMonitorUtility.setTaskInfo(monitor, this, parent.getUrl() + "/" + names[0]); //$NON-NLS-1$

		Set<IRepositoryResource> fullSet = new HashSet<>();
		for (String name2 : names) {
			fullSet.addAll(Arrays.asList(SVNUtility.makeResourceSet(parent, name2, false)));
		}
		IRepositoryResource[] toBeCreated = fullSet.toArray(new IRepositoryResource[fullSet.size()]);

		final String[] childUrls = SVNUtility.asURLArray(toBeCreated, true);
		Arrays.sort(childUrls);

		ISVNNotificationCallback notify = info -> {
			String[] path = childUrls;
			revisionPair[0] = new RevisionPair(info.revision, path, location);
			String message = BaseMessages.format(SVNMessages.Console_CommittedRevision,
					new String[] { String.valueOf(info.revision) });
			CreateFolderOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
		};

		complexWriteToConsole(() -> {
			CreateFolderOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn mkdir"); //$NON-NLS-1$
			for (int i = 0; i < childUrls.length && !monitor.isCanceled(); i++) {
				CreateFolderOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						" \"" + SVNUtility.decodeURL(childUrls[i]) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			CreateFolderOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, " -m \"" + comment + "\"" //$NON-NLS-1$//$NON-NLS-2$
					+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
		});

		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			SVNUtility.addSVNNotifyListener(proxy, notify);
			proxy.mkdir(childUrls, comment, ISVNConnector.Options.NONE, null,
					new SVNProgressMonitor(this, monitor, null));
		} finally {
			SVNUtility.removeSVNNotifyListener(proxy, notify);
			parent.getRepositoryLocation().releaseSVNProxy(proxy);
		}

		ProgressMonitorUtility.progress(monitor, 1, 1);
	}

	@Override
	public RevisionPair[] getRevisions() {
		return revisionPair;
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { name });
	}

}
