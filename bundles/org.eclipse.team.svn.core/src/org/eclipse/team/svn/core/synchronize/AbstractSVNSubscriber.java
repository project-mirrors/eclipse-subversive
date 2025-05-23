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
 *    Andrey Loskutov - Performance improvements for AbstractSVNSubscriber
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.diff.provider.ThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.internal.core.mapping.ResourceVariantFileRevision;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.local.IRemoteStatusOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ILoggedOperationFactory;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Abstract subscriber class. Can be implemented as synchronize or merge subscriber.
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractSVNSubscriber extends Subscriber implements IResourceStatesListener {
	protected static final IResourceVariantComparator RV_COMPARATOR = new ResourceVariantComparator();

	public static final String CONTIGOUS_PREF_NODE = "contigous"; //$NON-NLS-1$

	public static final String CONTIGOUS_REPORT_DEFAULT = "true"; //$NON-NLS-1$

	protected final static QualifiedName REMOTE_CACHE_KEY = new QualifiedName("org.eclipse.team.svn", //$NON-NLS-1$
			"remote-cache-key"); //$NON-NLS-1$

	protected IRemoteStatusCache statusCache;

	protected Set<IResource> oldResources;

	protected String name;

	public AbstractSVNSubscriber(boolean usePersistentCache, String name) {
		this.name = name;
		if (usePersistentCache) {
			statusCache = new PersistentRemoteStatusCache(REMOTE_CACHE_KEY);
		} else {
			statusCache = new RemoteStatusCache();
		}
		SVNRemoteStorage.instance().addResourceStatesListener(ResourceStatesChangedEvent.class, this);
		oldResources = new HashSet<>();
	}

	public static boolean getSynchInfoContigous() {
		return Boolean.parseBoolean(SVNTeamPlugin.instance()
				.getPreferences()
				.node("synch_info") //$NON-NLS-1$
				.get(AbstractSVNSubscriber.CONTIGOUS_PREF_NODE, AbstractSVNSubscriber.CONTIGOUS_REPORT_DEFAULT));
	}

	public static void setSynchInfoContigous(boolean isContigous) {
		SVNTeamPlugin.instance()
				.getPreferences()
				.node("synch_info") //$NON-NLS-1$
				.put(AbstractSVNSubscriber.CONTIGOUS_PREF_NODE, String.valueOf(isContigous));
		SVNTeamPlugin.instance().savePreferences();
	}

	public boolean isSynchronizedWithRepository() throws TeamException {
		return statusCache.containsData();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isSupervised(IResource resource) {
		return FileUtility.isConnected(resource) && !FileUtility.isNotSupervised(resource);
	}

	@Override
	public IResource[] members(IResource resource) throws TeamException {
		ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
		/*
		 * Don't filter out incoming changes here (which don't exist on file system but are phantoms)
		 * 
		 * Allow to return members for unversioned external
		 */
		if (IStateFilter.SF_INTERNAL_INVALID.accept(local)
				|| IStateFilter.SF_IGNORED.accept(local) && !IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(local)) {
			return FileUtility.NO_CHILDREN;
		}
		return statusCache.allMembers(resource);
	}

	@Override
	public IResource[] roots() {
		ArrayList<IResource> roots = new ArrayList<>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (FileUtility.isConnected(project)) {
				roots.add(project);
			}
		}
		return roots.toArray(new IResource[roots.size()]);
	}

	// override in order to correctly support models
	@Override
	public IDiff getDiff(IResource resource) throws CoreException {
		AbstractSVNSyncInfo info = (AbstractSVNSyncInfo) getSyncInfo(resource);
		if (info == null || info.getKind() == SyncInfo.IN_SYNC) {
			return null;
		}
		int direction = SyncInfo.getDirection(info.getKind());
		ITwoWayDiff local = null;
		if (direction == SyncInfo.OUTGOING || direction == SyncInfo.CONFLICTING) {
			int kind = AbstractSVNSubscriber.syncKind2DiffKind(info.getLocalKind());
			if (resource.getType() == IResource.FILE) {
				IFileRevision before = asFileState(info.getBase());
				//FIXME: SVNLocalFileRevision - move all the related stuff from the UI plug-in
				IFileRevision after = new LocalFileRevision((IFile) local);
				local = new ResourceDiff(info.getLocal(), kind, 0, before, after);
			} else {// For folders, we don't need file states
				local = new ResourceDiff(info.getLocal(), kind, 0, null, null); // using this type of constructor instead of shorthand notation, since we don't want assertion in case of obstructed resources of different kinds
			}
		}
		ITwoWayDiff remote = null;
		if (direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING) {
			int kind = AbstractSVNSubscriber.syncKind2DiffKind(info.getRemoteKind());
			if (info.getLocal().getType() == IResource.FILE) {
				IFileRevision before = asFileState(info.getBase());
				IFileRevision after = asFileState(info.getRemote());
				remote = new ResourceDiff(info.getLocal(), kind, 0, before, after);
			} else {
				remote = new ResourceDiff(info.getLocal(), kind);
			}
		}
		return new ThreeWayDiff(local, remote);
	}

	private static int syncKind2DiffKind(int kind) {
		kind = SyncInfo.getChange(kind);
		return kind == SyncInfo.ADDITION
				? IDiff.ADD
				: kind == SyncInfo.DELETION ? IDiff.REMOVE : kind == SyncInfo.CHANGE ? IDiff.CHANGE : IDiff.NO_CHANGE;
	}

	private IFileRevision asFileState(IResourceVariant variant) {
		return variant == null ? null : new ResourceVariantFileRevision(variant);
	}

	@Override
	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		if (!isSupervised(resource)) {
			return null;
		}
		IResourceChange remoteStatus = SVNRemoteStorage.instance()
				.resourceChangeFromBytes(statusCache.getBytes(resource));
		// incoming additions shouldn't call WC access
		ILocalResource localStatus = statusCache.containsData()
				? SVNRemoteStorage.instance().asLocalResourceDirty(resource)
				: SVNRemoteStorage.instance().asLocalResource(resource);
		if (!IStateFilter.SF_INTERNAL_INVALID.accept(localStatus) || remoteStatus != null) {
			SyncInfo info = getSVNSyncInfo(localStatus, remoteStatus);
			if (info != null) {
				info.init();
				int kind = info.getKind();
				if (SyncInfo.getChange(kind) == SyncInfo.DELETION
						&& (SyncInfo.getDirection(kind) & SyncInfo.OUTGOING) != 0 && !resource.exists()) {
					synchronized (oldResources) {
						oldResources.add(resource);
					}
				} else if (localStatus.hasTreeConflict() && !resource.exists()) {
					/*
					 * Handle situation if resource has tree conflict and doesn't exist locally,
					 * e.g. local - missing, incoming - modify during merge operation.
					 * 
					 * E.g. it can be used to update Synchronize View content when we call
					 * Revert action from Package Explorer. As resource doesn't exist (missing) then
					 * we don't know anything about it and it's not passed to refresh operation;
					 * as a result resource will leave in Synchronize view marked as tree conflicted
					 * despite that really it was reverted. In order to fix it, we add such resource
					 * in oldResources list.
					 */
					synchronized (oldResources) {
						oldResources.add(resource);
					}
				}
			}
			return info;
		}
		return null;
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return AbstractSVNSubscriber.RV_COMPARATOR;
	}

	@Override
	public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		ArrayList<IResource> resourcesToOperateList = new ArrayList<>();
		for (IResource current : resources) {
			if (FileUtility.isConnected(current)) {
				resourcesToOperateList.add(current);
			}
		}
		IResource[] operableData = resourcesToOperateList.toArray(new IResource[0]);
		HashSet<IResource> refreshScope = this.clearRemoteStatusesImpl(operableData);
		AbstractSVNSubscriber.this.resourcesStateChangedImpl(refreshScope.toArray(new IResource[refreshScope.size()]));
		if (AbstractSVNSubscriber.getSynchInfoContigous()) {
			IActionOperation op = new UpdateStatusOperation(operableData, depth);
			ProgressMonitorUtility.doTaskExternal(op, monitor);
		} else {
			resourcesStateChangedImpl(findChanges(operableData, depth, monitor,
					SVNTeamPlugin.instance().getOptionProvider().getLoggedOperationFactory()));
		}
	}

	public void clearRemoteStatuses(IResource[] resources) throws TeamException {
		HashSet<IResource> refreshScope = this.clearRemoteStatusesImpl(resources);
		resourcesStateChangedImpl(refreshScope.toArray(new IResource[refreshScope.size()]));
	}

	@Override
	public void resourcesStateChanged(ResourceStatesChangedEvent event) {
		try {
			if (event.type == ResourceStatesChangedEvent.CHANGED_NODES) {
				// event contains roots list + depth (and depth could be one of: zero, one, infinite)
				resourcesStateChangedImpl(event.getResourcesRecursivelly());
			}
		} catch (TeamException e) {
			LoggedOperation.reportError(this.getClass().getName(), e);
		}
	}

	protected HashSet<IResource> clearRemoteStatusesImpl(IResource[] resources) throws TeamException {
		return this.clearRemoteStatusesImpl(statusCache, resources);
	}

	protected HashSet<IResource> clearRemoteStatusesImpl(IRemoteStatusCache cache, IResource[] resources)
			throws TeamException {
		final HashSet<IResource> refreshSet = new HashSet<>();
		cache.traverse(resources, IResource.DEPTH_INFINITE, (current, data) -> {
			IResource resource = SVNRemoteStorage.instance().resourceChangeFromBytes(data).getResource();
			if (resource != null) {
				refreshSet.add(resource);
			}
		});
		for (IResource element : resources) {
			cache.flushBytes(element, IResource.DEPTH_INFINITE);
		}
		return refreshSet;
	}

	protected void resourcesStateChangedImpl(IResource[] resources) throws TeamException {
		synchronized (oldResources) {
			Set<IResource> allResources = new HashSet<>(Arrays.asList(resources));
			for (Iterator<IResource> it = oldResources.iterator(); it.hasNext();) {
				IResource resource = it.next();
				/*
				 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=207026
				 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=282000
				 * 		deleted/missing file which was removed from SychView reappears after modification of an unrelated resource
				 * 		the reason is: this code adds it to the refresh event
				 * 		the reason this code exists - Team Services caches resource states and then, when deletion is committed,
				 * 		there is no way to remove deletion from Synch View because to SVN plug-in it does not exists anymore.
				 * 
				 * Is there a better way, so that there is no access to working copy?
				 */
				if (resource.getLocation() == null) {
					it.remove(); // no need to keep checking the resource if it became inaccessible
				} else if (!allResources.contains(resource)) {
					SVNChangeStatus status = SVNUtility.getSVNInfoForNotConnected(resource);
					// when the status changes from deleted/missing it is time to refresh corresponding resource
					if (status == null || status.textStatus != SVNEntryStatus.Kind.DELETED
							&& status.textStatus != SVNEntryStatus.Kind.MISSING) {
						allResources.add(resource);
						it.remove();
					}
				}
			}
			if (allResources.isEmpty()) {
				return;
			}
			IResource[] refreshSet = allResources.toArray(new IResource[allResources.size()]);
			// ensure we cached all locally-known resources (used in pair with asLocalResourceDirty() in getSyncInfo())
			//	TODO best case - the code should not exists. Needs to be verified if there is a different approach.
			if (CoreExtensionsManager.instance().getOptionProvider().is(IOptionProvider.SVN_CACHE_ENABLED)) {
				IResource[] parents = FileUtility.getParents(refreshSet, false);
				FileUtility.reorder(parents, true); //ensure the proper load order, so that there is no performance overhead
				for (IResource parent : parents) {
					try {
						SVNRemoteStorage.instance().getRegisteredChildren((IContainer) parent);
					} catch (Exception ex) {
						LoggedOperation.reportError(SVNMessages.getErrorString("Error_CheckCache"), ex); //$NON-NLS-1$
					}
				}
			}
			fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, refreshSet));
		}
	}

	protected IResource[] findChanges(IResource[] resources, int depth, IProgressMonitor monitor,
			ILoggedOperationFactory operationWrapperFactory) {
		CompositeOperation op = new CompositeOperation("", SVNMessages.class); //$NON-NLS-1$

		final IRemoteStatusOperation rStatusOp = addStatusOperation(op, resources, depth);
		if (rStatusOp == null) {
			return FileUtility.NO_CHILDREN;
		}
		op.setOperationName(rStatusOp.getId());

		final ArrayList<IResource> changes = new ArrayList<>();
		op.add(new AbstractActionOperation("Operation_FetchChanges", SVNMessages.class) { //$NON-NLS-1$
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				SVNEntryStatus[] statuses = rStatusOp.getStatuses();
				if (statuses != null) {

					//prepare and sort resources
					//If we don't sort them, PersistentRemoteStatusCache.setBytes will fail
					//because it can't set bytes for resource which doesn't have a parent
					Map<IResource, IResourceChange> resourcesMap = new HashMap<>();
					for (int i = 0; i < statuses.length && !monitor.isCanceled(); i++) {
						if (AbstractSVNSubscriber.this.isIncoming(statuses[i])) {
							IResourceChange resourceChange = AbstractSVNSubscriber.this.handleResourceChange(rStatusOp,
									statuses[i]);
							if (resourceChange != null) {
								IResource resource = resourceChange.getResource();
								resourcesMap.put(resource, resourceChange);
							}
						}
					}

					if (!resourcesMap.isEmpty()) {
						IResource[] resources = resourcesMap.keySet().toArray(new IResource[0]);
						FileUtility.reorder(resources, true);

						//process
						for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
							IResource resource = resources[i];
							final IResourceChange resourceChange = resourcesMap.get(resource);
							final AbstractActionOperation self = this;

							this.protectStep(monitor1 -> {
								ProgressMonitorUtility.setTaskInfo(monitor1, self,
										String.valueOf(resourceChange.getRevision()));
								statusCache.setBytes(resourceChange.getResource(),
										SVNRemoteStorage.instance().resourceChangeAsBytes(resourceChange));
							}, monitor, resources.length);
							changes.add(resourceChange.getResource());
						}
					}
				}
			}
		}, new IActionOperation[] { rStatusOp });
		ProgressMonitorUtility.doTaskExternal(op, monitor, operationWrapperFactory);

		return changes.toArray(new IResource[changes.size()]);
	}

	protected abstract boolean isIncoming(SVNEntryStatus status);

	protected abstract IResourceChange handleResourceChange(IRemoteStatusOperation rStatusOp, SVNEntryStatus status);

	protected abstract SyncInfo getSVNSyncInfo(ILocalResource localStatus, IResourceChange remoteStatus)
			throws TeamException;

	protected abstract IRemoteStatusOperation addStatusOperation(CompositeOperation op, IResource[] resources,
			int depth);

	public class UpdateStatusOperation extends AbstractActionOperation implements ILoggedOperationFactory {
		protected IResource[] resources;

		protected int depth;

		public UpdateStatusOperation(IResource[] resources, int depth) {
			super("Operation_UpdateStatus", SVNMessages.class); //$NON-NLS-1$
			ArrayList<IResource> tResources = new ArrayList<>();
			for (IResource resource : resources) {
				if (resource.getType() == IResource.ROOT) {
					tResources.addAll(Arrays.asList(((IWorkspaceRoot) resource).getProjects()));
				} else {
					tResources.add(resource);
				}
			}
			this.resources = resources;
			this.depth = depth;
		}

		@Override
		public IActionOperation getLogged(IActionOperation operation) {
			return new LoggedOperation(operation) {
				@Override
				protected void handleError(IStatus errorStatus) {
					UpdateStatusOperation.this.reportStatus(errorStatus);
				}
			};
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			Map<IProject, List<IResource>> project2Resources = SVNUtility.splitWorkingCopies(resources);
			for (Iterator<List<IResource>> it = project2Resources.values().iterator(); it.hasNext()
					&& !monitor.isCanceled();) {
				List<IResource> entry = it.next();
				final IResource[] wcResources = entry.toArray(new IResource[entry.size()]);
				this.protectStep(monitor1 -> resourcesStateChangedImpl(findChanges(
						wcResources, depth, monitor1, UpdateStatusOperation.this)), monitor, project2Resources.size());
			}
		}

	}
}
