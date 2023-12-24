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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.mapping;

import java.util.HashSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;
import org.eclipse.team.internal.ui.mapping.ResourceModelLabelProvider;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;

public class SVNChangeSetLabelProvider extends ResourceModelLabelProvider {

	private Image changeSetImage;

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ActiveChangeSet
				&& SVNTeamPlugin.instance().getModelChangeSetManager().isDefault((ActiveChangeSet) element)) {
			return super.getText(element) + " " + SVNUIMessages.ChangeSet_DefaultDecoration; //$NON-NLS-1$
		}
		return super.getText(element);
	}

	@Override
	protected String getDelegateText(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof ChangeSet) {
			ChangeSet set = (ChangeSet) element;
			return set.getName();
		}
		return super.getDelegateText(elementOrPath);
	}

	@Override
	protected Image getDelegateImage(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof ChangeSet) {
			return getChangeSetImage();
		}
		return super.getDelegateImage(elementOrPath);
	}

	private Image getChangeSetImage() {
		if (changeSetImage == null) {
			changeSetImage = SVNTeamUIPlugin.instance()
					.getImageDescriptor("icons/objects/changeset.gif") //$NON-NLS-1$
					.createImage();
		}
		return changeSetImage;
	}

	@Override
	public void dispose() {
		if (changeSetImage != null) {
			changeSetImage.dispose();
		}
		super.dispose();
	}

	@Override
	protected boolean isBusy(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) element;
			IResource[] resources = dcs.getResources();
			for (IResource resource : resources) {
				if (getContext().getDiffTree().getProperty(resource.getFullPath(), IDiffTree.P_BUSY_HINT)) {
					return true;
				}
			}
			return false;
		}
		return super.isBusy(elementOrPath);
	}

	@Override
	protected boolean hasDecendantConflicts(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) element;
			IResource[] resources = dcs.getResources();
			for (IResource resource : resources) {
				if (getContext().getDiffTree()
						.getProperty(resource.getFullPath(), IDiffTree.P_HAS_DESCENDANT_CONFLICTS)) {
					return true;
				}
			}
			return false;
		}
		if (elementOrPath instanceof TreePath && element instanceof IResource) {
			DiffChangeSet set = internalGetChangeSet(elementOrPath);
			if (set != null) {
				ResourceTraversal[] traversals = getTraversalCalculator().getTraversals(set, (TreePath) elementOrPath);
				return getContext().getDiffTree().hasMatchingDiffs(traversals, CONFLICT_FILTER);
			}
		}
		return super.hasDecendantConflicts(elementOrPath);
	}

	private DiffChangeSet internalGetChangeSet(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			Object o = tp.getFirstSegment();
			if (o instanceof DiffChangeSet) {
				return (DiffChangeSet) o;
			}
		}
		return null;
	}

	@Override
	protected int getMarkerSeverity(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) element;
			HashSet<IProject> projects = new HashSet<>();
			IResource[] resources = dcs.getResources();
			int severity = -1;
			for (IResource resource : resources) {
				IProject project = resource.getProject();
				if (!projects.contains(project)) {
					projects.add(project);
					int next = super.getMarkerSeverity(project);
					if (next == IMarker.SEVERITY_ERROR) {
						return IMarker.SEVERITY_ERROR;
					}
					if (next == IMarker.SEVERITY_WARNING) {
						severity = next;
					}
				}
			}
			return severity;
		}
		return super.getMarkerSeverity(elementOrPath);
	}

	@Override
	protected void updateLabels(Object[] elements) {
		super.updateLabels(addSetsContainingElements(elements));
	}

	private Object[] addSetsContainingElements(Object[] elements) {
		HashSet<Object> result = new HashSet<>();
		for (Object object : elements) {
			result.add(object);
			if (object instanceof IProject) {
				IProject project = (IProject) object;
				ChangeSet[] sets = getSetsContaing(project);
				for (ChangeSet set : sets) {
					result.add(set);
				}
			}
		}
		return result.toArray();
	}

	private ChangeSet[] getSetsContaing(IProject project) {
		return getContentProvider().getSetsShowingPropogatedStateFrom(project.getFullPath());
	}

	private SVNChangeSetContentProvider getContentProvider() {
		return (SVNChangeSetContentProvider) getExtensionSite().getExtension().getContentProvider();
	}

	private Object internalGetElement(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			return tp.getLastSegment();
		}
		return elementOrPath;
	}

	@Override
	public Font getFont(Object element) {
		element = internalGetElement(element);
		if (element instanceof ActiveChangeSet && isDefaultActiveSet((ActiveChangeSet) element)) {
			return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		}
		return super.getFont(element);
	}

	private boolean isDefaultActiveSet(ActiveChangeSet set) {
		ChangeSetCapability changeSetCapability = getContentProvider().getChangeSetCapability();
		if (changeSetCapability != null) {
			ActiveChangeSetManager activeChangeSetManager = changeSetCapability.getActiveChangeSetManager();
			if (activeChangeSetManager != null) {
				return activeChangeSetManager.isDefault(set);
			}
		}
		return false;
	}
}
