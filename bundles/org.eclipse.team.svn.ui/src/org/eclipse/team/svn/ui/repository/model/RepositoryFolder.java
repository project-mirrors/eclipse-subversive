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

package org.eclipse.team.svn.ui.repository.model;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.remote.GetRemoteFolderChildrenOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.DefaultOperationWrapperFactory;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Repository folder node representation
 * 
 * @author Alexander Gurov
 */
public class RepositoryFolder extends RepositoryResource implements IParentTreeNode {
	protected GetRemoteFolderChildrenOperation childrenOp;

	protected Object[] wrappedChildren;

	public RepositoryFolder(RepositoryResource parent, IRepositoryResource resource) {
		super(parent, resource);
	}

	@Override
	public void refresh() {
		childrenOp = null;
		wrappedChildren = null;
		super.refresh();
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	public Object[] getChildren(Object o) {
		final IRepositoryContainer container = (IRepositoryContainer) resource;

		if (wrappedChildren != null) {
			return wrappedChildren;
		}

		if (childrenOp != null) {
			Object[] retVal = RepositoryFolder.wrapChildren(this, childrenOp.getChildren(), childrenOp);
			if (retVal != null) {
				wrappedChildren = retVal;
			} else if (childrenOp.getExecutionState() != IActionOperation.ERROR) {
				retVal = new Object[] { new RepositoryPending(this) };
			} else {
				retVal = wrappedChildren = new Object[] { new RepositoryError(childrenOp.getStatus()) };
			}
			return retVal;
		}
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
		childrenOp = new GetRemoteFolderChildrenOperation(container,
				SVNTeamPreferences.getRepositoryBoolean(store, SVNTeamPreferences.REPOSITORY_SHOW_EXTERNALS_NAME),
				SVNTeamPreferences.getBehaviourBoolean(store,
						SVNTeamPreferences.BEHAVIOUR_CASE_INSENSITIVE_TABLE_SORTING_NAME));

		CompositeOperation op = new CompositeOperation(childrenOp.getId(), childrenOp.getMessagesClass());
		op.add(childrenOp);
		op.add(getRefreshOperation(getViewer()));

		UIMonitorUtility.doTaskScheduled(op, new DefaultOperationWrapperFactory() {
			@Override
			public IActionOperation getLogged(IActionOperation operation) {
				return new LoggedOperation(operation);
			}
		});

		return new Object[] { new RepositoryPending(this) };
	}

	public Object[] peekChildren(Object o) {
		if (childrenOp == null) {
			return getChildren(o);
		}
		Object[] retVal = RepositoryFolder.wrapChildren(this, childrenOp.getChildren(), childrenOp);
		return retVal == null
				? new Object[] { childrenOp.getExecutionState() != IActionOperation.ERROR
						? (Object) new RepositoryPending(this)
						: new RepositoryError(childrenOp.getStatus()) }
				: retVal;
	}

	public static RepositoryResource[] wrapChildren(RepositoryResource parent, IRepositoryResource[] resources,
			GetRemoteFolderChildrenOperation childrenOp) {
		if (resources == null) {
			return null;
		}
		RepositoryResource[] wrappers = new RepositoryResource[resources.length];
		for (int i = 0; i < resources.length; i++) {
			String externalsName = childrenOp != null ? childrenOp.getExternalsName(i) : null;
			wrappers[i] = RepositoryFolder.wrapChild(parent, resources[i], externalsName);
		}
		return wrappers;
	}

	public static RepositoryResource wrapChild(RepositoryResource parent, IRepositoryResource resource,
			String externalsName) {
		RepositoryResource retVal = null;
		if (resource instanceof IRepositoryRoot && externalsName == null) {
			IRepositoryRoot tmp = (IRepositoryRoot) resource;
			switch (tmp.getKind()) {
				case IRepositoryRoot.KIND_TRUNK: {
					retVal = new RepositoryTrunk(parent, tmp);
					break;
				}
				case IRepositoryRoot.KIND_BRANCHES: {
					retVal = new RepositoryBranches(parent, tmp);
					break;
				}
				case IRepositoryRoot.KIND_TAGS: {
					retVal = new RepositoryTags(parent, tmp);
					break;
				}
				default: {
					retVal = new RepositoryRoot(parent, tmp);
					break;
				}
			}
		} else {
			retVal = resource instanceof IRepositoryFile
					? (RepositoryResource) new RepositoryFile(parent, resource)
					: new RepositoryFolder(parent, resource);
			if (externalsName != null) {
				retVal.setLabel(externalsName);
				retVal.setExternals(true);
			}
		}
		return retVal;
	}

	@Override
	protected ImageDescriptor getImageDescriptorImpl() {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}

}
