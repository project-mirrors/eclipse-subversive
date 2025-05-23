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

package org.eclipse.team.svn.ui.synchronize.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.ui.action.IResourceSelector;
import org.eclipse.team.svn.ui.synchronize.FilteredSynchronizeModelOperation;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

/**
 * Synchronize view action abstract implementation
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractSynchronizeModelAction extends SynchronizeModelAction {
	/**
	 * Provides set of resources filtered by FastSyncInfoFilter(s)
	 */
	protected IResourceSelector syncInfoSelector;

//	/**
//	 * Handles tree-based selection without applying of FastSyncInfoFilter(s).
//	 * Group nodes are provided by default, in order to disallow group nodes please use code <code>new ISyncStateFilter.StateFilterWrapper(filter, false)</code>.
//	 */
//	protected IResourceSelector treeNodeSelector;

	public AbstractSynchronizeModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		setEnabled(false);
		setToolTipText(text);

		createSyncInfoSelector();
		//this.createTreeNodeSelector();
	}

	public AbstractSynchronizeModelAction(String text, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		setEnabled(false);
		setToolTipText(text);

		createSyncInfoSelector();
		//this.createTreeNodeSelector();
	}

	@Override
	protected final SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration,
			IDiffElement[] elements) {
		IActionOperation op = getOperation(configuration, elements);
		return new FilteredSynchronizeModelOperation(configuration, elements, op);
	}

	protected abstract IActionOperation getOperation(ISynchronizePageConfiguration configuration,
			IDiffElement[] elements);

	protected void createSyncInfoSelector() {
		syncInfoSelector = new IResourceSelector() {
			@Override
			public IResource[] getSelectedResources() {
				return this.getSelectedResources(new ISyncStateFilter.StateFilterWrapper(IStateFilter.SF_ALL, false));
			}

			@Override
			public IResource[] getSelectedResources(IStateFilter filter) {
				if (filter instanceof ISyncStateFilter) {
					return this.getSelectedResources((ISyncStateFilter) filter);
				}
				return this.getSelectedResources(new ISyncStateFilter.StateFilterWrapper(filter, false));
			}

			@Override
			public IResource[] getSelectedResourcesRecursive(IStateFilter filter) {
				return this.getSelectedResources(filter);
			}

			@Override
			public IResource[] getSelectedResourcesRecursive(IStateFilter filter, int depth) {
				return this.getSelectedResources(filter);
			}

			private IResource[] getSelectedResources(ISyncStateFilter filter) {
				AbstractSVNSyncInfo[] infos = AbstractSynchronizeModelAction.this.getSVNSyncInfos();
				HashSet<IResource> retVal = new HashSet<>();
				for (AbstractSVNSyncInfo element : infos) {
					ILocalResource local = element.getLocalResource();
					ILocalResource remote = element.getRemoteChangeResource();
					if (remote instanceof IResourceChange
							&& filter.acceptRemote(remote.getResource(), remote.getStatus(), remote.getChangeMask())
							|| filter.accept(local)) {
						retVal.add(local.getResource());
					}
				}
				if (filter.acceptGroupNodes()) {
					HashSet<ISynchronizeModelElement> selection = new HashSet<>(
							Arrays.asList(AbstractSynchronizeModelAction.this.getSelectedElements()));
					for (IDiffElement element : AbstractSynchronizeModelAction.this.getFilteredDiffElements()) {
						if (element instanceof ISynchronizeModelElement
								&& retVal.contains(((ISynchronizeModelElement) element).getResource())) {
							IDiffContainer parent = element.getParent();
							ArrayList<IResource> parents = new ArrayList<>();
							while (parent != null && parent instanceof ISynchronizeModelElement
									&& ((ISynchronizeModelElement) parent).getResource() != null) {
								IResource parentResource = ((ISynchronizeModelElement) parent).getResource();
								try {
									//As there can be unversioned externals in Sync View, don't process them
									AbstractSVNSyncInfo info = (AbstractSVNSyncInfo) UpdateSubscriber.instance()
											.getSyncInfo(parentResource);
									if (info != null) {
										ILocalResource local = info.getLocalResource();
										if (!IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(local)) {
											parents.add(parentResource);
										}
									}

									if (selection.contains(parent)) {
										retVal.addAll(parents);
										break;
									}
									parent = parent.getParent();
								} catch (Exception e) {
									LoggedOperation.reportError(this.getClass().getName(), e);
								}
							}
						}
					}
				}
				return retVal.toArray(new IResource[retVal.size()]);
			}
		};
	}

//	protected void createTreeNodeSelector() {
//		this.treeNodeSelector = new IResourceSelector() {
//			public IResource[] getSelectedResources() {
//			    return this.getSelectedResources(new ISyncStateFilter.StateFilterWrapper(IStateFilter.SF_ALL, true));
//			}
//
//			public IResource[] getSelectedResources(IStateFilter filter) {
//				return this.getSelectedResourcesRecursive(filter, IResource.DEPTH_ZERO);
//			}
//
//			public IResource[] getSelectedResourcesRecursive(IStateFilter filter) {
//				return this.getSelectedResourcesRecursive(filter, IResource.DEPTH_INFINITE);
//			}
//
//			public IResource[] getSelectedResourcesRecursive(IStateFilter filter, int depth) {
//	            if (filter instanceof ISyncStateFilter) {
//	    			return this.getSelectedResourcesRecursive((ISyncStateFilter)filter, depth);
//	            }
//				return this.getSelectedResourcesRecursive(new ISyncStateFilter.StateFilterWrapper(filter, true), depth);
//			}
//
//			private IResource[] getSelectedResourcesRecursive(ISyncStateFilter filter, int depth) {
//			    HashSet<IResource> retVal = new HashSet<IResource>();
//				for (ISynchronizeModelElement element : AbstractSynchronizeModelAction.this.getSelectedElements()) {
//	    			this.fetchSelectedNodes(retVal, element, filter, depth);
//				}
//				return retVal.toArray(new IResource[retVal.size()]);
//			}
//
//			private void fetchSelectedNodes(Set<IResource> nodes, ISynchronizeModelElement node, ISyncStateFilter filter, int depth) {
//				IResource resource = node.getResource();
//				if (filter.accept(SVNRemoteStorage.instance().asLocalResource(resource))) {
//					nodes.add(resource);
//				}
//				else if (node instanceof SyncInfoModelElement) {
//					ILocalResource change = ((ResourceVariant)((AbstractSVNSyncInfo)((SyncInfoModelElement)node).getSyncInfo()).getRemote()).getResource();
//					if (change instanceof IResourceChange && filter.acceptRemote(change.getResource(), change.getStatus(), change.getChangeMask())) {
//						nodes.add(resource);
//					}
//				}
//				if (depth != IResource.DEPTH_ZERO) {
//					int sizeBefore = nodes.size();
//		    		for (IDiffElement element : node.getChildren()) {
//		    			this.fetchSelectedNodes(nodes, (ISynchronizeModelElement)element, filter, depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO);
//		    		}
//		    		if (sizeBefore != nodes.size() && filter.acceptGroupNodes()) {
//		    			nodes.add(resource);
//		    		}
//				}
//			}
//		};
//	}

	public IResource getSelectedResource() {
		ISynchronizeModelElement[] selection = getSelectedElements();
		return selection.length == 0 ? null : getSelectedElements()[0].getResource();
	}

	public IResource[] getAllSelectedResources() {
		List<IResource> resources = new ArrayList<>();
		ISynchronizeModelElement[] selection = getSelectedElements();
		for (ISynchronizeModelElement modelElement : selection) {
			IResource resource = modelElement.getResource();
			if (resource != null) {
				resources.add(resource);
			}
		}
		return resources.toArray(new IResource[0]);
	}

	public AbstractSVNSyncInfo getSelectedSVNSyncInfo() {
		ISynchronizeModelElement[] selection = getSelectedElements();
		if (selection.length == 0 || !(selection[0] instanceof SyncInfoModelElement)) {
			return null;
		}
		return (AbstractSVNSyncInfo) ((SyncInfoModelElement) selection[0]).getSyncInfo();
	}

	public AbstractSVNSyncInfo[] getSVNSyncInfos() {
		List<AbstractSVNSyncInfo> filtered = new ArrayList<>();
		for (IDiffElement e : getFilteredDiffElements()) {
			filtered.add((AbstractSVNSyncInfo) ((SyncInfoModelElement) e).getSyncInfo());
		}
		return filtered.toArray(new AbstractSVNSyncInfo[filtered.size()]);
	}

	protected ISynchronizeModelElement[] getSelectedElements() {
		ArrayList<ISynchronizeModelElement> retVal = new ArrayList<>();
		IStructuredSelection selection = AbstractSynchronizeModelAction.this.getStructuredSelection();
		for (Object element : selection) {
			if (element instanceof ISynchronizeModelElement) {
				retVal.add((ISynchronizeModelElement) element);
			}
		}
		return retVal.toArray(new ISynchronizeModelElement[retVal.size()]);
	}

	public IResourceSelector getSyncInfoSelector() {
		return syncInfoSelector;
	}

//	public IResourceSelector getTreeNodeSelector() {
//		return this.treeNodeSelector;
//	}
}
