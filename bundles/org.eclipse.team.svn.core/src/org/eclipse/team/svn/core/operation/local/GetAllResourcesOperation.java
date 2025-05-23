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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;

/**
 * This operation find all children including deleted for the resource specified
 * 
 * @author Alexander Gurov
 */
public class GetAllResourcesOperation extends AbstractActionOperation {
	protected IContainer container;

	protected IResource[] children;

	public GetAllResourcesOperation(IContainer container) {
		super("Operation_GetResourceList", SVNMessages.class); //$NON-NLS-1$
		this.container = container;
		children = new IResource[0];
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] retVal = SVNRemoteStorage.instance().getRegisteredChildren(container);
		if (retVal != null) {
			children = retVal;
		}
	}

	public IResource[] getChildren() {
		return children;
	}

}
