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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNCommitStatus;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IPostCommitErrorsProvider;
import org.eclipse.team.svn.core.operation.local.change.FolderChange;
import org.eclipse.team.svn.core.operation.local.change.IActionOperationProcessor;
import org.eclipse.team.svn.core.operation.local.change.ResourceChange;
import org.eclipse.team.svn.core.operation.local.change.visitors.CompositeVisitor;
import org.eclipse.team.svn.core.operation.local.change.visitors.RemoveNonVersionedVisitor;
import org.eclipse.team.svn.core.operation.local.change.visitors.RestoreContentVisitor;
import org.eclipse.team.svn.core.operation.local.change.visitors.RestorePropertiesVisitor;
import org.eclipse.team.svn.core.operation.local.change.visitors.SaveContentVisitor;
import org.eclipse.team.svn.core.operation.local.change.visitors.SavePropertiesVisitor;
import org.eclipse.team.svn.core.operation.local.refactor.DeleteResourceOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Mark as merged operation implementation
 * 
 * @author Alexander Gurov
 */
public class MarkAsMergedOperation extends AbstractWorkingCopyOperation
		implements IActionOperationProcessor, IResourceProvider, IPostCommitErrorsProvider {
	protected boolean override;

	protected boolean keepLocks;

	protected String overrideMessage;

	protected IResource[] committables = {};

	protected IResource[] withDifferentNodeKind = {};

	protected boolean ignoreExternals;

	protected ArrayList<SVNCommitStatus> postCommitErrors;

	public MarkAsMergedOperation(IResource[] resources, boolean override, String overrideMessage,
			boolean ignoreExternals) {
		this(resources, override, overrideMessage, false, ignoreExternals);
	}

	public MarkAsMergedOperation(IResource[] resources, boolean override, String overrideMessage, boolean keepLocks,
			boolean ignoreExternals) {
		super("Operation_MarkAsMerged", SVNMessages.class, resources); //$NON-NLS-1$
		this.override = override;
		this.overrideMessage = overrideMessage;
		this.keepLocks = keepLocks;
		this.ignoreExternals = ignoreExternals;
	}

	public MarkAsMergedOperation(IResourceProvider provider, boolean override, String overrideMessage,
			boolean keepLocks, boolean ignoreExternals) {
		super("Operation_MarkAsMerged", SVNMessages.class, provider); //$NON-NLS-1$
		this.override = override;
		this.overrideMessage = overrideMessage;
		this.keepLocks = keepLocks;
		this.ignoreExternals = ignoreExternals;
	}

	public MarkAsMergedOperation(IResourceProvider provider, boolean override, String overrideMessage,
			boolean ignoreExternals) {
		this(provider, override, overrideMessage, false, ignoreExternals);
	}

	@Override
	public IResource[] getResources() {
		return committables;
	}

	public IResource[] getHavingDifferentNodeKind() {
		return withDifferentNodeKind;
	}

	@Override
	public SVNCommitStatus[] getPostCommitErrors() {
		return postCommitErrors == null ? null : postCommitErrors.toArray(new SVNCommitStatus[postCommitErrors.size()]);
	}

	@Override
	public void doOperation(IActionOperation op, IProgressMonitor monitor) {
		this.reportStatus(op.run(monitor).getStatus());
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = FileUtility.shrinkChildNodesWithSwitched(operableData());
		final ArrayList<IResource> committables = new ArrayList<>();
		final ArrayList<IResource> withDifferentNodeKind = new ArrayList<>();
		postCommitErrors = new ArrayList<>();

		for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
			final IResource current = resources[i];
			this.protectStep(monitor1 -> {
				ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(current);
				if (IStateFilter.SF_DELETED.accept(local) && !IStateFilter.SF_PREREPLACEDREPLACED.accept(local)) {
					MarkAsMergedOperation.this.markDeleted(local, new NullProgressMonitor());
					committables.add(local.getResource());
				} else if (!IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
					if ((local.getChangeMask() & ILocalResource.TREE_CONFLICT_UNKNOWN_NODE_KIND) != 0) {
						MarkAsMergedOperation.this.doOperation(
								new RevertOperation(new IResource[] { local.getResource() }, true), monitor1);
						MarkAsMergedOperation.this.doOperation(
								new RefreshResourcesOperation(new IResource[] { local.getResource() },
										IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL),
								monitor1);
					} else {
						boolean nodeKindChanged = MarkAsMergedOperation.this.markExisting(local, monitor1);
						if (!nodeKindChanged) {
							committables.add(local.getResource());
						} else {
							withDifferentNodeKind.add(local.getResource());
						}
					}
				}
			}, monitor, resources.length);
		}

		this.committables = committables.toArray(new IResource[committables.size()]);
		this.withDifferentNodeKind = withDifferentNodeKind.toArray(new IResource[withDifferentNodeKind.size()]);
	}

	protected void markDeleted(ILocalResource local, IProgressMonitor monitor) throws Exception {
		doOperation(new RevertOperation(new IResource[] { local.getResource() }, true), monitor);
		doOperation(new UpdateOperation(new IResource[] { local.getResource() }, getRevisionToUpdate(local),
				ignoreExternals), monitor);
		//don't delete the resource which already doesn't exist on file system
		//this can happen with tree conflicts, for instance, local - delete and remote - delete
		File f = new File(FileUtility.getWorkingCopyPath(local.getResource()));
		if (f.exists()) {
			doOperation(new DeleteResourceOperation(local.getResource()), monitor);
		}
	}

	/*
	 * Steps:
	 * 	1. Temporary save props and content of the resource (see SavePropertiesVisitor, SaveContentVisitor)
	 * 	2. Revert
	 * 	3. Remove all non versioned resources on file system
	 * 	4. Update
	 * 	5. If 'override' flag is true and local resource exists:
	 * 		5.1	If resource doesn't exist on repository, then delete it locally (see prepareToOverride)
	 * 		5.2	Commit
	 * 	6. Restore props and content from temporary storage
	 */
	protected boolean markExisting(ILocalResource local, IProgressMonitor monitor) throws Exception {
		boolean nodeKindChanged = false;
		ResourceChange change = ResourceChange.wrapLocalResource(null, local, true);

		try {
			CompositeVisitor visitor = new CompositeVisitor();
			visitor.add(new SavePropertiesVisitor());
			visitor.add(new SaveContentVisitor());
			change.traverse(visitor, IResource.DEPTH_INFINITE, this, monitor);

			if (FileUtility.checkForResourcesPresenceRecursive(new IResource[] { local.getResource() },
					IStateFilter.SF_REVERTABLE)) {
				doOperation(new RevertOperation(new IResource[] { local.getResource() }, true), monitor);
			}

			change.traverse(new RemoveNonVersionedVisitor(true), IResource.DEPTH_INFINITE, this, monitor);

			doOperation(new UpdateOperation(new IResource[] { local.getResource() }, getRevisionToUpdate(local),
					ignoreExternals), monitor);
			String wcPath = FileUtility.getWorkingCopyPath(local.getResource());
			boolean isLocalExists = new File(wcPath).exists();
			if (override && isLocalExists) {
				nodeKindChanged = prepareToOverride(change, monitor);
				//do additional check for exists because prepareToOverride can delete resources
				if (new File(wcPath).exists()) {
					CommitOperation op = new CommitOperation(new IResource[] { local.getResource() }, overrideMessage,
							true, keepLocks);
					doOperation(op, monitor);
					SVNCommitStatus[] errors = op.getPostCommitErrors();
					if (errors != null) {
						postCommitErrors.addAll(Arrays.asList(errors));
					}
				}
			}

			visitor = new CompositeVisitor();
			visitor.add(new RestoreContentVisitor(nodeKindChanged));
			visitor.add(new RestorePropertiesVisitor());
			change.traverse(visitor, IResource.DEPTH_INFINITE, this, monitor);
		} finally {
			change.disposeChangeModel(monitor);
		}

		return nodeKindChanged;
	}

	protected SVNRevision getRevisionToUpdate(ILocalResource local) throws Exception {
		AbstractSVNSyncInfo syncInfo = (AbstractSVNSyncInfo) UpdateSubscriber.instance()
				.getSyncInfo(local.getResource());
		long revNum = SVNRevision.INVALID_REVISION_NUMBER;
		if (syncInfo != null) {
			ILocalResource remoteResource = syncInfo.getRemoteChangeResource();
			revNum = remoteResource.getRevision();
		}
		return revNum == SVNRevision.INVALID_REVISION_NUMBER ? SVNRevision.HEAD : SVNRevision.fromNumber(revNum);
	}

	protected boolean prepareToOverride(ResourceChange change, IProgressMonitor monitor) {
		boolean nodeKindBeforeUpdate = change instanceof FolderChange;
		if (nodeKindBeforeUpdate) {
			ResourceChange[] children = ((FolderChange) change).getChildren();
			for (ResourceChange child : children) {
				prepareToOverride(child, monitor);
			}
		}
		ILocalResource local = change.getLocal();
		File real = new File(FileUtility.getWorkingCopyPath(local.getResource()));
		boolean nodeKindChanged = false;
		if (real.exists()) {
			nodeKindChanged = nodeKindBeforeUpdate != real.isDirectory();
			if (IStateFilter.SF_NOTONREPOSITORY.accept(local) || nodeKindChanged) {
				doOperation(new DeleteResourceOperation(local.getResource()), monitor);
			}
		}
		return nodeKindChanged;
	}

}
