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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNNotification;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.IRevisionProvider;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Base implementation for copy and move repository resources operations
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractCopyMoveResourcesOperation extends AbstractRepositoryOperation
		implements IRevisionProvider {
	protected IRepositoryResource destinationResource;

	protected String message;

	protected String resName;

	protected ArrayList<RevisionPair> revisionsPairs;

	public AbstractCopyMoveResourcesOperation(String operationName, Class<? extends NLS> messagesClass,
			IRepositoryResource destinationResource, IRepositoryResource[] selectedResources, String message,
			String resName) {
		super(operationName, messagesClass, selectedResources);
		this.destinationResource = destinationResource;
		this.message = message;
		this.resName = resName;
	}

	@Override
	public RevisionPair[] getRevisions() {
		return revisionsPairs == null ? null : revisionsPairs.toArray(new RevisionPair[revisionsPairs.size()]);
	}

	@Override
	protected void runImpl(final IProgressMonitor monitor) throws Exception {
		revisionsPairs = new ArrayList<>();
		IRepositoryResource[] selectedResources = operableData();
		final SVNEntryRevisionReference[] refs = new SVNEntryRevisionReference[selectedResources.length];
		String[] paths = new String[selectedResources.length];
		for (int i = 0; i < selectedResources.length; i++) {
			refs[i] = SVNUtility.getEntryRevisionReference(selectedResources[i]);
			paths[i] = refs[i].path;
		}
		final IRepositoryLocation location = selectedResources[0].getRepositoryLocation();
		final String dstUrl = destinationResource.getUrl() + (resName != null && resName.length() > 0
				? "/" + resName //$NON-NLS-1$
				: selectedResources.length > 1 ? "" : "/" + selectedResources[0].getName()); //$NON-NLS-1$ //$NON-NLS-2$
		ISVNNotificationCallback notify = new ISVNNotificationCallback() {
			private int i = 0;

			@Override
			public void notify(SVNNotification info) {
				if (i == refs.length) {
					return;
				}
				String[] paths = AbstractCopyMoveResourcesOperation.this.getRevisionPaths(refs[i].path, dstUrl);
				revisionsPairs.add(new RevisionPair(info.revision, paths, location));
				String message = BaseMessages.format(SVNMessages.Console_CommittedRevision,
						new String[] { String.valueOf(info.revision) });
				AbstractCopyMoveResourcesOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
				i++;
			}
		};
		ISVNConnector proxy = location.acquireSVNProxy();
		//NOTE NPE in SVN Kit if parents exists and MAKE_PARENTS is specified
		//NOTE JavaHL is crashed when empty folder is copied independently from MAKE_PARENTS option
		SVNUtility.addSVNNotifyListener(proxy, notify);
		try {
			runCopyMove(proxy, refs, paths, SVNUtility.encodeURL(dstUrl), monitor);
		} finally {
			SVNUtility.removeSVNNotifyListener(proxy, notify);
			location.releaseSVNProxy(proxy);
		}
	}

	protected abstract String[] getRevisionPaths(String srcUrl, String dstUrl);

	protected abstract void runCopyMove(ISVNConnector proxy, SVNEntryRevisionReference[] source, String[] sourcePaths,
			String destinationUrl, IProgressMonitor monitor) throws Exception;
}
