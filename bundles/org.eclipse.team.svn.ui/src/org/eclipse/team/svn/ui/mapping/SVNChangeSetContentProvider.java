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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.BatchingChangeSetManager;
import org.eclipse.team.internal.core.subscribers.BatchingChangeSetManager.CollectorChangeEvent;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;
import org.eclipse.team.internal.core.subscribers.IChangeSetChangeListener;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.mapping.ResourceModelContentProvider;
import org.eclipse.team.internal.ui.mapping.ResourceModelLabelProvider;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.svn.core.mapping.SVNActiveChangeSet;
import org.eclipse.team.svn.core.mapping.SVNChangeSetModelProvider;
import org.eclipse.team.svn.core.mapping.SVNIncomingChangeSet;
import org.eclipse.team.svn.core.mapping.SVNUnassignedChangeSet;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.INavigatorSorterService;

public class SVNChangeSetContentProvider extends ResourceModelContentProvider implements ITreePathContentProvider {

	private final class CollectorListener
	implements IChangeSetChangeListener, BatchingChangeSetManager.IChangeSetCollectorChangeListener {

		@Override
		public void setAdded(final ChangeSet set) {
			if (set instanceof ActiveChangeSet) {
				final DiffChangeSet unassigned = getUnassignedSet();
				unassigned.remove(set.getResources());
				if (SVNChangeSetContentProvider.this.isVisibleInMode(set)) {
					UIMonitorUtility.getDisplay().syncExec(() -> {
						AbstractTreeViewer viewer = (AbstractTreeViewer) getViewer();
						viewer.getControl().setRedraw(false);
						try {
							viewer.add(viewer.getInput(), set);
							if (unassigned.isEmpty()) {
								viewer.remove(unassigned);
							}
						} finally {
							viewer.getControl().setRedraw(true);
						}
					});
				}
			}
		}

		@Override
		public void defaultSetChanged(final ChangeSet previousDefault, final ChangeSet set) {
			if (SVNChangeSetContentProvider.this.isVisibleInMode(set)
					|| SVNChangeSetContentProvider.this.isVisibleInMode(previousDefault)) {
				UIMonitorUtility.getDisplay().syncExec(() -> {
					AbstractTreeViewer viewer = (AbstractTreeViewer) getViewer();
					viewer.getControl().setRedraw(false);
					try {
						if (previousDefault != null) {
							viewer.refresh(previousDefault, true);
						}
						if (set != null) {
							viewer.refresh(set, true);
						}
					} finally {
						viewer.getControl().setRedraw(true);
					}
				});
			}
		}

		@Override
		public void setRemoved(final ChangeSet set) {
			if (set instanceof ActiveChangeSet) {
				handleSetRemoval(set);
				if (SVNChangeSetContentProvider.this.isVisibleInMode(set)) {
					UIMonitorUtility.getDisplay().syncExec(() -> {
						AbstractTreeViewer viewer = (AbstractTreeViewer) getViewer();
						viewer.getControl().setRedraw(false);
						try {
							viewer.remove(set);
							DiffChangeSet unassigned = getUnassignedSet();
							if (!unassigned.isEmpty()) {
								viewer.add(viewer.getInput(), unassigned);
							}
						} finally {
							viewer.getControl().setRedraw(true);
						}
					});
				}
			}
		}

		private void handleSetRemoval(final ChangeSet set) {
			IResource[] resources = set.getResources();
			ArrayList<IDiff> toAdd = new ArrayList<>();
			for (IResource element : resources) {
				IDiff diff = getContext().getDiffTree().getDiff(element);
				//if active change set deleted, then we can freely add its resources to unassigned change set
				if (diff != null) {
					toAdd.add(diff);
				}
			}
			getUnassignedSet().add(toAdd.toArray(new IDiff[toAdd.size()]));
		}

		@Override
		public void nameChanged(final ChangeSet set) {
			if (SVNChangeSetContentProvider.this.isVisibleInMode(set)) {
				UIMonitorUtility.getDisplay().syncExec(() -> {
					AbstractTreeViewer viewer = (AbstractTreeViewer) getViewer();
					viewer.update(set, null);
				});
			}
		}

		@Override
		public void resourcesChanged(final ChangeSet set, final IPath[] paths) {
			if (set instanceof ActiveChangeSet) {
				handleSetChange(set, paths);
				if (SVNChangeSetContentProvider.this.isVisibleInMode(set)) {
					UIMonitorUtility.getDisplay().syncExec(() -> {
						AbstractTreeViewer viewer = (AbstractTreeViewer) getViewer();
						viewer.getControl().setRedraw(false);
						try {
							if (SVNChangeSetContentProvider.this.hasChildrenInContext(set)) {
								if (getVisibleSetsInViewer().contains(set)) {
									viewer.refresh(set, true);
								} else {
									viewer.add(viewer.getInput(), set);
								}
							} else if (!(set instanceof SVNActiveChangeSet)
									|| !((SVNActiveChangeSet) set).isManagedExternally()) {
								viewer.remove(set);
							}
							DiffChangeSet unassigned = getUnassignedSet();
							if (!unassigned.isEmpty()) {
								viewer.add(viewer.getInput(), unassigned);
							} else {
								viewer.remove(unassigned);
							}
						} finally {
							viewer.getControl().setRedraw(true);
						}
					});
				}
			}
		}

		private void handleSetChange(final ChangeSet set, final IPath[] paths) {
			try {
				getTheRest().beginInput();
				for (IPath path : paths) {
					if (((DiffChangeSet) set).contains(path)) {
						IDiff diff = ((DiffChangeSet) set).getDiffTree().getDiff(path);
						if (diff != null) {
							getTheRest().remove(ResourceDiffTree.getResourceFor(diff));
						}
					} else {
						IDiff diff = getContext().getDiffTree().getDiff(path);
						if (diff != null && canAddToUnnassignedChangeSet(diff)) {
							getTheRest().add(diff);
						}
					}
				}
			} finally {
				getTheRest().endInput(null);
			}
		}

		@Override
		public void changeSetChanges(final CollectorChangeEvent event, IProgressMonitor monitor) {
			ChangeSet[] addedSets = event.getAddedSets();
			final ChangeSet[] visibleAddedSets = getVisibleSets(addedSets);
			ChangeSet[] removedSets = event.getRemovedSets();
			final ChangeSet[] visibleRemovedSets = getVisibleSets(removedSets);
			ChangeSet[] changedSets = event.getChangedSets();
			final ChangeSet[] visibleChangedSets = getVisibleSets(changedSets);
			final DiffChangeSet unassigned = getUnassignedSet();
			try {
				getTheRest().beginInput();
				for (ChangeSet addedSet : addedSets) {
					unassigned.remove(addedSet.getResources());
				}
				if (removedSets.length > 0) {
					addAllUnassignedToUnassignedSet();
				}
				for (ChangeSet set : changedSets) {
					IPath[] paths = event.getChangesFor(set);
					if (event.getSource().contains(set)) {
						handleSetChange(set, paths);
					} else {
						try {
							getTheRest().beginInput();
							for (IPath path : paths) {
								IDiff diff = getContext().getDiffTree().getDiff(path);
								if (diff != null && canAddToUnnassignedChangeSet(diff)) {
									getTheRest().add(diff);
								}
							}
						} finally {
							getTheRest().endInput(null);
						}
					}
				}
			} finally {
				getTheRest().endInput(monitor);
			}
			if (visibleAddedSets.length > 0 || visibleRemovedSets.length > 0 || visibleChangedSets.length > 0) {
				UIMonitorUtility.getDisplay().syncExec(() -> {
					AbstractTreeViewer viewer = (AbstractTreeViewer) getViewer();
					try {
						viewer.getControl().setRedraw(false);
						if (visibleAddedSets.length > 0) {
							viewer.add(viewer.getInput(), visibleAddedSets);
						}
						if (visibleRemovedSets.length > 0) {
							for (ChangeSet visibleRemovedSet : visibleRemovedSets) {
								if (!(visibleRemovedSet instanceof SVNActiveChangeSet)
										|| !((SVNActiveChangeSet) visibleRemovedSet).isManagedExternally()) {
									viewer.remove(visibleRemovedSet);
								}
							}
						}
						for (ChangeSet visibleChangedSet : visibleChangedSets) {
							viewer.refresh(visibleChangedSet, true);
						}
						if (!unassigned.isEmpty()) {
							viewer.add(viewer.getInput(), unassigned);
						} else {
							viewer.remove(unassigned);
						}
					} finally {
						viewer.getControl().setRedraw(true);
					}
				});
			}
		}

		private ChangeSet[] getVisibleSets(ChangeSet[] sets) {
			ArrayList<ChangeSet> result = new ArrayList<>();
			for (ChangeSet set : sets) {
				if (SVNChangeSetContentProvider.this.isVisibleInMode(set)) {
					result.add(set);
				}
			}
			return result.toArray(new ChangeSet[result.size()]);
		}
	}

	private DiffChangeSet unassignedDiffs;

	private SVNIncomingChangeSetCollector incomingCollector;

	private boolean collectorInitialized;

	private IChangeSetChangeListener collectorListener = new CollectorListener();

	@Override
	protected String getModelProviderId() {
		return SVNChangeSetModelProvider.ID;
	}

	protected boolean isVisibleInMode(ChangeSet set) {
		if (getViewer().getInput() instanceof SVNChangeSetModelProvider) {
			if (set instanceof ActiveChangeSet || set instanceof SVNUnassignedChangeSet) {
				return getConfiguration().getMode() != ISynchronizePageConfiguration.INCOMING_MODE;
			}
			if (set instanceof DiffChangeSet) {
				return getConfiguration().getMode() != ISynchronizePageConfiguration.OUTGOING_MODE;
			}
		}
		return false;
	}

	protected boolean isEnabled() {
		final Object input = getViewer().getInput();
		return input instanceof SVNChangeSetModelProvider;
	}

	@Override
	public Object[] getElements(Object parent) {
		if (parent instanceof ISynchronizationContext) {
			//FIXME Does change set model conflict with other models? Remove this override if no.
			return new Object[0];
		}
		if (parent == getModelProvider()) {
			return getRootElements();
		}
		return super.getElements(parent);
	}

	private Object[] getRootElements() {
		if (!collectorInitialized) {
			initializeCheckedInChangeSetCollector(getChangeSetCapability());
			collectorInitialized = true;
		}
		ArrayList<ChangeSet> result = new ArrayList<>();
		ChangeSet[] sets = getAllSets();
		for (ChangeSet set : sets) {
			if (this.hasChildren(TreePath.EMPTY.createChildPath(set))
					|| set instanceof ActiveChangeSet && ((ActiveChangeSet) set).isUserCreated()) {
				result.add(set);
			}
		}
		if (!getUnassignedSet().isEmpty() && this.hasChildren(TreePath.EMPTY.createChildPath(getUnassignedSet()))) {
			result.add(getUnassignedSet());
		}
		return result.toArray();
	}

	private synchronized DiffChangeSet getUnassignedSet() {
		if (unassignedDiffs == null) {
			unassignedDiffs = new SVNUnassignedChangeSet(SVNUIMessages.ChangeSetModel_UnassignedChangeSetTitle);
			addAllUnassignedToUnassignedSet();
		}
		return unassignedDiffs;
	}

	private void addAllUnassignedToUnassignedSet() {
		IResourceDiffTree allChanges = getContext().getDiffTree();
		final ArrayList<IDiff> diffs = new ArrayList<>();
		allChanges.accept(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), diff -> {
			if (canAddToUnnassignedChangeSet(diff)) {
				diffs.add(diff);
			}
			return true;
		}, IResource.DEPTH_INFINITE);
		unassignedDiffs.add(diffs.toArray(new IDiff[diffs.size()]));
	}

	private ResourceDiffTree getTheRest() {
		return (ResourceDiffTree) getUnassignedSet().getDiffTree();
	}

	protected boolean isContainedInSet(IDiff diff, ChangeSet[] sets) {
		for (ChangeSet set : sets) {
			if (set.contains(ResourceDiffTree.getResourceFor(diff))) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected ResourceTraversal[] getTraversals(ISynchronizationContext context, Object object) {
		if (object instanceof ChangeSet) {
			ChangeSet set = (ChangeSet) object;
			IResource[] resources = set.getResources();
			return new ResourceTraversal[] { new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE) };
		}
		return super.getTraversals(context, object);
	}

	@Override
	public Object[] getChildren(TreePath parentPath) {
		if (!isEnabled()) {
			return new Object[0];
		}
		if (parentPath.getSegmentCount() == 0) {
			return getRootElements();
		}
		Object first = parentPath.getFirstSegment();
		if (!this.isVisibleInMode(first)) {
			return new Object[0];
		}
		IResourceDiffTree diffTree;
		Object parent = parentPath.getLastSegment();
		if (first instanceof DiffChangeSet) {
			DiffChangeSet set = (DiffChangeSet) first;
			diffTree = set.getDiffTree();
			if (parent instanceof DiffChangeSet) {
				parent = getModelRoot();
			}
		} else {
			return new Object[0];
		}
		Object[] children = this.getChildren(parent);
		HashSet<Object> result = new HashSet<>();
		for (Object child : children) {
			if (this.isVisible(child, diffTree)) {
				result.add(child);
			}
		}
		return result.toArray();
	}

	private boolean isVisibleInMode(Object first) {
		if (first instanceof ChangeSet) {
			ChangeSet changeSet = (ChangeSet) first;
			int mode = getConfiguration().getMode();
			switch (mode) {
				case ISynchronizePageConfiguration.BOTH_MODE:
					return true;
				case ISynchronizePageConfiguration.CONFLICTING_MODE:
					return containsConflicts(changeSet);
				case ISynchronizePageConfiguration.INCOMING_MODE:
					return changeSet instanceof SVNIncomingChangeSet
							|| isUnassignedSet(changeSet) && hasIncomingChanges(changeSet);
				case ISynchronizePageConfiguration.OUTGOING_MODE:
					return changeSet instanceof ActiveChangeSet || hasConflicts(changeSet)
							|| isUnassignedSet(changeSet) && hasOutgoingChanges(changeSet);
				default:
					break;
			}
		}
		return true;
	}

	private boolean hasIncomingChanges(ChangeSet changeSet) {
		if (changeSet instanceof DiffChangeSet) {
			return ((DiffChangeSet) changeSet).getDiffTree()
					.countFor(IThreeWayDiff.INCOMING, IThreeWayDiff.DIRECTION_MASK) > 0;
		}
		return false;
	}

	private boolean hasOutgoingChanges(ChangeSet ChangeSet) {
		if (ChangeSet instanceof DiffChangeSet) {
			return ((DiffChangeSet) ChangeSet).getDiffTree()
					.countFor(IThreeWayDiff.OUTGOING, IThreeWayDiff.DIRECTION_MASK) > 0;
		}
		return false;
	}

	private boolean isUnassignedSet(ChangeSet changeSet) {
		return changeSet == unassignedDiffs;
	}

	private boolean hasConflicts(ChangeSet changeSet) {
		if (changeSet instanceof DiffChangeSet) {
			return ((DiffChangeSet) changeSet).getDiffTree()
					.countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK) > 0;
		}
		return false;
	}

	private boolean containsConflicts(ChangeSet changeSet) {
		if (changeSet instanceof DiffChangeSet) {
			return ((DiffChangeSet) changeSet).getDiffTree()
					.hasMatchingDiffs(ResourcesPlugin.getWorkspace().getRoot().getFullPath(),
							ResourceModelLabelProvider.CONFLICT_FILTER);
		}
		return false;
	}

	private boolean isVisible(Object object, IResourceDiffTree tree) {
		if (object instanceof IResource) {
			IResource resource = (IResource) object;
			IDiff diff = tree.getDiff(resource);
			if (diff != null && this.isVisible(diff)) {
				return true;
			}
			IDiff[] diffs = tree.getDiffs(resource, getTraversalCalculator().getLayoutDepth(resource, null));
			for (IDiff diff2 : diffs) {
				if (isVisible(diff2)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasChildrenInContext(ChangeSet set) {
		IResource[] resources = set.getResources();
		for (IResource element : resources) {
			if (getContext().getDiffTree().getDiff(element) != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasChildren(TreePath path) {
		if (path.getSegmentCount() == 1) {
			Object first = path.getFirstSegment();
			if (first instanceof ChangeSet) {
				return isVisibleInMode(first) && hasChildrenInContext((ChangeSet) first);
			}
		}
		return getChildren(path).length > 0;
	}

	@Override
	public TreePath[] getParents(Object element) {
		if (element instanceof ChangeSet) {
			return new TreePath[] { TreePath.EMPTY };
		}
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			DiffChangeSet[] sets = getSetsContaining(resource);
			if (sets.length > 0) {
				ArrayList<TreePath> result = new ArrayList<>();
				for (DiffChangeSet set : sets) {
					TreePath path = getPathForElement(set, resource.getParent());
					if (path != null) {
						result.add(path);
					}
				}
				return result.toArray(new TreePath[result.size()]);
			}
			TreePath path = getPathForElement(getUnassignedSet(), resource.getParent());
			if (path != null) {
				return new TreePath[] { path };
			}
		}
		return new TreePath[0];
	}

	/*
	 * It doesn't include unassigned change set
	 */
	private DiffChangeSet[] getOutgoingSets() {
		ArrayList<DiffChangeSet> result = new ArrayList<>();
		ChangeSetCapability changeSetCapability = getChangeSetCapability();
		if (changeSetCapability != null && changeSetCapability.supportsActiveChangeSets()) {
			ActiveChangeSetManager collector = changeSetCapability.getActiveChangeSetManager();
			ChangeSet[] sets = collector.getSets();
			for (ChangeSet set : sets) {
				result.add((DiffChangeSet) set);
			}
		}
		return result.toArray(new DiffChangeSet[result.size()]);
	}

	private DiffChangeSet[] getAllSets() {
		ArrayList<DiffChangeSet> result = new ArrayList<>();
		ChangeSetCapability changeSetCapability = getChangeSetCapability();
		if (changeSetCapability != null && changeSetCapability.supportsActiveChangeSets()) {
			ActiveChangeSetManager collector = changeSetCapability.getActiveChangeSetManager();
			ChangeSet[] sets = collector.getSets();
			for (ChangeSet set : sets) {
				result.add((DiffChangeSet) set);
			}
		}
		if (incomingCollector != null) {
			ChangeSet[] sets = incomingCollector.getSets();
			for (ChangeSet set : sets) {
				result.add((DiffChangeSet) set);
			}
		}
		return result.toArray(new DiffChangeSet[result.size()]);
	}

	private DiffChangeSet[] getSetsContaining(IResource resource) {
		ArrayList<DiffChangeSet> result = new ArrayList<>();
		DiffChangeSet[] allSets = getAllSets();
		for (DiffChangeSet set : allSets) {
			if (isVisible(resource, set.getDiffTree())) {
				result.add(set);
			}
		}
		return result.toArray(new DiffChangeSet[result.size()]);
	}

	private TreePath getPathForElement(DiffChangeSet set, IResource resource) {
		List<Object> pathList = getPath(set.getDiffTree(), resource);
		if (pathList != null) {
			pathList.add(0, set);
			TreePath path = new TreePath(pathList.toArray());
			return path;
		}
		return null;
	}

	private List<Object> getPath(IResourceDiffTree tree, IResource resource) {
		if (resource == null) {
			return null;
		}
		boolean hasDiff = tree.getDiff(resource) == null;
		if (hasDiff && tree.members(resource).length == 0 || resource.getType() == IResource.ROOT) {
			return null;
		}
		ArrayList<Object> result = new ArrayList<>();
		result.add(resource.getProject());
		if (resource.getType() != IResource.PROJECT) {
			String layout = getTraversalCalculator().getLayout();
			if (layout.equals(IPreferenceIds.FLAT_LAYOUT) || layout.equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.FOLDER) {
				result.add(resource);
			} else if (layout.equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.FILE) {
				IContainer parent = resource.getParent();
				if (parent.getType() != IResource.PROJECT) {
					result.add(parent);
				}
				result.add(resource);
			} else {
				ArrayList<Object> resourcePath = new ArrayList<>();
				IResource next = resource;
				while (next.getType() != IResource.PROJECT) {
					resourcePath.add(next);
					next = next.getParent();
				}
				for (int i = resourcePath.size() - 1; i >= 0; i--) {
					result.add(resourcePath.get(i));
				}
			}
		}
		return result;
	}

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		ChangeSetCapability capability = getChangeSetCapability();
		if (capability.supportsActiveChangeSets()) {
			ActiveChangeSetManager collector = capability.getActiveChangeSetManager();
			collector.addListener(collectorListener);
		}
		SVNChangeSetSorter sorter = getSorter();
		if (sorter != null) {
			sorter.setConfiguration(getConfiguration());
		}
	}

	private SVNChangeSetSorter getSorter() {
		INavigatorContentService content = getExtensionSite().getService();
		INavigatorSorterService sorting = content.getSorterService();
		INavigatorContentExtension extension = getExtensionSite().getExtension();
		if (extension != null) {
			ViewerSorter sorter = sorting.findSorter(extension.getDescriptor(), getModelProvider(),
					new DiffChangeSet(), new DiffChangeSet());
			//FIXME: AF: not a case anymore, need to find another way
//			if (sorter instanceof SVNChangeSetSorter) {
//				return (SVNChangeSetSorter) sorter;
//			}
		}
		return null;
	}

	private void initializeCheckedInChangeSetCollector(ChangeSetCapability capability) {
		if (capability.supportsCheckedInChangeSets()) {
			incomingCollector = ((SVNModelParticipantChangeSetCapability) capability)
					.createIncomingChangeSetCollector(getConfiguration());
			incomingCollector.addListener(collectorListener);
			incomingCollector.add(((ResourceDiffTree) getContext().getDiffTree()).getDiffs());
		}
	}

	@Override
	public void dispose() {
		ChangeSetCapability capability = getChangeSetCapability();
		if (capability.supportsActiveChangeSets()) {
			capability.getActiveChangeSetManager().removeListener(collectorListener);
		}
		if (incomingCollector != null) {
			incomingCollector.removeListener(collectorListener);
			incomingCollector.dispose();
		}
		super.dispose();
	}

	public IResourceDiffTree getDiffTree(TreePath path) {
		if (path.getSegmentCount() > 0) {
			Object first = path.getFirstSegment();
			if (first instanceof DiffChangeSet) {
				DiffChangeSet set = (DiffChangeSet) first;
				return set.getDiffTree();
			}
		}
		return getTheRest();
	}

	@Override
	public void diffsChanged(final IDiffChangeEvent event, IProgressMonitor monitor) {
		IPath[] removed = event.getRemovals();
		IDiff[] added = event.getAdditions();
		IDiff[] changed = event.getChanges();
		try {
			getTheRest().beginInput();
			for (IPath element : removed) {
				getTheRest().remove(element);
			}
			doDiffsChanged(added);
			doDiffsChanged(changed);
		} finally {
			getTheRest().endInput(monitor);
		}
		if (incomingCollector != null) {
			incomingCollector.handleChange(event);
		}
		UIMonitorUtility.getDisplay().asyncExec(() -> {
			AbstractTreeViewer viewer = (AbstractTreeViewer) getViewer();
			viewer.refresh();
		});
	}

	protected void doDiffsChanged(IDiff[] diff) {
		for (IDiff element : diff) {
			if (!isContainedInSet(element, getOutgoingSets())) {
				if (hasLocalChanges(element)) {
					getTheRest().add(element);
				} else {
					getTheRest().remove(element.getPath());
				}
			}
		}
	}

	protected boolean canAddToUnnassignedChangeSet(IDiff diff) {
		if (!isContainedInSet(diff, getOutgoingSets())) {
			return hasLocalChanges(diff);
		}
		return false;
	}

	protected boolean hasLocalChanges(IDiff diff) {
		try {
			//TODO correctly get subscriber
			Subscriber subscriber = UpdateSubscriber.instance();
			AbstractSVNSyncInfo syncInfo = (AbstractSVNSyncInfo) subscriber
					.getSyncInfo(ResourceDiffTree.getResourceFor(diff));
			if (syncInfo != null && (SyncInfo.getDirection(syncInfo.getKind()) & SyncInfo.OUTGOING) != 0) {
				return true;
			}
		} catch (Exception e) {
			LoggedOperation.reportError(SVNChangeSetContentProvider.class.getName(), e);
		}
		return false;
	}

	@Override
	protected void updateLabels(ISynchronizationContext context, IPath[] paths) {
		super.updateLabels(context, paths);
		ChangeSet[] sets = this.getSetsShowingPropogatedStateFrom(paths);
		if (sets.length > 0) {
			((AbstractTreeViewer) getViewer()).update(sets, null);
		}
	}

	private ChangeSet[] getSetsShowingPropogatedStateFrom(IPath[] paths) {
		HashSet<ChangeSet> result = new HashSet<>();
		for (IPath path : paths) {
			ChangeSet[] sets = this.getSetsShowingPropogatedStateFrom(path);
			Collections.addAll(result, sets);
		}
		return result.toArray(new ChangeSet[result.size()]);
	}

	protected DiffChangeSet[] getSetsShowingPropogatedStateFrom(IPath path) {
		ArrayList<DiffChangeSet> result = new ArrayList<>();
		DiffChangeSet[] allSets = getAllSets();
		for (DiffChangeSet set : allSets) {
			if (set.getDiffTree().getDiff(path) != null || set.getDiffTree().getChildren(path).length > 0) {
				result.add(set);
			}
		}
		return result.toArray(new DiffChangeSet[result.size()]);
	}

	public ChangeSetCapability getChangeSetCapability() {
		ISynchronizeParticipant participant = getConfiguration().getParticipant();
		if (participant instanceof IChangeSetProvider) {
			IChangeSetProvider provider = (IChangeSetProvider) participant;
			return provider.getChangeSetCapability();
		}
		return null;
	}

	private HashSet<ChangeSet> getVisibleSetsInViewer() {
		TreeViewer viewer = (TreeViewer) getViewer();
		Tree tree = viewer.getTree();
		TreeItem[] children = tree.getItems();
		HashSet<ChangeSet> result = new HashSet<>();
		for (TreeItem control : children) {
			Object data = control.getData();
			if (data instanceof ChangeSet) {
				ChangeSet set = (ChangeSet) data;
				result.add(set);
			}
		}
		return result;
	}
}
