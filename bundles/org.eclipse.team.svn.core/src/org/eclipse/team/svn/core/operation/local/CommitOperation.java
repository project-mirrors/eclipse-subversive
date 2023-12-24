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

package org.eclipse.team.svn.core.operation.local;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNCommitStatus;
import org.eclipse.team.svn.core.connector.SVNConnectorUnresolvedConflictException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNErrorCodes;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.IPostCommitErrorsProvider;
import org.eclipse.team.svn.core.operation.IRevisionProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Commit operation implementation
 * 
 * @author Alexander Gurov
 */
public class CommitOperation extends AbstractConflictDetectionOperation
		implements IRevisionProvider, IPostCommitErrorsProvider {
	protected SVNDepth depth;

	protected long options;

	protected String message;

	protected ArrayList<RevisionPair> revisionsPairs;

	protected ArrayList<SVNCommitStatus> postCommitErrors;

	protected String[] paths;

	public CommitOperation(IResource[] resources, String message, boolean recursive, boolean keepLocks) {
		this(resources, message, SVNDepth.infinityOrEmpty(recursive),
				keepLocks ? ISVNConnector.Options.KEEP_LOCKS : ISVNConnector.Options.NONE);
	}

	public CommitOperation(IResourceProvider provider, String message, boolean recursive, boolean keepLocks) {
		this(provider, message, SVNDepth.infinityOrEmpty(recursive),
				keepLocks ? ISVNConnector.Options.KEEP_LOCKS : ISVNConnector.Options.NONE);
	}

	public CommitOperation(IResource[] resources, String message, SVNDepth depth, long options) {
		super("Operation_Commit", SVNMessages.class, resources); //$NON-NLS-1$
		this.message = message;
		this.depth = depth;
		this.options = options & ISVNConnector.CommandMasks.COMMIT;
	}

	public CommitOperation(IResourceProvider provider, String message, SVNDepth depth, long options) {
		super("Operation_Commit", SVNMessages.class, provider); //$NON-NLS-1$
		this.message = message;
		this.depth = depth;
		this.options = options & ISVNConnector.CommandMasks.COMMIT;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		revisionsPairs = new ArrayList<>();
		postCommitErrors = new ArrayList<>();
		IResource[] resources = operableData();

		defineInitialResourceSet(resources);

		if (depth == SVNDepth.INFINITY) {
			resources = FileUtility.shrinkChildNodesWithSwitched(resources);
		} else {
			FileUtility.reorder(resources, true);
		}

		if ((CoreExtensionsManager.instance().getSVNConnectorFactory().getSupportedFeatures()
				& ISVNConnectorFactory.OptionalFeatures.ATOMIC_X_COMMIT) != 0) {
			Map proxy2Resources = SVNUtility.splitRepositoryLocations(resources);
			for (Iterator it = proxy2Resources.entrySet().iterator(); it.hasNext() && !monitor.isCanceled();) {
				Map.Entry entry = (Map.Entry) it.next();
				IRepositoryLocation location = (IRepositoryLocation) entry.getKey();
				performCommit(location, (List) entry.getValue(), monitor, proxy2Resources.size());
			}
		} else {
			Map project2Resources = SVNUtility.splitWorkingCopies(resources);
			IRemoteStorage storage = SVNRemoteStorage.instance();
			for (Iterator it = project2Resources.entrySet().iterator(); it.hasNext() && !monitor.isCanceled();) {
				Map.Entry entry = (Map.Entry) it.next();

				IRepositoryLocation location = storage.getRepositoryLocation((IResource) entry.getKey());
				performCommit(location, (List) entry.getValue(), monitor, project2Resources.size());
			}
		}
	}

	protected void performCommit(final IRepositoryLocation location, List resources, final IProgressMonitor monitor,
			int total) {
		paths = FileUtility.asPathArray((IResource[]) resources.toArray(new IResource[0]));

		complexWriteToConsole(() -> {
			CommitOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD,
					"svn commit" + ISVNConnector.Options.asCommandLine(options)); //$NON-NLS-1$
			for (int i = 0; i < paths.length && !monitor.isCanceled(); i++) {
				CommitOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, " \"" + paths[i] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			CommitOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, (depth == SVNDepth.INFINITY ? "" : " -N") //$NON-NLS-1$//$NON-NLS-2$
					+ ((options & ISVNConnector.Options.KEEP_LOCKS) != 0
							? " --no-unlock" //$NON-NLS-1$
							: "") //$NON-NLS-1$
					+ " -m \"" + message + "\"" //$NON-NLS-1$//$NON-NLS-2$
					+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
		});

		final ISVNConnector proxy = location.acquireSVNProxy();
		this.protectStep(monitor1 -> {
			SVNProgressMonitor svnMonitor = new SVNProgressMonitor(CommitOperation.this, monitor1, null);
			proxy.commit(
					paths, message, null, depth, options, null, svnMonitor);
			SVNCommitStatus status = svnMonitor.getCommitStatuses().isEmpty()
					? null
					: svnMonitor.getCommitStatuses().iterator().next();
			if (status != null && status.revision != SVNRevision.INVALID_REVISION_NUMBER) {
				revisionsPairs.add(new RevisionPair(status.revision, paths, location));
				String message = BaseMessages.format(SVNMessages.Console_CommittedRevision,
						new String[] { String.valueOf(status.revision) });
				CommitOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
			}
			if (svnMonitor.getPostCommitErrors() != null) {
				postCommitErrors.addAll(svnMonitor.getPostCommitErrors());
			}
		}, monitor, total);
		location.releaseSVNProxy(proxy);
	}

	@Override
	public RevisionPair[] getRevisions() {
		return revisionsPairs == null ? null : revisionsPairs.toArray(new RevisionPair[revisionsPairs.size()]);
	}

	@Override
	public SVNCommitStatus[] getPostCommitErrors() {
		return postCommitErrors == null || postCommitErrors.size() == 0
				? null
				: postCommitErrors.toArray(new SVNCommitStatus[postCommitErrors.size()]);
	}

	@Override
	public void reportStatus(int severity, String message, Throwable t) {
		if (t instanceof SVNConnectorUnresolvedConflictException) {
			setUnresolvedConflict(true);

			StringBuilder messageBuf = new StringBuilder();
			if (t.getMessage() != null && t.getMessage().length() > 0) {
				messageBuf.append(t.getMessage());
			}
			SVNConnectorUnresolvedConflictException ex = (SVNConnectorUnresolvedConflictException) t;
			if (ex.getErrorId() == SVNErrorCodes.fsConflict) {
				messageBuf.append(messageBuf.toString().endsWith("\n") ? "\n" : "\n\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				messageBuf.append(SVNMessages.CommitOperation_3);
			}
			setConflictMessage(messageBuf.toString());
			for (String path : paths) {
				for (IResource res : getProcessed()) {
					if (FileUtility.getResourcePath(res).equals(new Path(path))) {
						removeProcessed(res);
						addUnprocessed(res);
						break;
					}
				}
			}
		} else {
			super.reportStatus(severity, message, t);
		}
	}

}
