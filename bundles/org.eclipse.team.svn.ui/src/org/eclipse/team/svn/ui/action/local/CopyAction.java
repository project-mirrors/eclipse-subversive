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

package org.eclipse.team.svn.ui.action.local;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.AddToSVNOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.refactor.CopyResourceOperation;
import org.eclipse.team.svn.core.operation.local.refactor.CopyResourceWithHistoryOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.local.ContainerSelectionPanel;

/**
 * Copy local resource from one location to another with history if resource is versioned
 * 
 * @author Sergiy Logvin
 */
public class CopyAction extends AbstractWorkingCopyAction {

	public CopyAction() {
	}

	@Override
	public void runImpl(IAction action) {
		IResource[] AllResources = this.getSelectedResources(CopyAction.SF_EXCLUDE_DELETED_AND_PROJECTS);

		HashMap<String, IResource> resourcesWithoutEqualsNames = new HashMap<>();
		HashSet<String> conflictedResources = excludeResourcesWithEqualNames(resourcesWithoutEqualsNames, AllResources);

		if (resourcesWithoutEqualsNames.isEmpty()) {
			MessageDialog dialog = new MessageDialog(getShell(), SVNUIMessages.CopyAction_Conflict_Title, null,
					SVNUIMessages.CopyAction_Conflict_Message, MessageDialog.WARNING,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			dialog.open();
			return;
		}
		//make new filtered resources list without resources with equal names
		final IResource[] resources = resourcesWithoutEqualsNames.values()
				.toArray(new IResource[resourcesWithoutEqualsNames.size()]);

		ContainerSelectionPanel panel = new ContainerSelectionPanel(resources, conflictedResources);
		DefaultDialog dialog = new DefaultDialog(getShell(), panel);
		if (dialog.open() == 0) {
			IPath path = panel.getSelectedPath();
			boolean saveHistory = panel.isCopyWithHistorySelected();

			CompositeOperation op = new CompositeOperation("Operation_CopyResources", SVNUIMessages.class); //$NON-NLS-1$

			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IActionOperation addOp = null;
			if (panel.isOverrideResourceName()) {
				path = path.append(panel.getOverridenName());
				if (resources.length == 1) {
					IResource destination = resources[0].getType() == IResource.FILE
							? (IResource) root.getFile(path)
							: root.getFolder(path);
					String dirPath = FileUtility.getWorkingCopyPath(destination.getParent());
					if (dirPath != null) {
						new File(dirPath).mkdirs();
					}
					boolean[] checkSave = { saveHistory };
					IActionOperation copyOp = getCopyOperation(resources[0], checkSave, destination);
					if (checkSave[0]) {
						IResource[] parents = FileUtility.getOperableParents(new IResource[] { destination },
								IStateFilter.SF_UNVERSIONED, true);
						if (parents.length > 0) {
							addOp = new AddToSVNOperation(parents);
						}
					}
					if (addOp != null) {
						op.add(addOp);
						op.add(copyOp, new IActionOperation[] { addOp });
					} else {
						op.add(copyOp);
					}
				} else {
					IResource destination = root.getFolder(path);
					String dirPath = FileUtility.getWorkingCopyPath(destination);
					if (dirPath != null) {
						new File(dirPath).mkdirs();
					}
					IResource[] parents = FileUtility.addOperableParents(new IResource[] { destination },
							IStateFilter.SF_UNVERSIONED, true);
					if (parents.length > 0) {
						addOp = new AddToSVNOperation(parents);
						op.add(addOp);
					}
				}
			}

			if (resources.length > 1 || !panel.isOverrideResourceName()) {
				for (IResource element : resources) {
					IPath tPath = path.append(element.getName());
					IResource target = element.getType() == IResource.FILE
							? (IResource) root.getFile(tPath)
							: root.getFolder(tPath);
					IActionOperation copyOp = getCopyOperation(element, new boolean[] { saveHistory }, target);
					if (addOp != null) {
						op.add(copyOp, new IActionOperation[] { addOp });
					} else {
						op.add(copyOp);
					}
				}
			}

			op.add(new RefreshResourcesOperation(new IResource[] { root.findMember(panel.getSelectedPath()) }));

			runScheduled(op);
		}
	}

	protected IActionOperation getCopyOperation(IResource resource, boolean[] saveHistory, IResource destination) {
		if (saveHistory[0]) {
			CopyResourceWithHistoryOperation copyOp = new CopyResourceWithHistoryOperation(resource, destination);
			if (copyOp.isAllowed()) {
				return copyOp;
			}
			saveHistory[0] = false;
		}
		return new CopyResourceOperation(resource, destination);
	}

	@Override
	public boolean isEnabled() {
		return checkForResourcesPresence(CopyAction.SF_EXCLUDE_DELETED_AND_PROJECTS);
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

	protected static final IStateFilter SF_EXCLUDE_DELETED_AND_PROJECTS = new IStateFilter.AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			if (!IStateFilter.SF_LINKED.accept(resource, state, mask)
					&& !IStateFilter.SF_OBSTRUCTED.accept(resource, state, mask)) {
				return (resource instanceof IFolder || resource instanceof IFile) && state != IStateFilter.ST_DELETED
						&& state != IStateFilter.ST_MISSING;
			}
			return false;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state != IStateFilter.ST_LINKED && state != IStateFilter.ST_OBSTRUCTED;
		}
	};

	protected HashSet<String> excludeResourcesWithEqualNames(HashMap<String, IResource> map, IResource[] resources) {
		HashSet<String> conflicts = new HashSet<>();
		for (IResource element : resources) {
			if (map.containsKey(element.getName())) {
				conflicts.add(element.getName());
			} else {
				map.put(element.getName(), element);
			}
		}
		//delete all conflicts from resources set
		for (String element : conflicts) {
			map.remove(element);
		}
		return conflicts;
	}

}
