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
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.IRevisionProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Branch and Tag operation implementation
 * 
 * @author Alexander Gurov
 */
public class BranchTagOperation extends AbstractRepositoryOperation implements IRevisionProvider {
	protected String destinationUrl;

	protected String message;

	protected ArrayList<RevisionPair> revisionsPairs;

	public BranchTagOperation(String operationName, Class<? extends NLS> messagesClass, IRepositoryResource[] resources,
			IRepositoryResource destination, String message) {
		super("Operation_" + operationName, messagesClass, resources); //$NON-NLS-1$
		destinationUrl = destination.getUrl();
		this.message = message;
	}

	@Override
	public RevisionPair[] getRevisions() {
		return revisionsPairs == null ? null : revisionsPairs.toArray(new RevisionPair[revisionsPairs.size()]);
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		revisionsPairs = new ArrayList<>();
		IRepositoryResource[] resources = operableData();

		ProgressMonitorUtility.setTaskInfo(monitor, this, FileUtility.getNamesListAsString(resources));
		final IRepositoryLocation location = resources[0].getRepositoryLocation();
		final ISVNConnector proxy = location.acquireSVNProxy();
		try {
			for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
				final IRepositoryResource current = resources[i];
				final String url2 = SVNUtility.encodeURL(BranchTagOperation.this.destinationUrl);

				ISVNNotificationCallback notify = info -> {
					revisionsPairs.add(new RevisionPair(info.revision, new String[] { url2 }, location));
					String message = BaseMessages.format(SVNMessages.Console_CommittedRevision,
							new String[] { String.valueOf(info.revision) });
					BranchTagOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
				};
				SVNUtility.addSVNNotifyListener(proxy, notify);

				this.protectStep(monitor1 -> {
					BranchTagOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn copy \"" //$NON-NLS-1$
							+ current.getUrl() + "\" \"" + destinationUrl + "\" -r " //$NON-NLS-1$//$NON-NLS-2$
							+ current.getSelectedRevision() + " -m \"" + message + "\"" //$NON-NLS-1$//$NON-NLS-2$
							+ ISVNConnector.Options.asCommandLine(ISVNConnector.Options.INTERPRET_AS_CHILD)
							+ FileUtility.getUsernameParam(current.getRepositoryLocation().getUsername()) + "\n"); //$NON-NLS-1$
					SVNEntryRevisionReference[] src = {
							new SVNEntryRevisionReference(SVNUtility.encodeURL(current.getUrl()),
									current.getPegRevision(), current.getSelectedRevision()) };
					proxy.copyRemote(src, url2, message, ISVNConnector.Options.INTERPRET_AS_CHILD, null,
							ISVNConnector.NO_EXTERNALS_TO_PIN,
							new SVNProgressMonitor(BranchTagOperation.this, monitor1, null));
				}, monitor, resources.length);

				SVNUtility.removeSVNNotifyListener(proxy, notify);
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

}
