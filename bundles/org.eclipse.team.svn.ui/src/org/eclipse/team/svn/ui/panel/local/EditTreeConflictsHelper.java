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

package org.eclipse.team.svn.ui.panel.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor.Action;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor.Operation;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor.Reason;
import org.eclipse.team.svn.core.connector.SVNConflictResolution;
import org.eclipse.team.svn.core.connector.SVNConflictVersion;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryReference;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.local.MarkResolvedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RevertOperation;
import org.eclipse.team.svn.core.operation.local.UpdateOperation;
import org.eclipse.team.svn.core.operation.local.refactor.DeleteResourceOperation;
import org.eclipse.team.svn.core.operation.remote.CopyRemoteResourcesToWcOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Helper class for <class>EditTreeConflictsPanel</class>
 * 
 * @author Igor Burilo
 */
public class EditTreeConflictsHelper {

	protected ILocalResource local;

	protected SVNConflictDescriptor treeConflict;

	public EditTreeConflictsHelper(ILocalResource local) {
		this.local = local;
		treeConflict = this.local.getTreeConflictDescriptor();
	}

	public String getOperationAsString() {
		String operation = SVNUIMessages.EditTreeConflictsPanel_None_Operation;
		switch (treeConflict.operation) {
			case UPDATE:
				operation = SVNUIMessages.EditTreeConflictsPanel_Update_Operation;
				break;
			case MERGE:
				operation = SVNUIMessages.EditTreeConflictsPanel_Merge_Operation;
				break;
			case SWITCHED:
				operation = SVNUIMessages.EditTreeConflictsPanel_Switch_Operation;
				break;
			case NONE:
				break;
		}
		return operation;
	}

	public String getReasonAsString() {
		String reason = ""; //$NON-NLS-1$
		switch (treeConflict.reason) {
			case ADDED:
				reason = SVNUIMessages.EditTreeConflictsPanel_Add_Reason;
				break;
			case DELETED:
				reason = SVNUIMessages.EditTreeConflictsPanel_Delete_Reason;
				break;
			case MISSING:
				reason = SVNUIMessages.EditTreeConflictsPanel_Missing_Reason;
				break;
			case MODIFIED:
				reason = SVNUIMessages.EditTreeConflictsPanel_Modified_Reason;
				break;
			case OBSTRUCTED:
				reason = SVNUIMessages.EditTreeConflictsPanel_Obstructed_Reason;
				break;
			case UNVERSIONED:
				reason = SVNUIMessages.EditTreeConflictsPanel_Unversioned_Reason;
				break;
			case MOVED_AWAY:
				reason = SVNUIMessages.EditTreeConflictsPanel_MovedAway_Reason;
				break;
			case MOVED_HERE:
				reason = SVNUIMessages.EditTreeConflictsPanel_MovedHere_Reason;
				break;
			case REPLACED:
				reason = SVNUIMessages.EditTreeConflictsPanel_Replaced_Reason;
				break;
		}
		return reason;
	}

	public String getActionAsString() {
		String action = SVNUIMessages.EditTreeConflictsPanel_Replace_Action;
		switch (treeConflict.action) {
			case ADD:
				action = SVNUIMessages.EditTreeConflictsPanel_Add_Action;
				break;
			case DELETE:
				action = SVNUIMessages.EditTreeConflictsPanel_Delete_Action;
				break;
			case MODIFY:
				action = SVNUIMessages.EditTreeConflictsPanel_Modify_Action;
				break;
			case REPLACE:
				break;
		}
		return action;
	}

	public String getTip() {
		String tip = null;
		/*
		 * Add it ?
		 * 
		 * [Merge: local missing incoming edit]
		 * 		In case alpha was renamed, rename alpha in the branch
		 * 		and run the merge again.
		 */
		if (treeConflict.action == Action.DELETE && treeConflict.reason == Reason.DELETED) {
			tip = SVNUIMessages.EditTreeConflictsPanel_Tip1;
		} else if (treeConflict.reason == Reason.MODIFIED && treeConflict.action == Action.DELETE) {
			tip = SVNUIMessages.EditTreeConflictsPanel_Tip2;
		} else if (treeConflict.reason == Reason.DELETED || treeConflict.action == Action.DELETE) {
			tip = SVNUIMessages.EditTreeConflictsPanel_Tip3;
		}
		return tip;
	}

	public String getSrcUrl(boolean isLeft) {
		SVNConflictVersion version = isLeft ? treeConflict.srcLeftVersion : treeConflict.srcRightVersion;
		return version == null
				? SVNRemoteStorage.instance().asRepositoryResource(local.getResource()).getUrl()
				: SVNUtility.normalizeURL(version.reposURL + "/" + version.pathInRepos); //$NON-NLS-1$
	}

	/*
	 * When we apply incoming changes we're trying not to make the conflict
	 * as resolved (e.g. because of another conflicts may exist or user after this may need
	 * to perform other additional steps), but it's not always possible, e.g.
	 * some resolutions require 'revert' operation (which clears conflict markers)
	 */
	public boolean isRemoteOperationResolveTheConflict() {
		return (treeConflict.action == Action.MODIFY || treeConflict.action == Action.REPLACE)
				&& (treeConflict.operation == Operation.UPDATE || treeConflict.operation == Operation.SWITCHED);
	}

	public IActionOperation getOperation(boolean isRemoteResolution, boolean isLocalResolution, boolean markAsMerged) {
		// FIXME just retest SVN::resolve() function behaviour when SVN 1.7 is available, then decide on removing useless parts if there is any
		CompositeOperation op = new CompositeOperation("", SVNUIMessages.class); //$NON-NLS-1$
		//used as parameter to operations, e.g. update, revert
		boolean isRecursive = true;
		if (isRemoteResolution) {
			if (treeConflict.action == Action.ADD) {
				addRemoteAddOperation(op, isRecursive);
			} else if (treeConflict.action == Action.DELETE) {
				addRemoteDeleteOperation(op);
			} else if (treeConflict.action == Action.MODIFY || treeConflict.action == Action.REPLACE) {
				addRemoteModifyOperation(op, isRecursive);
			}
		}

		//add resolved operation
		boolean isManual = !isRemoteResolution && !isLocalResolution;
		if (isLocalResolution
				|| markAsMerged && (isRemoteResolution && !isRemoteOperationResolveTheConflict() || isManual)) {
			op.add(getResolvedOperation(isRemoteResolution, isLocalResolution, markAsMerged));
		}

		if (op.isEmpty()) {
			return null;
		}

		//TODO refresh parent ?
		op.add(new RefreshResourcesOperation(new IResource[] { local.getResource() }));
		return op;
	}

	protected void addRemoteDeleteOperation(CompositeOperation op) {
		/*
		 * If item doesn't exist locally, i.e. missing or deleted then: resolved
		 * otherwise: delete + resolved
		 */
		if (local.getResource().exists()) {
			op.add(new DeleteResourceOperation(local.getResource()));
		}
	}

	protected void addRemoteModifyOperation(CompositeOperation op, boolean isRecursive) {
		/*
		 * For 'update' operation:
		 * 		revert + update
		 * 
		 * For 'merge' operation:
		 * 		[delete] + copy + resolved
		 * 		Result: R+
		 */

		IResource resource = local.getResource();
		if (treeConflict.operation == Operation.UPDATE || treeConflict.operation == Operation.SWITCHED) {
			IActionOperation resolveOp = new RevertOperation(new IResource[] { resource }, isRecursive);
			op.add(resolveOp);

			SVNRevision rev = treeConflict.operation == Operation.UPDATE
					? SVNRevision.HEAD
					: SVNRevision.fromNumber(treeConflict.srcRightVersion.pegRevision);
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			UpdateOperation updateOp = new UpdateOperation(new IResource[] { resource }, rev, ignoreExternals);
			op.add(updateOp, new IActionOperation[] { resolveOp });
		} else if (treeConflict.operation == Operation.MERGE) {
			DeleteResourceOperation deleteOp = null;
			if (resource.exists()) {
				deleteOp = new DeleteResourceOperation(resource);
				op.add(deleteOp);
			}

			IActionOperation copyOp = getCopyResourceOperation();
			op.add(copyOp, deleteOp == null ? null : new IActionOperation[] { deleteOp });
		}
	}

	protected void addRemoteAddOperation(CompositeOperation op, boolean isRecursive) {
		/*
		 * 'Update' operation: resolved + [delete[ + update
		 * 'Merge' operation:  resolved + [delete] + copy
		 * 
		 * For 'merge' operation we perform a copy
		 * For 'update' operation we perform update because file already exists in repository
		 */
		DeleteResourceOperation deleteOp = null;
		//if resource exists on file system we delete it
		if (local.getResource().exists()) {
			deleteOp = new DeleteResourceOperation(local.getResource());
			op.add(deleteOp);
		}

		IActionOperation updateOp;
		if (treeConflict.operation == Operation.MERGE) {
			updateOp = getCopyResourceOperation();
		} else {
			SVNRevision rev = treeConflict.operation == Operation.UPDATE
					? SVNRevision.HEAD
					: SVNRevision.fromNumber(treeConflict.srcRightVersion.pegRevision);
			boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
					SVNTeamUIPlugin.instance().getPreferenceStore(),
					SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
			updateOp = new UpdateOperation(new IResource[] { local.getResource() }, rev, ignoreExternals);
		}
		op.add(updateOp, deleteOp == null ? null : new IActionOperation[] { deleteOp });
	}

	protected IActionOperation getCopyResourceOperation() {
		String url = getSrcUrl(false);
		long pegRev = treeConflict.srcRightVersion.pegRevision;

		return new CopyRemoteResourcesToWcOperation(new SVNEntryReference(url, SVNRevision.fromNumber(pegRev)),
				local.getResource());
	}

	protected IActionOperation getResolvedOperation(boolean isRemoteResolution, boolean isLocalResolution,
			boolean markAsMerged) {
//		int resolution = isRemoteResolution ? SVNConflictResolution.CHOOSE_REMOTE_FULL : isLocalResolution ? SVNConflictResolution.CHOOSE_LOCAL_FULL : SVNConflictResolution.CHOOSE_MERGED;
		// FIXME there is really the Subversion issue why the "svn: Tree conflicts can only be resolved to 'working' state" error happens
		//  for reference please check this article: http://tortoisesvn.tigris.org/ds/viewMessage.do?dsForumId=757&viewType=browseAll&dsMessageId=2411874#messagefocus
		//  so, for now Subversive code will just resolve conflicts and the only call SVN API resolve() function to mark it as merged
		//  which means the only acceptable option is SVNConflictResolution.CHOOSE_MERGED
		SVNConflictResolution.Choice resolution = SVNConflictResolution.Choice.CHOOSE_MERGED;
		return new MarkResolvedOperation(new IResource[] { local.getResource() }, resolution, SVNDepth.INFINITY);
	}

	public IRepositoryResource getRepositoryResourceForHistory(boolean isLeft) {
		SVNConflictVersion version = isLeft ? treeConflict.srcLeftVersion : treeConflict.srcRightVersion;
		String url = getSrcUrl(isLeft);
		String repos = version.reposURL;
		repos = SVNUtility.normalizeURL(repos);
		SVNRevision revision = SVNRevision.fromNumber(treeConflict.srcRightVersion.pegRevision);

		//find the first parent of resource which exists in repository in end revision (srcRightSource's pegRevision)
		while (true) {
			IPath path = SVNUtility.createPathForSVNUrl(url);
			path = path.removeLastSegments(1);
			url = path.toString();

			IRepositoryLocation location = null;
			ISVNConnector proxy = null;
			try {
				SVNEntryRevisionReference ref = new SVNEntryRevisionReference(url, revision, revision);
				IRepositoryResource rr = SVNUtility.asRepositoryResource(url, true);
				location = rr.getRepositoryLocation();
				proxy = location.acquireSVNProxy();
				SVNUtility.info(proxy, ref, SVNDepth.EMPTY, new SVNNullProgressMonitor());
				break;
			} catch (SVNConnectorException e) {
				if (repos.equals(url)) {
					break;
				}
			} finally {
				if (location != null && proxy != null) {
					location.releaseSVNProxy(proxy);
				}
			}
		}

		IRepositoryResource repositoryResource = SVNUtility.asRepositoryResource(url, true);
		repositoryResource.setPegRevision(revision);
		repositoryResource.setSelectedRevision(revision);
		return repositoryResource;
	}
}
