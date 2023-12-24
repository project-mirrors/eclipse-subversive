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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.remote;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;

/**
 * Composite operation which prepares destination folder (if it doesn't exist on the repository) and then makes branch or tag for the
 * selected resources
 *
 * @author Sergiy Logvin
 */
public class PreparedBranchTagOperation extends CompositeOperation implements IRepositoryResourceProvider {
	protected IRepositoryResource destination;

	protected IRepositoryResource[] resources;

	protected String message;

	protected String operationName;

	protected boolean forceCreate;

	protected IRepositoryResource[] targets;

	protected IResource[] wcResources;

	public PreparedBranchTagOperation(String operationName, IResource[] wcResources, IRepositoryResource[] resources,
			IRepositoryResource destination, String message, boolean forceCreate) {
		this(operationName, resources, destination, message, forceCreate);
		this.wcResources = wcResources;
	}

	public PreparedBranchTagOperation(String operationName, IRepositoryResource[] resources,
			IRepositoryResource destination, String message, boolean forceCreate) {
		super("Operation_Prepared" + operationName, SVNMessages.class); //$NON-NLS-1$
		this.operationName = operationName;
		this.resources = resources;
		this.destination = destination;
		this.message = message;
		this.forceCreate = forceCreate;
	}

	@Override
	public IRepositoryResource[] getRepositoryResources() {
		return targets;
	}

	public IRepositoryResource getDestination() {
		return destination;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource parent = PreparedBranchTagOperation.getExistentParent(destination);
		if (parent == null) {
			throw new UnreportableException(SVNMessages.getErrorString("Error_RepositoryInaccessible")); //$NON-NLS-1$
		}
		boolean createLastSegment = false;
		IPath newFolderPath = null;
		if (parent != destination) {
			newFolderPath = new Path(destination.getUrl().substring(parent.getUrl().length() + 1));
			createLastSegment = resources.length == 1 && !newFolderPath.isEmpty() && !forceCreate;
		}
		targets = new IRepositoryResource[resources.length];
		for (int i = 0; i < targets.length; i++) {
			String targetUrl = destination.getUrl();
			if (!createLastSegment) {
				targetUrl += "/" + resources[i].getName(); //$NON-NLS-1$
			}
			targets[i] = resources[i] instanceof IRepositoryFile
					? (IRepositoryResource) destination.asRepositoryFile(targetUrl, false)
					: destination.asRepositoryContainer(targetUrl, false);
		}

		if (CoreExtensionsManager.instance()
				.getSVNConnectorFactory()
				.getSVNAPIVersion() < ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x || wcResources != null) {
			CreateFolderOperation op = null;
			if (newFolderPath != null) {
				IPath cutPath = newFolderPath.removeLastSegments(1);
				if (resources.length != 1 || !cutPath.isEmpty() || forceCreate) {
					String folderName = resources.length == 1 && !cutPath.isEmpty() && !forceCreate
							? cutPath.toString()
							: newFolderPath.toString();
					this.add(op = new CreateFolderOperation(parent, folderName, message));
					this.add(new SetRevisionAuthorNameOperation(op, Options.FORCE), new IActionOperation[] { op });
				}
			}
			if (wcResources != null) {
				for (int i = 0; i < targets.length; i++) {
					this.add(
							new org.eclipse.team.svn.core.operation.local.BranchTagOperation(operationName,
									new IResource[] { wcResources[i] }, targets[i], message),
							op == null ? null : new IActionOperation[] { op });
				}
			} else {
				BranchTagOperation branchtagOp = new BranchTagOperation(operationName, SVNMessages.class, resources,
						destination, message);
				this.add(branchtagOp, op == null ? null : new IActionOperation[] { op });
				this.add(new SetRevisionAuthorNameOperation(branchtagOp, Options.FORCE));
			}
		} else {
			// SVN 1.5 allows to create branch/tag for repository resources at once
			CopyResourcesOperation branchtagOp = forceCreate
					? new CopyResourcesOperation(destination, resources, message, resources[0].getName())
					: new CopyResourcesOperation(destination.getParent(), resources, message, destination.getName());
			this.add(branchtagOp);
			this.add(new SetRevisionAuthorNameOperation(branchtagOp, Options.FORCE));
		}
		super.runImpl(monitor);
	}

	protected static IRepositoryResource getExistentParent(IRepositoryResource notExistentResource) throws Exception {
		if (notExistentResource == null) {
			return null;
		}
		return notExistentResource.exists()
				? notExistentResource
				: PreparedBranchTagOperation.getExistentParent(notExistentResource.getParent());
	}

}
