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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Delete remote resources operation
 * 
 * @author Alexander Gurov
 */
public class DeleteResourcesOperation extends AbstractRepositoryOperation implements IRevisionProvider {
	protected String message;

	protected ArrayList<RevisionPair> revisionsPairs;

	public DeleteResourcesOperation(IRepositoryResource[] resources, String message) {
		super("Operation_DeleteRemote", SVNMessages.class, resources); //$NON-NLS-1$
		this.message = message;
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		revisionsPairs = new ArrayList<>();
		IRepositoryResource[] resources = SVNUtility.shrinkChildNodes(operableData());

		Map<?, ?> repository2Resources = SVNUtility.splitRepositoryLocations(resources);

		for (Iterator<?> it = repository2Resources.entrySet().iterator(); it.hasNext() && !monitor.isCanceled();) {
			Map.Entry entry = (Map.Entry) it.next();
			final IRepositoryLocation location = (IRepositoryLocation) entry.getKey();
			final String[] paths = SVNUtility
					.asURLArray(((List<?>) entry.getValue()).toArray(new IRepositoryResource[0]), true);

			complexWriteToConsole(() -> {
				DeleteResourcesOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
						"svn delete" + ISVNConnector.Options.asCommandLine(ISVNConnector.Options.FORCE)); //$NON-NLS-1$
				for (int i = 0; i < paths.length && !monitor.isCanceled(); i++) {
					DeleteResourcesOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
							" \"" + SVNUtility.decodeURL(paths[i]) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				DeleteResourcesOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, " -m \"" + message + "\"" //$NON-NLS-1$//$NON-NLS-2$
						+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
			});

			final ISVNConnector proxy = location.acquireSVNProxy();
			ISVNNotificationCallback notify = info -> {
				revisionsPairs.add(new RevisionPair(info.revision, paths, location));
				String message = BaseMessages.format(SVNMessages.Console_CommittedRevision,
						new String[] { String.valueOf(info.revision) });
				DeleteResourcesOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
			};
			SVNUtility.addSVNNotifyListener(proxy, notify);
			this.protectStep(monitor1 -> proxy.removeRemote(paths, message, ISVNConnector.Options.FORCE, null,
					new SVNProgressMonitor(DeleteResourcesOperation.this, monitor1, null)), monitor, repository2Resources.size());
			SVNUtility.removeSVNNotifyListener(proxy, notify);

			location.releaseSVNProxy(proxy);
		}
	}

	@Override
	public RevisionPair[] getRevisions() {
		return revisionsPairs == null ? null : revisionsPairs.toArray(new RevisionPair[revisionsPairs.size()]);
	}

}
