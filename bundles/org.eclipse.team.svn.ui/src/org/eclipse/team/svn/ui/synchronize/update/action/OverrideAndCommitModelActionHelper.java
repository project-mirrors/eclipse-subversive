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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.update.action;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNWithPropertiesOperation;
import org.eclipse.team.svn.core.operation.local.ClearLocalStatusesOperation;
import org.eclipse.team.svn.core.operation.local.CommitOperation;
import org.eclipse.team.svn.core.operation.local.MarkAsMergedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.synchronize.UpdateSyncInfo;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.NotifyNodeKindChangedDialog;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.extension.factory.ICommitDialog;
import org.eclipse.team.svn.ui.operation.ClearUpdateStatusesOperation;
import org.eclipse.team.svn.ui.operation.ShowPostCommitErrorsOperation;
import org.eclipse.team.svn.ui.operation.TreatAsEditsOperation;
import org.eclipse.team.svn.ui.panel.local.CommitPanel;
import org.eclipse.team.svn.ui.synchronize.SVNChangeSetCapability;
import org.eclipse.team.svn.ui.synchronize.action.AbstractActionHelper;
import org.eclipse.team.svn.ui.synchronize.action.ISyncStateFilter;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.utility.UnacceptableOperationNotificator;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Override and commit conflicting files action helper
 * 
 * @author Igor Burilo
 */
public class OverrideAndCommitModelActionHelper extends AbstractActionHelper {

	public OverrideAndCommitModelActionHelper(IAction action, ISynchronizePageConfiguration configuration) {
		super(action, configuration);
	}

	@Override
	public FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(
				new int[] { SyncInfo.CONFLICTING, SyncInfo.OUTGOING, SyncInfo.INCOMING }) {
			@Override
			public boolean select(SyncInfo info) {
				UpdateSyncInfo sync = (UpdateSyncInfo) info;
				return super.select(info) && !IStateFilter.SF_OBSTRUCTED.accept(sync.getLocalResource());
			}
		};
	}

	@Override
	public IActionOperation getOperation() {
		String msg = null;
		boolean keepLocks = false;
		final IResource[][] resources = new IResource[1][];
		IResource[] treatAsEdits = null;

		IResource[] changedResources = getSyncInfoSelector()
				.getSelectedResourcesRecursive(ISyncStateFilter.SF_OVERRIDE);
		IResource[] overrideResources = UnacceptableOperationNotificator
				.shrinkResourcesWithNotOnRespositoryParents(configuration.getSite().getShell(), changedResources);
		if (overrideResources != null && overrideResources.length > 0) {
			overrideResources = FileUtility.addOperableParents(overrideResources, IStateFilter.SF_NOTONREPOSITORY);
			HashSet<IResource> allResourcesSet = new HashSet<>(Arrays.asList(overrideResources));
			String proposedComment = SVNChangeSetCapability.getProposedComment(overrideResources);
			CommitPanel commitPanel = new CommitPanel(overrideResources, overrideResources,
					CommitPanel.MSG_OVER_AND_COMMIT, proposedComment);
			ICommitDialog commitDialog = ExtensionsManager.getInstance()
					.getCurrentCommitFactory()
					.getCommitDialog(configuration.getSite().getShell(), allResourcesSet, commitPanel);
			if (commitDialog.open() != 0) {
				return null;
			}
			treatAsEdits = commitPanel.getTreatAsEdits();
			resources[0] = commitPanel.getSelectedResources().length == 0 ? null : commitPanel.getSelectedResources();
			msg = commitDialog.getMessage();
			keepLocks = commitPanel.getKeepLocks();
		}

		if (resources[0] == null) {
			return null;
		}

		CompositeOperation op = new CompositeOperation("Operation_UOverrideAndCommit", SVNUIMessages.class); //$NON-NLS-1$

		if (treatAsEdits != null && treatAsEdits.length > 0) {
			op.add(new TreatAsEditsOperation(treatAsEdits));
		}

		final MarkAsMergedOperation mergeOp = new MarkAsMergedOperation(resources[0], true, msg, keepLocks);
		op.add(mergeOp);
		op.add(new ShowPostCommitErrorsOperation(mergeOp));
		final IResource[] addition = FileUtility.getResourcesRecursive(resources[0],
				OverrideAndCommitModelActionHelper.SF_NEW);
		if (addition.length != 0) {
			IResourceProvider additionProvider = new IResourceProvider() {
				protected IResource[] result;

				@Override
				public IResource[] getResources() {
					if (result == null) {
						HashSet<IResource> tAdd = new HashSet<>(Arrays.asList(addition));
						IResource[] restricted = mergeOp.getHavingDifferentNodeKind();
						for (IResource element : restricted) {
							if (element instanceof IContainer) {//delete from add to SVN list, resources, with nodekind changed, and all their children
								IResource[] restrictedChildren = FileUtility.getResourcesRecursive(resources[0],
										IStateFilter.SF_ALL);
								tAdd.removeAll(Arrays.asList(restrictedChildren));
							} else {
								tAdd.remove(element);
							}

						}

						result = tAdd.toArray(new IResource[tAdd.size()]);
					}
					return result;
				}
			};
			op.add(new AddToSVNWithPropertiesOperation(additionProvider, false), new IActionOperation[] { mergeOp });
			op.add(new ClearLocalStatusesOperation(additionProvider));
		}
		CommitOperation mainOp = new CommitOperation(mergeOp, msg, true, keepLocks);
		IActionOperation[] dependsOn = { mergeOp };
		op.add(mainOp, dependsOn);
		op.add(new AbstractActionOperation("Operation_UNodeKindChanged", SVNUIMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				final IResource[] diffNodeKind = mergeOp.getHavingDifferentNodeKind();
				if (diffNodeKind.length > 0) {
					UIMonitorUtility.getDisplay().syncExec(() -> new NotifyNodeKindChangedDialog(UIMonitorUtility.getShell(), diffNodeKind).open());
				}
			}
		});
		op.add(new ShowPostCommitErrorsOperation(mainOp));
		op.add(new ClearUpdateStatusesOperation(resources[0]), new IActionOperation[] { mainOp });
		op.add(new RefreshResourcesOperation(resources[0]));
		ExtensionsManager.getInstance()
				.getCurrentCommitFactory()
				.performAfterCommitTasks(op, mainOp, dependsOn, configuration.getSite().getPart());
		return op;
	}

	public static final IStateFilter SF_NEW = new IStateFilter.AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_NEW;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

}
