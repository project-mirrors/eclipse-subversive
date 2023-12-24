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

package org.eclipse.team.svn.ui.utility;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNWithPropertiesOperation;
import org.eclipse.team.svn.core.operation.local.ClearLocalStatusesOperation;
import org.eclipse.team.svn.core.operation.local.CommitOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.SetRevisionAuthorNameOperation;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.action.IResourceSelector;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.operation.ClearUpdateStatusesOperation;
import org.eclipse.team.svn.ui.operation.NotifyUnresolvedConflictOperation;
import org.eclipse.team.svn.ui.operation.ShowPostCommitErrorsOperation;
import org.eclipse.team.svn.ui.operation.TreatAsEditsOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Utility class for package explorer and synchronize view commit actions
 * 
 * @author Sergiy Logvin
 */
public class CommitActionUtility {
	protected IResourceSelector selector;

	protected HashSet<IResource> newNonRecursive;

	protected HashSet<IResource> newRecursive;

	protected HashSet<IResource> parents;

	protected IResource[] allResources;

	protected HashSet<IResource> allResourcesSet;

	/*
	 * Determines whether we can call commit recursively.
	 * We can't call recursively in following cases:
	 * 	1. if there are externals among resources which we'll be shown in commit dialog
	 *  (if call commit on not external directory, externals will not be committed,
	 *  because commit doesn't handle externals(in contrast e.g. to 'update'))
	 *  2. if selected resource and its parent don't exist in repository
	 *  (if selected resource is new(or added) we show also its parent in commit dialog,
	 *  because we can't directly commit resource which parent doesn't exist in repository and
	 *  so we can't commit recursively)
	 */
	protected boolean canBeRecursiveCommit = true;

	public CommitActionUtility(IResourceSelector selector) {
		initialize(selector);
	}

	public void initialize(IResourceSelector selector) {
		this.selector = selector;

		allResourcesSet = new HashSet<>();
		allResourcesSet.addAll(Arrays.asList(this.selector.getSelectedResourcesRecursive(
				new IStateFilter.OrStateFilter(new IStateFilter[] { IStateFilter.SF_COMMITABLE,
						IStateFilter.SF_CONFLICTING, IStateFilter.SF_TREE_CONFLICTING, IStateFilter.SF_NEW }))));

		newNonRecursive = new HashSet<>(
				Arrays.asList(this.selector.getSelectedResources(IStateFilter.SF_IGNORED_NOT_FORBIDDEN)));
		newRecursive = new HashSet<>(Arrays.asList(FileUtility.getResourcesRecursive(
				allResourcesSet.toArray(new IResource[allResourcesSet.size()]), IStateFilter.SF_NEW,
				IResource.DEPTH_ZERO)));

		HashSet<IResource> fullSet = new HashSet<>(newNonRecursive);
		fullSet.addAll(newRecursive);
		parents = new HashSet<>(Arrays.asList(FileUtility.getOperableParents(
				fullSet.toArray(new IResource[fullSet.size()]), IStateFilter.SF_UNVERSIONED)));
		newNonRecursive.addAll(parents);
		fullSet.addAll(parents);

		allResourcesSet.addAll(fullSet);

		allResources = allResourcesSet.toArray(new IResource[allResourcesSet.size()]);
		allResourcesSet
				.addAll(Arrays.asList(FileUtility.addOperableParents(allResources, IStateFilter.SF_ADDED, true)));
		allResources = allResourcesSet.toArray(new IResource[allResourcesSet.size()]);

		canBeRecursiveCommit = FileUtility.getOperableParents(this.selector.getSelectedResources(),
				IStateFilter.SF_ADDED, false).length == 0;
		if (canBeRecursiveCommit && FileUtility.checkForResourcesPresence(allResources, IStateFilter.SF_SWITCHED,
				IResource.DEPTH_ZERO)) {
			canBeRecursiveCommit = false;
		}
	}

	public HashSet<IResource> getAllResourcesSet() {
		return allResourcesSet;
	}

	public IResource[] getAllResources() {
		return allResources;
	}

	public CompositeOperation getCompositeCommitOperation(IResource[] selectedResources, IResource[] treatAsEdits,
			String message, boolean keepLocks, Shell shell, IWorkbenchPart part) {
		return getNonRecursiveImpl(selectedResources, treatAsEdits, message, keepLocks, shell, part);
	}

	/*
	 * We separate commit on recursive and not recursive because of performance reasons, i.e. recursive
	 * commit works significantly faster.
	 */
	public CompositeOperation getCompositeCommitOperation(IResource[] selectedResources,
			IResource[] notSelectedResources, IResource[] treatAsEdits, String message, boolean keepLocks, Shell shell,
			IWorkbenchPart part, boolean tryRecursive) {
		return canBeRecursiveCommit && tryRecursive
				? getRecursiveImpl(selectedResources, notSelectedResources, treatAsEdits, message, keepLocks, shell,
						part)
				: getNonRecursiveImpl(selectedResources, treatAsEdits, message, keepLocks, shell, part);
	}

	protected CompositeOperation getRecursiveImpl(IResource[] selectedResources, IResource[] notSelectedResources,
			IResource[] treatAsEdits, String message, boolean keepLocks, Shell shell, IWorkbenchPart part) {
		IResource[] notSelectedNew = FileUtility.getResourcesRecursive(notSelectedResources,
				IStateFilter.SF_UNVERSIONED, IResource.DEPTH_ZERO);
		boolean allowsRecursiveAdd = true;
		for (IResource element : notSelectedNew) {
			if (newRecursive.remove(element)) {
				allowsRecursiveAdd = false;
			} else if (!parents.contains(element)) {
				newNonRecursive.remove(element);
			}
		}

		CommitOperation mainOp = new CommitOperation(
				notSelectedResources.length == 0 ? selector.getSelectedResources() : selectedResources, message,
				allowsRecursiveAdd && notSelectedNew.length == notSelectedResources.length, keepLocks);

		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

		if (treatAsEdits != null && treatAsEdits.length > 0) {
			op.add(new TreatAsEditsOperation(treatAsEdits));
		}

		if (allowsRecursiveAdd) {
			if (newNonRecursive.size() > 0) {
				IResource[] newNonRecursive = this.newNonRecursive.toArray(new IResource[this.newNonRecursive.size()]);
				op.add(new AddToSVNWithPropertiesOperation(newNonRecursive, false));
				op.add(new ClearLocalStatusesOperation(newNonRecursive));
			}
			if (newRecursive.size() > 0) {
				IResource[] newRecursive = this.newRecursive.toArray(new IResource[this.newRecursive.size()]);
				op.add(new AddToSVNWithPropertiesOperation(newRecursive, true));
				op.add(new ClearLocalStatusesOperation(newRecursive));
			}
		} else {
			newNonRecursive.addAll(newRecursive);
			newRecursive.clear();
			IResource[] newNonRecursive = this.newNonRecursive.toArray(new IResource[this.newNonRecursive.size()]);
			op.add(new AddToSVNWithPropertiesOperation(newNonRecursive, false));
			op.add(new ClearLocalStatusesOperation(newNonRecursive));
		}

		addCommonPart(selectedResources, op, mainOp, shell, part);
		op.add(new SetRevisionAuthorNameOperation(mainOp, Options.FORCE), new IActionOperation[] { mainOp });

		return op;
	}

	protected CompositeOperation getNonRecursiveImpl(IResource[] selectedResources, IResource[] treatAsEdits,
			String message, boolean keepLocks, Shell shell, IWorkbenchPart part) {
		CommitOperation mainOp = new CommitOperation(selectedResources, message, false, keepLocks);

		CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

		if (treatAsEdits != null && treatAsEdits.length > 0) {
			op.add(new TreatAsEditsOperation(treatAsEdits));
		}

		IResource[] newResources = FileUtility.getResourcesRecursive(selectedResources, IStateFilter.SF_UNVERSIONED,
				IResource.DEPTH_ZERO);
		if (newResources.length > 0) {
			op.add(new AddToSVNWithPropertiesOperation(newResources, false));
			op.add(new ClearLocalStatusesOperation(newResources));
		}

		addCommonPart(selectedResources, op, mainOp, shell, part);
		op.add(new SetRevisionAuthorNameOperation(mainOp, Options.FORCE), new IActionOperation[] { mainOp });

		return op;
	}

	protected void addCommonPart(IResource[] selectedResources, CompositeOperation op, CommitOperation mainOp,
			Shell shell, IWorkbenchPart part) {
		op.add(mainOp);
		op.add(new ClearUpdateStatusesOperation(selectedResources), new IActionOperation[] { mainOp });
		op.add(new RefreshResourcesOperation(selectedResources));
		op.add(new NotifyUnresolvedConflictOperation(mainOp));
		op.add(new ShowPostCommitErrorsOperation(mainOp));

		ExtensionsManager.getInstance().getCurrentCommitFactory().performAfterCommitTasks(op, mainOp, null, part);
	}

}
