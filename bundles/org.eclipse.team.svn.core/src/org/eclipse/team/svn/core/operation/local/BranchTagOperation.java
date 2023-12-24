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
 *    Alexey Mikoyan - Initial implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNNotificationCallback;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Branch and Tag from working copy operation implementation
 *
 * @author Alexey Mikoyan
 *
 */
public class BranchTagOperation extends AbstractWorkingCopyOperation {
	protected IRepositoryResource destination;

	protected String message;

	public BranchTagOperation(String operationName, IResource[] resources, IRepositoryResource destination,
			String message) {
		super("Operation_" + operationName, SVNMessages.class, resources); //$NON-NLS-1$
		this.destination = destination;
		this.message = message;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		ProgressMonitorUtility.setTaskInfo(monitor, this, FileUtility.getNamesListAsString(resources));
		IRepositoryLocation location = destination.getRepositoryLocation();
		final ISVNConnector proxy = location.acquireSVNProxy();
		try {
			final String destinationUrl = SVNUtility.encodeURL(destination.getUrl());
			for (IResource element : resources) {
				final String wcPath = FileUtility.getWorkingCopyPath(element);

				ISVNNotificationCallback notify = info -> {
					if (info.revision != -1) {
						String message = BaseMessages.format(SVNMessages.Console_CommittedRevision,
								new String[] { String.valueOf(info.revision) });
						BranchTagOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
					}
				};
				SVNUtility.addSVNNotifyListener(proxy, notify);

				this.protectStep(monitor1 -> {
					BranchTagOperation.this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn copy \"" + wcPath //$NON-NLS-1$
							+ "\" \"" + destinationUrl + "\" -r " + SVNRevision.WORKING + " -m \"" //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							+ message + "\"" //$NON-NLS-1$
							+ ISVNConnector.Options.asCommandLine(ISVNConnector.Options.INCLUDE_PARENTS)
							+ FileUtility.getUsernameParam(
									destination.getRepositoryLocation().getUsername())
							+ "\n"); //$NON-NLS-1$
					SVNEntryRevisionReference[] src = {
							new SVNEntryRevisionReference(wcPath, SVNRevision.WORKING, SVNRevision.WORKING) };
					proxy.copyRemote(src, destinationUrl, message, ISVNConnector.Options.INCLUDE_PARENTS, null,
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
