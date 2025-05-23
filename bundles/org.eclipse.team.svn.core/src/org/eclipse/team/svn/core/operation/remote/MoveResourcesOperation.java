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
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * Move remote resources
 * 
 * @author Alexander Gurov
 */
public class MoveResourcesOperation extends AbstractCopyMoveResourcesOperation {
	public MoveResourcesOperation(IRepositoryResource destinationResource, IRepositoryResource[] selectedResources,
			String message, String name) {
		super("Operation_MoveRemote", SVNMessages.class, destinationResource, selectedResources, message, name); //$NON-NLS-1$
	}

	@Override
	protected String[] getRevisionPaths(String srcUrl, String dstUrl) {
		return new String[] { srcUrl, dstUrl };
	}

	@Override
	protected void runCopyMove(ISVNConnector proxy, SVNEntryRevisionReference[] source, String[] sourcePaths,
			String destinationUrl, IProgressMonitor monitor) throws Exception {
		//this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn move \"" + SVNUtility.decodeURL(sourceUrl) + "\" \"" + SVNUtility.decodeURL(destinationUrl) + "\" -m \"" + this.message + "\"" + FileUtility.getUsernameParam(current.getRepositoryLocation().getUsername()) + "\n");
		proxy.moveRemote(sourcePaths, destinationUrl, message, ISVNConnector.CommandMasks.COPY_SERVER, null,
				new SVNProgressMonitor(this, monitor, null));
	}

}
