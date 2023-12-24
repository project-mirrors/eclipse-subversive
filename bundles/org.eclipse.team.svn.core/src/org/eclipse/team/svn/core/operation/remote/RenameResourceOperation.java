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
 * Rename remote resource operation implementation
 * 
 * @author Alexander Gurov
 */
public class RenameResourceOperation extends AbstractRepositoryOperation implements IRevisionProvider {
	protected String message;

	protected String newName;

	protected RevisionPair[] revisionPair;

	public RenameResourceOperation(IRepositoryResource resource, String newName, String message) {
		super("Operation_Rename", SVNMessages.class, new IRepositoryResource[] { resource }); //$NON-NLS-1$
		this.message = message;
		this.newName = newName;
	}

	@Override
	public RevisionPair[] getRevisions() {
		return revisionPair;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource resource = operableData()[0];
		revisionPair = new RevisionPair[1];
		final IRepositoryLocation location = resource.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		final String newUrl = resource.getParent().getUrl() + "/" + newName; //$NON-NLS-1$
		ISVNNotificationCallback notify = info -> {
			String[] path = { newUrl };
			revisionPair[0] = new RevisionPair(info.revision, path, location);
			String message = BaseMessages.format(SVNMessages.Console_CommittedRevision,
					new String[] { String.valueOf(info.revision) });
			RenameResourceOperation.this.writeToConsole(IConsoleStream.LEVEL_OK, message);
		};
		try {
			SVNUtility.addSVNNotifyListener(proxy, notify);
			writeToConsole(IConsoleStream.LEVEL_CMD,
					"svn move \"" + resource.getUrl() + "\" \"" + newUrl + "\" -m \"" + message + "\"" //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
							+ ISVNConnector.Options.asCommandLine(ISVNConnector.Options.INTERPRET_AS_CHILD)
							+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
			proxy.moveRemote(new String[] { SVNUtility.encodeURL(resource.getUrl()) }, SVNUtility.encodeURL(newUrl),
					message, ISVNConnector.Options.INTERPRET_AS_CHILD, null,
					new SVNProgressMonitor(this, monitor, null));
		} finally {
			SVNUtility.removeSVNNotifyListener(proxy, notify);
			resource.getRepositoryLocation().releaseSVNProxy(proxy);
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t),
				new Object[] { operableData()[0].getName(), newName });
	}

}
