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

package org.eclipse.team.svn.ui.operation;

import java.util.HashSet;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.SVNResourceRuleFactory;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ExternalProgramParameters;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.DetectExternalCompareOperationHelper;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.ExternalCompareOperationHelper;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.compare.ComparePanel;
import org.eclipse.team.svn.ui.compare.ConflictingFileEditorInput;
import org.eclipse.team.svn.ui.compare.PropertyCompareInput;
import org.eclipse.team.svn.ui.compare.ThreeWayPropertyCompareInput;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.preferences.SVNTeamDiffViewerPage;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Show conflict editor operation implementation
 * 
 * @author Alexander Gurov
 */
public class ShowConflictEditorOperation extends AbstractWorkingCopyOperation {

	protected boolean showInDialog;

	public ShowConflictEditorOperation(IResource[] resources, boolean showInDialog) {
		super("Operation_ShowConflictEditor", SVNUIMessages.class, resources); //$NON-NLS-1$
		this.showInDialog = showInDialog;
	}

	public ShowConflictEditorOperation(IResourceProvider provider, boolean showInDialog) {
		super("Operation_ShowConflictEditor", SVNUIMessages.class, provider); //$NON-NLS-1$
		this.showInDialog = showInDialog;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		ISchedulingRule rule = super.getSchedulingRule();
		if (rule instanceof IWorkspaceRoot) {
			return rule;
		}
		IResource[] resources = operableData();
		HashSet<ISchedulingRule> ruleSet = new HashSet<>();
		for (IResource element : resources) {
			ruleSet.add(SVNResourceRuleFactory.INSTANCE.refreshRule(element.getParent()));
		}
		return new MultiRule(ruleSet.toArray(new IResource[ruleSet.size()]));
	}

	@Override
	public int getOperationWeight() {
		return 0;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] conflictingResources = operableData();

		for (int i = 0; i < conflictingResources.length && !monitor.isCanceled(); i++) {
			final IResource current = conflictingResources[i];
			this.protectStep(monitor1 -> ShowConflictEditorOperation.this.showEditorFor(current, monitor1), monitor, conflictingResources.length);
		}
	}

	protected void showEditorFor(final IResource resource, IProgressMonitor monitor) throws Exception {
		IRemoteStorage storage = SVNRemoteStorage.instance();

		IRepositoryLocation location = storage.getRepositoryLocation(resource);
		ISVNConnector proxy = location.acquireSVNProxy();
		SVNChangeStatus[] status;

		try {
			status = SVNUtility.status(proxy, FileUtility.getWorkingCopyPath(resource), SVNDepth.EMPTY,
					ISVNConnector.Options.NONE, new SVNNullProgressMonitor());
		} finally {
			location.releaseSVNProxy(proxy);
		}

		// open property editor if required
		if (status.length == 1 && status[0].propStatus == SVNEntryStatus.Kind.CONFLICTED) {
			CompareConfiguration cc = new CompareConfiguration();
			cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, Boolean.TRUE);
			ILocalResource baseResource = SVNRemoteStorage.instance().asLocalResource(resource);
			IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(resource);
			SVNEntryRevisionReference baseReference = new SVNEntryRevisionReference(
					FileUtility.getWorkingCopyPath(resource), null, SVNRevision.BASE);
			final PropertyCompareInput compare = new ThreeWayPropertyCompareInput(cc, resource, null, baseReference,
					remote.getRepositoryLocation(), baseResource.getRevision());
			compare.run(monitor);
			UIMonitorUtility.getDisplay().syncExec(() -> {
				if (showInDialog) {
					ComparePanel panel = new ComparePanel(compare, resource);
					DefaultDialog dlg = new DefaultDialog(UIMonitorUtility.getShell(), panel);
					dlg.open();
					//CompareUI.openCompareDialog(compare);
				} else {
					CompareUI.openCompareEditor(compare);
				}
			});
		}

		// open compare editor if required
		if (resource.getType() == IResource.FILE && status.length == 1 && status[0].hasConflict
				&& status[0].treeConflicts != null && status[0].treeConflicts[0].remotePath != null
				&& status[0].treeConflicts[0].basePath != null) {
			IContainer parent = resource.getParent();
			parent.refreshLocal(IResource.DEPTH_ONE, monitor);
			/*
			 * If Subversion considers the file to be unmergeable, then the .mine file isn't
			 * created, since it would be identical to the working file.
			 */
			IFile local = null;
			Path tPath = null;
			if (status[0].treeConflicts[0].localPath != null && !"".equals(status[0].treeConflicts[0].localPath)) { //$NON-NLS-1$
				tPath = new Path(status[0].treeConflicts[0].localPath);
				local = parent.getFile(tPath.removeFirstSegments(tPath.segmentCount() - 1));
				if (!local.exists()) {
					local = null;
				}
			}
			local = local == null ? (IFile) resource : local;

			tPath = new Path(status[0].treeConflicts[0].remotePath);
			IFile remote = parent.getFile(tPath.removeFirstSegments(tPath.segmentCount() - 1));
			tPath = new Path(status[0].treeConflicts[0].basePath);
			IFile ancestor = parent.getFile(tPath.removeFirstSegments(tPath.segmentCount() - 1));

			//detect compare editor
			DetectExternalCompareOperationHelper detectCompareEditorHelper = new DetectExternalCompareOperationHelper(
					resource, SVNTeamDiffViewerPage.loadDiffViewerSettings(), false);
			detectCompareEditorHelper.execute(monitor);
			ExternalProgramParameters externalProgramParams = detectCompareEditorHelper.getExternalProgramParameters();
			if (externalProgramParams != null) {
				openExternalEditor((IFile) resource, local, remote, ancestor, externalProgramParams, monitor);
			} else {
				openEclipseEditor((IFile) resource, local, remote, ancestor, monitor);
			}
		}
	}

	protected void openExternalEditor(IFile target, IFile left, IFile right, IFile ancestor,
			ExternalProgramParameters externalProgramParams, IProgressMonitor monitor) throws Exception {
		String targetFile = FileUtility.getWorkingCopyPath(target);
		String oldFile = FileUtility.getWorkingCopyPath(ancestor);
		String workingFile = FileUtility.getWorkingCopyPath(left);
		String newFile = FileUtility.getWorkingCopyPath(right);

		ExternalCompareOperationHelper compareRunner = new ExternalCompareOperationHelper(oldFile, workingFile, newFile,
				targetFile, externalProgramParams, false);
		compareRunner.execute(monitor);
	}

	protected void openEclipseEditor(final IFile target, IFile left, IFile right, IFile ancestor,
			IProgressMonitor monitor) throws Exception {
		CompareConfiguration cc = new CompareConfiguration();
		cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, Boolean.TRUE);
		final ConflictingFileEditorInput compare = new ConflictingFileEditorInput(cc, target, left, right, ancestor);
		compare.run(monitor);
		UIMonitorUtility.getDisplay().syncExec(() -> {
			if (showInDialog) {
				ComparePanel panel = new ComparePanel(compare, target);
				DefaultDialog dlg = new DefaultDialog(UIMonitorUtility.getShell(), panel);
				dlg.open();
				//CompareUI.openCompareDialog(compare);
			} else {
				CompareUI.openCompareEditor(compare);
			}
			compare.setDirty(true);
		});
	}

}
