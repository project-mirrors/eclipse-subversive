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

package org.eclipse.team.svn.ui.action.local;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.ResourcesTraversalOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.RevertOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.UpdateOperation;
import org.eclipse.team.svn.core.operation.local.change.IActionOperationProcessor;
import org.eclipse.team.svn.core.operation.local.change.IResourceChangeVisitor;
import org.eclipse.team.svn.core.operation.local.change.ResourceChange;
import org.eclipse.team.svn.core.operation.local.change.visitors.RemoveNonVersionedVisitor;
import org.eclipse.team.svn.core.resource.ILocalFile;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractNonRecursiveTeamAction;
import org.eclipse.team.svn.ui.dialog.ReplaceWarningDialog;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Team services menu "replace with latest revision" action implementation
 * 
 * @author Alexander Gurov
 */
public class ReplaceWithLatestRevisionAction extends AbstractNonRecursiveTeamAction {
	public ReplaceWithLatestRevisionAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource[] resources = this.getSelectedResources(IStateFilter.SF_ONREPOSITORY);
		IActionOperation op = ReplaceWithLatestRevisionAction.getReplaceOperation(resources, getShell());
		if (op != null) {
			runScheduled(op);
		}
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresence(IStateFilter.SF_ONREPOSITORY);
	}

	public static IActionOperation getReplaceOperation(IResource[] resources, Shell shell) {
		ReplaceWarningDialog dialog = new ReplaceWarningDialog(shell);
		if (dialog.open() == 0) {
			CompositeOperation op = new CompositeOperation("Operation_ReplaceWithLatest", SVNUIMessages.class); //$NON-NLS-1$

			SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
			op.add(saveOp);

			SaveUnversionedOperation saveUnversioned = new SaveUnversionedOperation(resources);
			op.add(saveUnversioned);

			IActionOperation revertOp = new RevertOperation(resources, true);
			op.add(revertOp);
			IActionOperation removeOp = new ResourcesTraversalOperation("Operation_RemoveNonSVN", SVNMessages.class,
					resources, new RemoveNonVersionedVisitor(true), IResource.DEPTH_INFINITE);
			op.add(removeOp, new IActionOperation[] { revertOp });
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			op.add(new UpdateOperation(resources, ignoreExternals), new IActionOperation[] { revertOp, removeOp });

			op.add(new RestoreUnversionedOperation(resources, saveUnversioned));

			op.add(new RestoreProjectMetaOperation(saveOp));
			op.add(new RefreshResourcesOperation(resources));

			return op;
		}
		return null;
	}

	protected static class SaveUnversionedOperation extends AbstractWorkingCopyOperation
			implements IActionOperationProcessor {
		public List<ResourceChange> changes;

		public SaveUnversionedOperation(IResource[] resources) {
			super("Operation_SaveUnversioned", SVNUIMessages.class, resources); //$NON-NLS-1$
			changes = new ArrayList<>();
		}

		@Override
		public void doOperation(IActionOperation op, IProgressMonitor monitor) {
			this.reportStatus(op.run(monitor).getStatus());
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			IResource[] resources = operableData();
			for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
				final IResource current = resources[i];
				this.protectStep(monitor1 -> {
					ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(current);
					ResourceChange change = ResourceChange.wrapLocalResource(null, local, true);
					change.traverse(new IResourceChangeVisitor() {
						@Override
						public void preVisit(ResourceChange change, IActionOperationProcessor processor,
								IProgressMonitor monitor) throws Exception {
							ILocalResource local = change.getLocal();
							if (local instanceof ILocalFile && IStateFilter.SF_UNVERSIONED.accept(local)
									&& !local.getResource().isDerived(IResource.CHECK_ANCESTORS)) {
								File real = new File(FileUtility.getWorkingCopyPath(local.getResource()));
								// optimize operation performance using "move on FS" if possible
								if (real.exists() && !real.renameTo(change.getTemporary())) {
									FileUtility.copyFile(change.getTemporary(), real, monitor);
									real.delete();
								}
							}
						}

						@Override
						public void postVisit(ResourceChange change, IActionOperationProcessor processor,
								IProgressMonitor monitor) throws Exception {
						}
					}, IResource.DEPTH_INFINITE, SaveUnversionedOperation.this, monitor1);
					changes.add(change);
				}, monitor, resources.length);
			}
		}

	}

	protected static class RestoreUnversionedOperation extends AbstractWorkingCopyOperation
			implements IActionOperationProcessor {
		public SaveUnversionedOperation changes;

		public RestoreUnversionedOperation(IResource[] resources, SaveUnversionedOperation changes) {
			super("Operation_RestoreUnversioned", SVNUIMessages.class, resources); //$NON-NLS-1$
			this.changes = changes;
		}

		@Override
		public void doOperation(IActionOperation op, IProgressMonitor monitor) {
			this.reportStatus(op.run(monitor).getStatus());
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			ResourceChange[] changes = this.changes.changes.toArray(new ResourceChange[0]);
			for (int i = 0; i < changes.length && !monitor.isCanceled(); i++) {
				final ResourceChange change = changes[i];
				this.protectStep(monitor1 -> change.traverse(new IResourceChangeVisitor() {
					@Override
					public void preVisit(ResourceChange change, IActionOperationProcessor processor,
							IProgressMonitor monitor) throws Exception {
					}

					@Override
					public void postVisit(ResourceChange change, IActionOperationProcessor processor,
							IProgressMonitor monitor) throws Exception {
						ILocalResource local = change.getLocal();
						if (local instanceof ILocalFile && IStateFilter.SF_UNVERSIONED.accept(local)
								&& !local.getResource().isDerived(IResource.CHECK_ANCESTORS)) {
							File real = new File(FileUtility.getWorkingCopyPath(local.getResource()));
							// optimize operation performance using "move on FS" if possible
							if (!real.exists()) {
								real.getParentFile().mkdirs();
								if (!change.getTemporary().renameTo(real)) {
									FileUtility.copyFile(real, change.getTemporary(), monitor);
									change.getTemporary().delete();
								}
							}
						}
					}
				}, IResource.DEPTH_INFINITE, RestoreUnversionedOperation.this, monitor1), monitor, changes.length);
			}
		}

	}

}
