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
 *    Panagiotis Korros - [patch] bug fix: .svn folder is deleted by subversive
 *    Andrey Loskutov - [scalability] SVN update takes hours if "Synchronize" view is opened
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.svnstorage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.SVNTeamProvider;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNProgressMonitor;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNConflictVersion;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNEntry.Kind;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNErrorCodes;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.options.IIgnoreRecommendations;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.resource.IChangeStateProvider;
import org.eclipse.team.svn.core.resource.ILocalFolder;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.utility.AsynchronousActiveQueue;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.IQueuedElement;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVN based representation of IRemoteStorage
 * 
 * How external definitions are handled: External resources are not marked anymore with external flag (ILocalResource.IS_EXTERNAL) because
 * we can't exactly determine whether resource is external or not. This depends on where status operation is called. Example: Project/ src/
 * http://localhost/repos/foo com/foo com/ Here versioned folder 'com' is a part of external, so if we call status on 'com' folder we'll not
 * get that 'foo' folder is external because external is defined on higher level. Also if we call status operation on external versioned
 * folder itself (but not on its parent folder where external is defined), then we'll not get that this folder is external. As there are
 * ambiguities with externals detection by using statuses, we don't use this approach and simply check if resource is switched or not (by
 * matching urls). So external resources are marked as switched and unversioned external folders are marked as ignored.
 * 
 * @author Alexander Gurov
 */
public class SVNRemoteStorage extends AbstractSVNStorage implements IRemoteStorage {

	/**
	 * The name of the preferences node in the Subversive Core preferences that contains the known repositories as its children.
	 */
	public static final String PREF_REPOSITORIES_NODE = "repositories"; //$NON-NLS-1$

	/**
	 * The name of the preferences node in the Subversive Core preferences that contains the flag which determines whether we migrated from
	 * Authorization database to Equinox security storage
	 */
	public static final String PREF_MIGRATE_FROM_AUTH_DB_NODE = "migrateFromAuthorizationDatabase"; //$NON-NLS-1$

	/**
	 * The name of file containing the SVN repository locations information. Deprecated since Subversive 0.7.0 v20080404 - must not be used.
	 * The valid information is stored in preferences.
	 * 
	 * @see SVNRemoteStorage.PREF_REPOSITORIES_NODE
	 */
	public static final String STATE_INFO_FILE_NAME = ".svnRepositories"; //$NON-NLS-1$

	private static SVNRemoteStorage instance = new SVNRemoteStorage();

	protected Map localResources;

	protected Map switchedToUrls;

	protected Map externalsLocations;

	protected Map<Class, List<IResourceStatesListener>> resourceStateListeners;

	protected AsynchronousActiveQueue<SvnChange> fetchQueue;

	protected AsynchronousActiveQueue<ResourceStatesChangedEvent> eventQueue;

	protected long lastMonitorTime;

	protected Map<IResource, File> changeMonitorMap;

	protected int suggestedLoadDepth = IResource.DEPTH_INFINITE;

	public void resetExternalChangesMonitor() {
		lastMonitorTime = System.currentTimeMillis();
	}

	public void checkForExternalChanges() {
		long lastMonitorTime = this.lastMonitorTime;
		ArrayList<IResource> changed = new ArrayList<>();
		for (Object item : changeMonitorMap.entrySet().toArray()) {
			Map.Entry<IResource, File> entry = (Map.Entry<IResource, File>) item;
			if (entry.getValue().lastModified() > lastMonitorTime) {
				changed.add(entry.getKey());
			}
		}
		if (changed.size() > 0) {
			IResource[] resources = changed.toArray(new IResource[changed.size()]);
			SVNRemoteStorage.instance().refreshLocalResources(resources, IResource.DEPTH_INFINITE);
			fireResourceStatesChangedEvent(new ResourceStatesChangedEvent(resources, IResource.DEPTH_INFINITE,
					ResourceStatesChangedEvent.CHANGED_NODES));
		}
		resetExternalChangesMonitor();
	}

	public static SVNRemoteStorage instance() {
		return SVNRemoteStorage.instance;
	}

	@Override
	public void addResourceStatesListener(Class eventClass, IResourceStatesListener listener) {
		synchronized (resourceStateListeners) {
			List<IResourceStatesListener> listenersList = resourceStateListeners.get(eventClass);
			if (listenersList == null) {
				resourceStateListeners.put(eventClass, listenersList = new ArrayList<>());
			}
			if (!listenersList.contains(listener)) {
				listenersList.add(listener);
			}
		}
	}

	@Override
	public void removeResourceStatesListener(Class eventClass, IResourceStatesListener listener) {
		synchronized (resourceStateListeners) {
			List<IResourceStatesListener> listenersList = resourceStateListeners.get(eventClass);
			if (listenersList != null) {
				listenersList.remove(listener);
				if (listenersList.size() == 0) {
					resourceStateListeners.remove(eventClass);
				}
			}
		}
	}

	@Override
	public void fireResourceStatesChangedEvent(ResourceStatesChangedEvent event) {
		if (event.getSize() == 0) {
			return;
		}
		synchronized (resourceStateListeners) {
			List<IResourceStatesListener> listeners = resourceStateListeners.get(event.getClass());
			if (listeners == null || listeners.isEmpty()) {
				return;
			}
		}
		eventQueue.push(event);
	}

	@Override
	public void initialize(Map<String, Object> preferences) throws Exception {
		preferences.put(AbstractSVNStorage.IPREF_STATE_INFO_FILE, SVNRemoteStorage.STATE_INFO_FILE_NAME);
		preferences.put(AbstractSVNStorage.IPREF_REPO_NODE_NAME, SVNRemoteStorage.PREF_REPOSITORIES_NODE);
		preferences.put(AbstractSVNStorage.IPREF_AUTH_NODE_NAME, SVNRemoteStorage.PREF_MIGRATE_FROM_AUTH_DB_NODE);
		super.initialize(preferences);
	}

	@Override
	public IResourceChange asResourceChange(IChangeStateProvider changeState, boolean update) {
		IResource resource = null;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IPath location = new Path(changeState.getLocalPath());
		SVNEntry.Kind nodeKind = changeState.getNodeKind();
		SVNEntryStatus.Kind propKind = changeState.getPropertiesChangeType();
		SVNEntryStatus.Kind textKind = changeState.getTextChangeType();
		boolean isCopied = changeState.isCopied();
		boolean isSwitched = changeState.isSwitched();
		SVNRevision.Number remoteRevision = changeState.getChangeRevision();
		long revision = remoteRevision != null ? remoteRevision.getNumber() : SVNRevision.INVALID_REVISION_NUMBER;
		// repositoryTextStatus can be StatusKind::none in two cases: resource not modified and non versioned
		// in the second case we should ignore repository status calculation

		SVNConflictDescriptor treeConflictDescriptor = changeState.getTreeConflictDescriptor();
		String textStatusStr = SVNRemoteStorage.getTextStatusString(propKind, textKind, update);
		String propStatusStr = SVNRemoteStorage.getPropStatusString(propKind);
		if (nodeKind == SVNEntry.Kind.DIR) {
			if ((resource = changeState
					.getExact(root.findContainersForLocationURI(URIUtil.toURI(location.makeAbsolute())))) == null) {
				return null;
			}
			//handle resource name for case insensitive OS's
			if (!resource.getName().equals(location.lastSegment())) {
				resource = root.getContainerForLocation(location);
			}

			int changeMask = SVNRemoteStorage.getChangeMask(textKind, propKind, isCopied, isSwitched, false);
			if (IStateFilter.SF_NOTEXISTS.accept(resource, textStatusStr, changeMask)) {
				revision = SVNRevision.INVALID_REVISION_NUMBER;
			}
			return new SVNFolderChange(resource, revision, textStatusStr, propStatusStr, changeMask,
					changeState.getChangeAuthor(), changeState.getChangeDate(), treeConflictDescriptor, null,
					changeState.getComment());
		}
		if ((resource = changeState
				.getExact(root.findFilesForLocationURI(URIUtil.toURI(location.makeAbsolute())))) == null) {
			return null;
		}
		//handle resource name for case insensitive OS's
		if (!resource.getName().equals(location.lastSegment())) {
			resource = root.getFileForLocation(location);
		}

		int changeMask = SVNRemoteStorage.getChangeMask(textKind, propKind, isCopied, isSwitched, false);
		if (IStateFilter.SF_NOTEXISTS.accept(resource, textStatusStr, changeMask)) {
			revision = SVNRevision.INVALID_REVISION_NUMBER;
		}
		return new SVNFileChange(resource, revision, textStatusStr, propStatusStr, changeMask,
				changeState.getChangeAuthor(), changeState.getChangeDate(), treeConflictDescriptor, null,
				changeState.getComment());
	}

	@Override
	public byte[] resourceChangeAsBytes(IResourceChange resource) {
		if (resource == null) {
			return null;
		}
		int kind = resource.getPegRevision().getKind().id;
		String originatorData = null;
		if (resource.getOriginator() != null) {
			byte[] data = repositoryResourceAsBytes(resource.getOriginator());
			originatorData = new String(Base64.encode(data));
		}
		long lastCommitDate = resource.getLastCommitDate();
		String comment = resource.getComment();
		String retVal = /*0*/ String.valueOf(resource instanceof ILocalFolder) + ";" + //$NON-NLS-1$
		/*1*/ new String(Base64.encode(FileUtility.getWorkingCopyPath(resource.getResource()).getBytes())) + ";" + //$NON-NLS-1$
		/*2*/ resource.getRevision() + ";" + //$NON-NLS-1$
		/*3*/ resource.getTextStatus() + ";" + //$NON-NLS-1$
		/*4*/ resource.getAuthor() + ";" + //$NON-NLS-1$
		/*5*/ (lastCommitDate == 0 ? "null" : String.valueOf(lastCommitDate)) + ";" + //$NON-NLS-1$ //$NON-NLS-2$
		/*6*/ String.valueOf(kind) + ";" + //$NON-NLS-1$
		/*7*/ (kind == SVNRevision.Kind.NUMBER.id
				? String.valueOf(((SVNRevision.Number) resource.getPegRevision()).getNumber())
				: String.valueOf(kind)) + ";" + //$NON-NLS-1$
		/*8*/ (originatorData != null ? originatorData : "null") + ";" + //$NON-NLS-1$ //$NON-NLS-2$
		/*9*/ (comment == null ? "null" : new String(Base64.encode(comment.getBytes()))) + ";" + //$NON-NLS-1$ //$NON-NLS-2$
		/*10*/ resource.getChangeMask() + ";" + //$NON-NLS-1$
				getTreeConflictDescriptorAsString(resource.getTreeConflictDescriptor()) + ";" +
				/*22*/ resource.getPropStatus();
		return retVal.getBytes();
	}

	protected String getTreeConflictDescriptorAsString(SVNConflictDescriptor conflictDescriptor) {
		String retVal = /*11*/ (conflictDescriptor == null ? "null" : String.valueOf(conflictDescriptor.action.id)) //$NON-NLS-1$
				+ ";" + //$NON-NLS-1$
				/*12*/ (conflictDescriptor == null ? "null" : String.valueOf(conflictDescriptor.reason.id)) + ";" + //$NON-NLS-1$ //$NON-NLS-2$
				/*13*/ (conflictDescriptor == null ? "null" : String.valueOf(conflictDescriptor.operation.id)) + ";" + //$NON-NLS-1$ //$NON-NLS-2$
				getSVNConflictVersionAsString(
						conflictDescriptor == null ? null : conflictDescriptor.srcLeftVersion)
				+ ";" + //$NON-NLS-1$
				getSVNConflictVersionAsString(
						conflictDescriptor == null ? null : conflictDescriptor.srcRightVersion);
		return retVal;
	}

	protected String getSVNConflictVersionAsString(SVNConflictVersion conflictVersion) {
		String retVal = /*14 or 18*/ (conflictVersion == null ? "null" : String.valueOf(conflictVersion.nodeKind.id)) //$NON-NLS-1$
				+ ";" + //$NON-NLS-1$
				/*15 or 19*/ (conflictVersion == null
						? "null" //$NON-NLS-1$
						: new String(Base64.encode(conflictVersion.pathInRepos.getBytes())))
				+ ";" + //$NON-NLS-1$
				/*16 or 20*/ (conflictVersion == null ? "null" : String.valueOf(conflictVersion.pegRevision)) + ";" + //$NON-NLS-1$ //$NON-NLS-2$
				/*17 or 21*/ (conflictVersion == null
						? "null" //$NON-NLS-1$
						: new String(Base64.encode(conflictVersion.reposURL.getBytes())));
		return retVal;
	}

	@Override
	public IResourceChange resourceChangeFromBytes(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		String[] data = new String(bytes).split(";"); //$NON-NLS-1$
		boolean isFolder = "true".equals(data[0]); //$NON-NLS-1$
		String name = new String(Base64.decode(data[1].getBytes()));
		long revision = Long.parseLong(data[2]);
		String textStatus = deserializeStatus(data[3]);
		String author = "null".equals(data[4]) ? null : data[4]; //$NON-NLS-1$
		long lastCommitDate = "null".equals(data[5]) ? 0 : Long.parseLong(data[5]); //$NON-NLS-1$
		int revisionKind = Integer.parseInt(data[6]);
		SVNRevision pegRevision = null;
		if (revisionKind == SVNRevision.Kind.NUMBER.id) {
			long pegNum = Long.parseLong(data[7]);
			pegRevision = pegNum == revision || revision == SVNRevision.INVALID_REVISION_NUMBER
					? null
					: SVNRevision.fromNumber(pegNum);
		} else {
			pegRevision = SVNRevision.fromKind(SVNRevision.Kind.fromId(revisionKind));
		}
		String comment = "null".equals(data[9]) ? null : new String(Base64.decode(data[9].getBytes())); //$NON-NLS-1$
		int changeMask = "null".equals(data[10]) ? ILocalResource.NO_MODIFICATION : Integer.parseInt(data[10]); //$NON-NLS-1$
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		String propStatus = IStateFilter.ST_NORMAL;
		if (data.length >= 23) {
			propStatus = deserializeStatus(data[22]);
		} else //set statuses for backward compatibility code
		if ((changeMask & ILocalResource.PROP_MODIFIED) != 0) {
			propStatus = IStateFilter.ST_MODIFIED;
			if ((changeMask & ILocalResource.TEXT_MODIFIED) == 0) {
				textStatus = IStateFilter.ST_NORMAL;
			}
		}

		SVNConflictDescriptor treeConflict = getTreeConflictDescriptorFromString(name, data);
		IResourceChange change = isFolder
				? (IResourceChange) new SVNFolderChange(root.getContainerForLocation(new Path(name)), revision,
						textStatus, propStatus, changeMask, author, lastCommitDate, treeConflict, pegRevision, comment)
				: new SVNFileChange(root.getFileForLocation(new Path(name)), revision, textStatus, propStatus,
						changeMask, author, lastCommitDate, treeConflict, pegRevision, comment);

		if (!"null".equals(data[8])) { //$NON-NLS-1$
			byte[] originatorData = Base64.decode(data[8].getBytes());
			change.setOriginator(this.repositoryResourceFromBytes(originatorData));
		}

		return change;
	}

	protected SVNConflictDescriptor getTreeConflictDescriptorFromString(String path, String[] data) {
		SVNConflictDescriptor conflictDescriptor = null;
		if (data.length >= 23 && !"null".equals(data[11])) { //$NON-NLS-1$
			SVNConflictDescriptor.Action action = "null".equals(data[11]) //$NON-NLS-1$
					? SVNConflictDescriptor.Action.MODIFY
					: SVNConflictDescriptor.Action.fromId(Integer.parseInt(data[11]));
			SVNConflictDescriptor.Reason reason = "null".equals(data[12]) //$NON-NLS-1$
					? SVNConflictDescriptor.Reason.MODIFIED
					: SVNConflictDescriptor.Reason.fromId(Integer.parseInt(data[12]));
			SVNConflictDescriptor.Operation operation = "null".equals(data[13]) //$NON-NLS-1$
					? SVNConflictDescriptor.Operation.NONE
					: SVNConflictDescriptor.Operation.fromId(Integer.parseInt(data[13]));
			conflictDescriptor = new SVNConflictDescriptor(path, action, reason, operation,
					getSVNConflictVersionFromString(data, true), getSVNConflictVersionFromString(data, false));
		}
		return conflictDescriptor;
	}

	protected SVNConflictVersion getSVNConflictVersionFromString(String[] data, boolean isLeft) {
		int indexShift = isLeft ? 0 : 4;
		SVNEntry.Kind nodeKind = SVNEntry.Kind
				.fromId("null".equals(data[14 + indexShift]) ? 0 : Integer.parseInt(data[14 + indexShift])); //$NON-NLS-1$
		String pathInRepos = "null".equals(data[15 + indexShift]) //$NON-NLS-1$
				? null
				: new String(Base64.decode(data[15 + indexShift].getBytes()));
		long pegRevision = "null".equals(data[16 + indexShift]) ? 0 : Long.parseLong(data[16 + indexShift]); //$NON-NLS-1$
		String reposUrl = "null".equals(data[17 + indexShift]) //$NON-NLS-1$
				? null
				: new String(Base64.decode(data[17 + indexShift].getBytes()));
		return new SVNConflictVersion(reposUrl, pegRevision, pathInRepos, nodeKind);
	}

	public synchronized IResource[] getRegisteredChildren(IContainer container) throws Exception {
		// for null and workspace root members shouldn't be provided
		if (container == null || container.getProject() == null) {
			return null;
		}
		IResource[] members = FileUtility.resourceMembers(container, false);

		Map map = (Map) localResources.get(container);
		if (map == null) {
			loadLocalResourcesSubTree(container, IResource.DEPTH_ONE);
			map = (Map) localResources.get(container);
		}
		Set retVal = null;
		if (map != null) {
			retVal = new HashSet(Arrays.asList(members));
			for (Object local : map.values()) {
				if (((ILocalResource) local).getStatus() != IStateFilter.ST_NOTEXISTS) {
					retVal.add(((ILocalResource) local).getResource());
				}
			}
		}

		return retVal == null ? members : (IResource[]) retVal.toArray(new IResource[retVal.size()]);
	}

	@Override
	public ILocalResource asLocalResourceDirty(IResource resource) {
		if (!CoreExtensionsManager.instance().getOptionProvider().is(IOptionProvider.SVN_CACHE_ENABLED)) {
			return this.asLocalResource(resource);
		}
		// null resource and workspace root shouldn't be provided
		if (resource == null || resource.getProject() == null || !resource.getProject().isAccessible()) {
			return wrapUnexistingResource(resource, IStateFilter.ST_INTERNAL_INVALID, 0);
		}
		ILocalResource retVal = getCachedResource(resource);
		if (retVal == null) {
			ILocalResource parent = getFirstExistingParentLocal(resource);
			int mask = parent == null ? 0 : parent.getChangeMask() & ILocalResource.IS_SWITCHED;
			retVal = wrapUnexistingResource(resource, IStateFilter.ST_NOTEXISTS, mask);
		}
		return retVal;
	}

	protected ILocalResource getCachedResource(IResource resource) {
		Map map = (Map) localResources.get(resource.getParent());
		if (map != null) {
			return (ILocalResource) map.get(resource);
		}
		return null;
	}

	protected void setCachedResource(ILocalResource local) {
		IResource parent = local.getResource().getParent();
		Map map = (Map) localResources.get(parent);
		if (map == null) {
			localResources.put(parent, map = new HashMap());
		}
		map.put(local.getResource(), local);
	}

	@Override
	public ILocalResource asLocalResourceAccessible(IResource resource) {
		ILocalResource retVal = this.asLocalResource(resource);
		if (IStateFilter.SF_INTERNAL_INVALID.accept(retVal)) {
			// resource == null because workspace is not refreshed, in order to avoid NPE there should be another message used...
			throw new UnreportableException(SVNMessages.formatErrorString("Error_InaccessibleResource", //$NON-NLS-1$
					new String[] { resource == null ? "" : FileUtility.getWorkingCopyPath(resource) })); //$NON-NLS-1$
		}
		return retVal;
	}

	@Override
	public ILocalResource asLocalResource(IResource resource) {
		return asLocalResourceImpl(resource, suggestedLoadDepth);
	}

	protected ILocalResource asLocalResourceImpl(IResource resource, int depth) {
		// null resource and workspace root shouldn't be provided
		if (resource == null || resource.getProject() == null || !resource.getProject().isAccessible()) {
			return wrapUnexistingResource(resource, IStateFilter.ST_INTERNAL_INVALID, 0);
		}
		synchronized (this) {
			ILocalResource local = getCachedResource(resource);
			if (local == null) {
				try {
					local = loadLocalResourcesSubTree(resource, depth);
				} catch (RuntimeException ex) {
					throw ex;
				} catch (SVNConnectorException ex) {
					return wrapUnexistingResource(resource, IStateFilter.ST_INTERNAL_INVALID, 0);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			return local;
		}
	}

	@Override
	public synchronized void refreshLocalResources(IResource[] resources, int depth) {
		suggestedLoadDepth = IResource.DEPTH_ONE;
		if (depth == IResource.DEPTH_INFINITE) {
			resources = FileUtility.shrinkChildNodes(resources);
			suggestedLoadDepth = IResource.DEPTH_INFINITE;
		}
		for (IResource element : resources) {
			refreshLocalResourceImpl(element, depth);
			localResources.remove(element.getParent());
		}
	}

	@Override
	public ILocalResource asLocalResource(IProject project, String url, int kind) {
		synchronized (switchedToUrls) {
			for (Iterator it = switchedToUrls.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String cachedUrl = (String) entry.getValue();
				if (SVNUtility.createPathForSVNUrl(cachedUrl).isPrefixOf(SVNUtility.createPathForSVNUrl(url))) {
					IPath target = ((IPath) entry.getKey()).append(url.substring(cachedUrl.length()))
							.removeFirstSegments(1);
					return this.asLocalResource(
							kind == IResource.FOLDER ? (IResource) project.getFolder(target) : project.getFile(target));
				}
			}
		}
		String projectUrl = this.asRepositoryResource(project).getUrl();
		if (url.length() < projectUrl.length()) {
			return wrapUnexistingResource(null, IStateFilter.ST_INTERNAL_INVALID, 0);
		}
		String pathInProject = url.substring(projectUrl.length());
		if (pathInProject.length() == 0) {
			return this.asLocalResource(project);
		}
		return this.asLocalResource(kind == IResource.FOLDER
				? (IResource) project.getFolder(pathInProject)
				: project.getFile(pathInProject));
	}

	@Override
	public IRepositoryResource asRepositoryResource(IResource resource) {
		IProject project = resource.getProject();
		IRepositoryResource baseResource = getConnectedProjectInformation(project).getRepositoryResource();

		if (resource.equals(project)) {
			return SVNUtility.copyOf(baseResource);
		}

		IRepositoryLocation location = baseResource.getRepositoryLocation();

		String url = (String) switchedToUrls.get(resource.getFullPath());
		if (url == null) {
			ILocalResource parent = getFirstExistingParentLocal(resource);
			if (parent != null && (parent.getChangeMask() & ILocalResource.IS_SWITCHED) != 0) {
				IPath parentPath = parent.getResource().getFullPath();
				if (switchedToUrls.containsKey(parentPath)) {
					url = (String) switchedToUrls.get(parentPath) + "/" //$NON-NLS-1$
							+ resource.getFullPath().removeFirstSegments(parentPath.segmentCount()).toString();
				}
			}
		}

		if (url == null) {
			url = makeUrl(resource, baseResource);
		} else if (!SVNUtility.createPathForSVNUrl(location.getRepositoryRootUrl())
				.isPrefixOf(SVNUtility.createPathForSVNUrl(url))) {
			location = wrapLocationIfRequired(location, url, resource.getType() == IResource.FILE);
		}

		return resource instanceof IContainer
				? (IRepositoryResource) location.asRepositoryContainer(url, false)
				: location.asRepositoryFile(url, false);
	}

	public IRepositoryResource asRepositoryResource(IRepositoryLocation location, SVNEntryRevisionReference reference,
			ISVNProgressMonitor monitor) throws SVNConnectorException {
		IRepositoryResource res = null;

		String url = reference.path;
		//re-create SVNEntryRevisionReference because SVNUtility.info throws exception if peg revision or revision are null
		reference = new SVNEntryRevisionReference(SVNUtility.encodeURL(url),
				reference.pegRevision == null ? SVNRevision.HEAD : reference.pegRevision,
				reference.revision == null ? SVNRevision.HEAD : reference.revision);

		if (!SVNUtility.createPathForSVNUrl(location.getRepositoryRootUrl())
				.isPrefixOf(SVNUtility.createPathForSVNUrl(url))) {
			boolean isFile = false;
			location = wrapLocationIfRequired(location, url, isFile);
		}

		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			//detect if resource is a file or directory
			SVNEntryInfo[] entriesInfo = SVNUtility.info(proxy, reference, SVNDepth.EMPTY, monitor);
			if (entriesInfo.length > 0) {
				SVNEntryInfo info = entriesInfo[0];
				if (info.kind == Kind.FILE) {
					res = location.asRepositoryFile(url, false);
				} else if (info.kind == Kind.DIR) {
					res = location.asRepositoryContainer(url, false);
				}
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}

		return res;
	}

	@Override
	public IRepositoryResource asRepositoryResource(IRepositoryLocation location, String url, boolean isFile) {
		String rootURL = location.getRepositoryRootUrl();
		if (rootURL == null) {
			rootURL = location.getUrl(); // the repository is unavailable, assume the repository root and the location url are the same
		}
		if (rootURL == null || // what if someone works with a location which is yet to be initialized?
				!SVNUtility.createPathForSVNUrl(rootURL).isPrefixOf(SVNUtility.createPathForSVNUrl(url))) {
			location = wrapLocationIfRequired(location, url, isFile);
		}
		return !isFile
				? (IRepositoryResource) location.asRepositoryContainer(url, false)
				: location.asRepositoryFile(url, false);
	}

	@Override
	public IRepositoryLocation getRepositoryLocation(IResource resource) {
		return getConnectedProjectInformation(resource.getProject()).getRepositoryLocation();
	}

	@Override
	protected IRepositoryLocation wrapLocationIfRequired(IRepositoryLocation location, String url, boolean isFile) {
		if (!SVNUtility.createPathForSVNUrl(location.getRepositoryRootUrl())
				.isPrefixOf(SVNUtility.createPathForSVNUrl(url))) {
			synchronized (externalsLocations) {
				List locations = (List) externalsLocations.get(location);
				if (locations == null) {
					externalsLocations.put(location, locations = new ArrayList());
				}
				boolean found = false;
				for (Iterator it = locations.iterator(); it.hasNext();) {
					IRepositoryLocation tmp = (IRepositoryLocation) it.next();
					if (url.startsWith(tmp.getUrl()) || tmp.getUrl().startsWith(url)) {
						location = tmp;
						found = true;
						break;
					}
				}
				if (!found) {
					location = new SVNRepositoryLocationWrapper(location,
							isFile ? url.substring(0, url.lastIndexOf('/')) : url);
					locations.add(location);
				}
			}
		}
		return location;
	}

	protected ILocalResource wrapUnexistingResource(IResource resource, String state, int mask) {
		return resource == null || resource.getType() == IResource.FILE
				? (ILocalResource) new SVNLocalFile(resource, SVNRevision.INVALID_REVISION_NUMBER,
						SVNRevision.INVALID_REVISION_NUMBER, state, IStateFilter.ST_NORMAL, mask, null, 0, null)
				: new SVNLocalFolder(resource, SVNRevision.INVALID_REVISION_NUMBER, SVNRevision.INVALID_REVISION_NUMBER,
						state, IStateFilter.ST_NORMAL, mask, null, 0, null);
	}

	protected String makeUrl(IResource resource, IRepositoryResource baseResource) {
		if (resource.getType() == IResource.PROJECT) {
			return baseResource.getUrl();
		}
		IProject project = resource.getProject();
		String url = resource.getFullPath().toString();
		//truncating of the project name allow us to remap a content of project with name 'A' to the remote folder called 'B'
		return baseResource.getUrl() + "/" + url.substring(project.getFullPath().toString().length() + 1); //$NON-NLS-1$
	}

	protected SVNTeamProvider getConnectedProjectInformation(IProject project) {
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider == null) {
			String errMessage = SVNMessages.formatErrorString("Error_NotConnectedProject", //$NON-NLS-1$
					new String[] { project.getName() });
			throw new UnreportableException(errMessage);
		}
		if (!(provider instanceof SVNTeamProvider)) {
			String errMessage = SVNMessages.formatErrorString("Error_AnotherProvider", //$NON-NLS-1$
					new String[] { project.getName(), provider.getID() });
			throw new UnreportableException(errMessage);
		}

		return (SVNTeamProvider) provider;
	}

	protected void refreshLocalResourceImpl(IResource resource, int depth) {
		if (resource.getType() != IResource.FILE) {
			if (resource.getType() == IResource.PROJECT) {
				SVNTeamProvider info = (SVNTeamProvider) RepositoryProvider.getProvider(resource.getProject(),
						SVNTeamPlugin.NATURE_ID);
				if (info != null) {
					try {
						info.relocateResource();
					} catch (CoreException ex) {
						throw new RuntimeException(ex);
					}
				}
			}
			if (depth != IResource.DEPTH_ZERO) {
				Map map = (Map) localResources.get(resource);
				if (map != null) {
					for (Iterator it = map.keySet().iterator(); it.hasNext();) {
						refreshLocalResourceImpl((IResource) it.next(),
								depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : depth);
					}
				}
			}
			localResources.remove(resource);
		}
		switchedToUrls.remove(resource.getFullPath());
	}

	protected ILocalResource loadLocalResourcesSubTree(final IResource resource, int depth) throws Exception {
		SVNTeamProvider provider = (SVNTeamProvider) RepositoryProvider.getProvider(resource.getProject(),
				SVNTeamPlugin.NATURE_ID);
		if (provider == null || FileUtility.isSVNInternals(resource)) {
			return wrapUnexistingResource(resource, IStateFilter.ST_INTERNAL_INVALID, 0);
		}

		boolean isCacheEnabled = CoreExtensionsManager.instance()
				.getOptionProvider()
				.is(IOptionProvider.SVN_CACHE_ENABLED);
		if (!isCacheEnabled) {
			localResources.clear();
		}
		depth = isCacheEnabled ? depth : IResource.DEPTH_ZERO;

		ILocalResource retVal = null;
		boolean isLinked = FileUtility.isLinked(resource);
		IResource parent = resource.getParent();
		boolean parentExists = parent != null && parent.isAccessible();
		if (parentExists && !isLinked) {
			ILocalResource parentLocal = asLocalResourceImpl(parent, IResource.DEPTH_ONE);
			if (parentLocal == null || !SVNRemoteStorage.SF_NONSVN.accept(parentLocal)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(parentLocal)) {
				retVal = loadLocalResourcesSubTreeSVNImpl(provider, resource, depth);
			}
		}

		return retVal == null || IStateFilter.SF_UNVERSIONED.accept(retVal) && !IStateFilter.SF_IGNORED.accept(retVal)
				? loadUnversionedSubtree(resource, isLinked, depth)
				: retVal;
	}

	protected ILocalResource loadUnversionedSubtree(final IResource resource, final boolean isLinked, int depth)
			throws Exception {
		final ILocalResource[] tmp = new ILocalResource[1];
		/*
		 * Performance optimization: make an assumption that if resource is unversioned external then all its unversioned
		 * children are also unversioned externals (without traversing them once again)
		 */
		final boolean[] isUnversionedExternalParent = { false };
		FileUtility.visitNodes(resource, child -> {
			if (FileUtility.isSVNInternals(child)
					|| SVNRemoteStorage.this.canFetchStatuses(new File(FileUtility.getWorkingCopyPath(child)))) {
				return false;
			}
			ILocalResource parent = SVNRemoteStorage.this.getFirstExistingParentLocal(child);
			boolean parentIsSymlink = parent != null && (parent.getChangeMask() & ILocalResource.IS_SYMLINK) != 0;
			int parentCM = (parent != null
					? parent.getChangeMask() & (ILocalResource.IS_SWITCHED | ILocalResource.IS_FORBIDDEN)
					: 0) | (parentIsSymlink ? ILocalResource.IS_FORBIDDEN : 0);
			// if resource has unversioned parents it cannot be wrapped directly and it status should be calculated in other way
			String inheritedStatus = parentIsSymlink
					? IStateFilter.ST_IGNORED
					: SVNRemoteStorage.this.calculateUnversionedStatus(resource, isLinked);
			String textState = child == resource
					? inheritedStatus
					: SVNRemoteStorage.this.getDelegatedStatus(child,
							parentIsSymlink ? IStateFilter.ST_IGNORED : inheritedStatus, 0);
			int changeMask = parentCM;
			//FIXME sometime it fails to detect a symlink change mask and fails to delegate change mask status ILocalResource.IS_UNVERSIONED_EXTERNAL ?
			if (textState == IStateFilter.ST_NEW && child.getType() == IResource.FOLDER
					&& FileUtility.isSymlink(child)) {
				changeMask |= ILocalResource.IS_SYMLINK;
			}
			//if resource's parent is ignored but not external, then don't check this resource as it is ignored too
			if ((parent != null && !IStateFilter.SF_IGNORED_NOT_FORBIDDEN.accept(parent) || parent == null)
					&& textState == IStateFilter.ST_IGNORED && (changeMask & ILocalResource.IS_FORBIDDEN) == 0) {
				if (isUnversionedExternalParent[0] || SVNRemoteStorage.this.containsSVNMetaInChildren(resource)) {
					changeMask |= ILocalResource.IS_FORBIDDEN;
					if (!isUnversionedExternalParent[0] && child.equals(resource)) {
						isUnversionedExternalParent[0] = true;
					}
				}
			}

			ILocalResource retVal = SVNRemoteStorage.this.registerResource(child,
					SVNRevision.INVALID_REVISION_NUMBER, SVNRevision.INVALID_REVISION_NUMBER, textState,
					IStateFilter.ST_NORMAL, changeMask, null, -1, null);
			if (tmp[0] == null) {
				tmp[0] = retVal;
			}
			return true;
		}, depth, false);

		return tmp[0] != null ? tmp[0] : wrapUnexistingResource(resource, IStateFilter.ST_INTERNAL_INVALID, 0);
	}

	protected String calculateUnversionedStatus(final IResource resource, boolean isLinked) {
		String status = IStateFilter.ST_NOTEXISTS;
		if (isLinked) {
			status = IStateFilter.ST_LINKED;
		} else {
			IPath location = resource.getLocation();
			if (location != null && SVNRemoteStorage.isFileExists(location)) {
				// may be ignored ?
				status = IStateFilter.ST_IGNORED;
				if (!SVNUtility.isIgnored(resource)) {
					ILocalResource local = getCachedResource(resource);
					if (local != null) {
						return local.getStatus();
					}
					status = getTopLevelStatus(resource, IStateFilter.ST_NEW, 0);
				}
			}
		}
		return status;
	}

	public static boolean isFileExists(IPath location) {
		File file = location.toFile();
		if (file.exists()) {
			if (FileUtility.isWindows()) {
				// should be case sensitive on Windows OS also
				try {
					return file.getName().equals(file.getCanonicalFile().getName());
				} catch (IOException e) {
					return false;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	protected ILocalResource loadLocalResourcesSubTreeSVNImpl(SVNTeamProvider provider, IResource resource, int depth)
			throws Exception {
		IProject project = resource.getProject();
		IResource target = resource.getType() == IResource.FILE ? resource.getParent() : resource;

		IPath wcPath = project.getLocation();
		IPath resourcePath = target.getLocation();
		IPath requestedPath = resource.getLocation();
		if (wcPath == null || resourcePath == null || requestedPath == null) {
			return null;
		}
		String projectPath = wcPath.toString();
		int subPathStart = projectPath.length();

		// search for first parent which contains .svn folder
		//	only folders due to condition above - check for presence of SVN meta directly at the path...
		boolean hasSVNMeta = true;
		if (SVNUtility.isPriorToSVN17()) {
			while (!this.canFetchStatuses(resourcePath)) {
				if (target == null || target.getType() == IResource.PROJECT) {
					return null;
				}
				hasSVNMeta = false;
				// load statuses non-recursively for the found parent
				resourcePath = resourcePath.removeLastSegments(1);
				target = target.getParent();
			}
		}
		IRepositoryResource baseResource = provider.getRepositoryResource();
		int offsetFromRoot = resourcePath.segmentCount() - wcPath.segmentCount();
		SVNDepth svnDepth = depth == IResource.DEPTH_ZERO
				? SVNDepth.EMPTY
				: depth == IResource.DEPTH_ONE ? SVNDepth.IMMEDIATES : SVNDepth.INFINITY;
		svnDepth = offsetFromRoot < 1
				|| !CoreExtensionsManager.instance().getOptionProvider().is(IOptionProvider.SVN_CACHE_ENABLED)
						? SVNDepth.IMMEDIATES
						: svnDepth;
		SVNChangeStatus[] statuses = getStatuses(resourcePath.toString(), svnDepth);
		String desiredUrl = makeUrl(target, baseResource);
		SVNChangeStatus[][] loadTargets = new SVNChangeStatus[1][];
		ILocalResource retVal = fillCache(statuses, desiredUrl, resource, subPathStart, requestedPath, loadTargets);

		if (statuses.length == 1 && !localResources.containsKey(target)) {
			// the caching is done for the folder if it is empty
			localResources.put(target, new HashMap());
		}

		statuses = loadTargets[0];
		if (retVal != null && hasSVNMeta && statuses.length > 1 && depth != IResource.DEPTH_ZERO
				&& CoreExtensionsManager.instance().getOptionProvider().is(IOptionProvider.SVN_CACHE_ENABLED)) {
			fetchQueue.push(new SvnChange(statuses, target));
		}

		return retVal;
	}

	protected boolean canFetchStatuses(IPath path) {
		return path.append(SVNUtility.getSVNFolderName()).toFile().exists();
	}

	protected boolean canFetchStatuses(File file) {
		return new File(file, SVNUtility.getSVNFolderName()).exists();
	}

	protected SVNChangeStatus[] getStatuses(String path, SVNDepth depth) throws Exception {
		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			SVNChangeStatus[] statuses = SVNUtility.status(proxy, path, depth,
					ISVNConnector.Options.INCLUDE_UNCHANGED | ISVNConnector.Options.INCLUDE_IGNORED,
					new SVNNullProgressMonitor());
			SVNUtility.reorder(statuses, true);
			return statuses;
		}
		// FIXME The only discovered reason of SBV-3557 defect is that some
		// folders under the ignored path contains completely or partially
		// empty .svn folders. May be due to using external tools.
		// So, suppress throwing "Path is not a working copy directory" exception.
		// 155007 is error code for "Path is not a working copy directory".
		catch (SVNConnectorException cwe) {
			if (cwe.getErrorId() == SVNErrorCodes.wcCleanupRequired) {
				// no way to read statuses, return some fake for now...
				return new SVNChangeStatus[] { new SVNChangeStatus(path, "", SVNEntry.Kind.DIR, 0, 0, 0, "",
						SVNEntryStatus.Kind.MODIFIED, SVNEntryStatus.Kind.NORMAL, SVNEntryStatus.Kind.NORMAL,
						SVNEntryStatus.Kind.NORMAL, false, false, false, null, null, 0, 0, SVNEntry.Kind.DIR, "", false,
						false, null, null) };
			}
			if (cwe.getErrorId() != SVNErrorCodes.wcNotDirectory && cwe.getErrorId() != SVNErrorCodes.wcPathNotFound) { // check if there is just nothing to report, since who knows what node's statuses were asked this time around...
				throw cwe;
			}
			return new SVNChangeStatus[0];
		} finally {
			proxy.dispose();
		}
	}

	/*
	 * Note that this method can modify statuses array
	 * (it even can make some statuses to null) passed as input parameter.
	 * This can happen for external definitions.
	 */
	protected ILocalResource fillCache(SVNChangeStatus[] statuses, String desiredUrl, IResource resource,
			int subPathStart, IPath requestedPath, SVNChangeStatus[][] loadTargets) {
		IProject project = resource.getProject();

		ILocalResource retVal = null;

		HashMap<IPath, SVNChangeStatus> lTargetsTmp = new HashMap<>();
		ISVNConnector proxy = null;
		try {
			for (int i = 0; i < statuses.length; i++) {
				SVNEntry.Kind nodeKind = SVNUtility.getNodeKind(statuses[i].path, statuses[i].nodeKind, true);
				// deleted in the branch then committed, deleted in the trunk then committed and then during merge we have a node with an unknown node kind but in the "tree conflict" state
				//	while the tree conflict flag isn't reported and some of SVN client calls knows nothing about the node itself, it still prevents commits.
				//	So, in order to solve the problem we should recognize the node as conflicting and allow to perform "revert" or "mark as merged" for it.
				if (statuses[i].hasConflict && statuses[i].treeConflicts == null || nodeKind == SVNEntry.Kind.NONE) {
					if (proxy == null) {
						proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
					}
					final SVNConflictDescriptor[][] treeConflicts = new SVNConflictDescriptor[1][];
					try {
						proxy.getInfo(new SVNEntryRevisionReference(statuses[i].path), SVNDepth.EMPTY,
								ISVNConnector.Options.FETCH_ACTUAL_ONLY, null, info -> treeConflicts[0] = info.treeConflicts, new SVNNullProgressMonitor());
					} catch (Exception ex) {
						// ignore
					}
					statuses[i].treeConflicts = treeConflicts[0];
				}
				if (nodeKind == SVNEntry.Kind.NONE && statuses[i].treeConflicts == null) {
					continue;
				}

				String fsNodePath = statuses[i].path;
				String nodePath = statuses[i].path;

				nodePath = nodePath.length() >= subPathStart ? nodePath.substring(subPathStart) : ""; //$NON-NLS-1$
				if (nodePath.length() > 0 && nodePath.charAt(nodePath.length() - 1) == '/') {
					nodePath = nodePath.substring(0, nodePath.length() - 1);
				} else if (i > 0 && nodePath.trim().length() == 0) {
					// Debug code was removed. We already have all information about the JavaSVN bug.
					continue;
				}

				IResource tRes = null;
				boolean isSymlink = false;
				tRes = project.findMember(nodePath, true);
				if (tRes != null && tRes.getType() != nodeKind.id && FileUtility.isSymlink(tRes)) { // FILE instead of SYMLINK: SVN Kit failure?
					nodeKind = SVNEntry.Kind.SYMLINK;
				}
				if (nodeKind == SVNEntry.Kind.SYMLINK) { //symlink, if reported
					isSymlink = true;
				}
				if (tRes == null) {
					if (nodeKind == SVNEntry.Kind.DIR) {
						if (nodePath.length() == 0) {
							tRes = project;
						} else {
							tRes = project.getFolder(new Path(nodePath));
						}
					} else {
						tRes = project.getFile(new Path(nodePath));
					}
				}
				if (nodeKind == SVNEntry.Kind.SYMLINK) {
					nodeKind = tRes.getType() == IResource.FILE ? SVNEntry.Kind.FILE : SVNEntry.Kind.DIR;
				}
				if (new Path(statuses[i].path).equals(requestedPath)
						&& (resource.getType() > IResource.FOLDER || resource.getType() == nodeKind.id)) {//if nodekind not equals do not use default resource
					tRes = resource;
				}
				String tDesiredUrl = tRes.getFullPath()
						.removeFirstSegments(resource.getFullPath().segmentCount())
						.toString();
				tDesiredUrl = tDesiredUrl.length() > 0 ? desiredUrl + "/" + tDesiredUrl : desiredUrl;

				ILocalResource local = getCachedResource(tRes);
				if (local == null) {
					if (nodeKind == SVNEntry.Kind.DIR) {
						lTargetsTmp.put(tRes.getFullPath(), statuses[i]);
					}
					if (tRes.getParent() != null && lTargetsTmp.containsKey(tRes.getParent().getFullPath())) {
						lTargetsTmp.remove(tRes.getParent().getFullPath());
					}

					ILocalResource parent = getFirstExistingParentLocal(tRes);

					if (parent != null && (parent.getChangeMask()
							& (ILocalResource.IS_SYMLINK | ILocalResource.IS_FORBIDDEN)) != 0) {
						local = registerUnversionedResource(tRes, ILocalResource.IS_FORBIDDEN);
						continue;
					}

					boolean isSVNExternals = false;
					if (statuses[i].textStatus == SVNEntryStatus.Kind.EXTERNAL) {
						isSVNExternals = true;
						/*
						 * If there is an SVNEntryStatus.Kind.EXTERNAL status, then there is no info in the resource's real status,
						 * and that is why we do reload it explicitly
						 */
						statuses[i] = SVNUtility.getSVNInfoForNotConnected(tRes);
						if (statuses[i] == null) {
							local = registerUnversionedResource(tRes, ILocalResource.IS_SVN_EXTERNALS);
							if (tRes == resource) {
								retVal = local;
							}
							continue;
						}
					} else if (i == 0 && statuses[i].url != null
							&& !SVNUtility.decodeURL(statuses[i].url).startsWith(tDesiredUrl)
							&& tRes.getParent().getType() != IResource.ROOT) {
						ILocalResource tLocalParent = getCachedResource(tRes.getParent());
						if (tLocalParent == null || (tLocalParent.getChangeMask() & ILocalResource.IS_SWITCHED) == 0) {
							if (proxy == null) {
								proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
							}
							try {
								SVNChangeStatus[] tStats = SVNUtility.status(proxy,
										FileUtility.getWorkingCopyPath(tRes.getParent()), SVNDepth.IMMEDIATES,
										ISVNConnector.Options.INCLUDE_UNCHANGED, new SVNNullProgressMonitor());
								for (SVNChangeStatus st : tStats) {
									if (st.path.equals(statuses[i].path)) {
										isSVNExternals = st.textStatus == SVNEntryStatus.Kind.EXTERNAL; // why is it UNVERSIONED if parent is new???
										break;
									}
								}
							} catch (Exception ex) {
								// ignore
							}
						}
					}

					// get the IS_COPIED flag by parent node (it is not fetched for deletions)
					boolean forceCopied = parent != null && parent.isCopied();
					int changeMask = SVNRemoteStorage.getChangeMask(statuses[i].textStatus, statuses[i].propStatus,
							forceCopied | statuses[i].isCopied, statuses[i].isSwitched, isSVNExternals);
					if (isSymlink) {
						changeMask |= ILocalResource.IS_SYMLINK;
					}
					if (statuses[i].wcLock != null) {
						changeMask |= ILocalResource.IS_LOCKED;
					}
					if (nodeKind == SVNEntry.Kind.NONE && statuses[i].treeConflicts != null) {
						changeMask |= ILocalResource.TREE_CONFLICT_UNKNOWN_NODE_KIND;
					}

					//check file external
					if (statuses[i].isFileExternal) {
						changeMask |= ILocalResource.IS_SWITCHED;
					}

					String textStatus = statuses[i].treeConflicts != null
							? IStateFilter.ST_CONFLICTING
							: SVNRemoteStorage.getTextStatusString(statuses[i].propStatus, statuses[i].textStatus,
									false);
					String propStatus = SVNRemoteStorage.getPropStatusString(statuses[i].propStatus);

					/*
					 * If folder is unversioned but contains in one of its children .svn folder,
					 * then we consider this folder as unversioned folder created by external definition and
					 * make its status in corresponding way, i.e. Ignored.
					 */
					if (textStatus == IStateFilter.ST_NEW && nodeKind == SVNEntry.Kind.DIR) {
						if (tRes.getLocation() != null && this.canFetchStatuses(tRes.getLocation())) { // externals root
							continue;
						} else if (containsSVNMetaInChildren(tRes)) {
							local = registerUnversionedResource(tRes, ILocalResource.IS_SVN_EXTERNALS);
							continue;
						}
					}

					if (!statuses[i].isSwitched && statuses[i].url != null
							&& !SVNUtility.decodeURL(statuses[i].url).startsWith(tDesiredUrl)) {
						changeMask |= ILocalResource.IS_SWITCHED;
					}

					if ((changeMask & ILocalResource.IS_SWITCHED) != 0) {
						// statuses[i].url == null when ILocalResource.IS_SWITCHED flag is set ??? Most likely the SVN client library issue in either one: url or state representation.
						//	So, for now just avoid NPE here and remove the switched flag.
						if (statuses[i].url != null) {
							switchedToUrls.put(tRes.getFullPath(), SVNUtility.decodeURL(statuses[i].url));
						} else {
							changeMask ^= ILocalResource.IS_SWITCHED;
						}
					}

					if (textStatus == IStateFilter.ST_DELETED
							&& (nodeKind == SVNEntry.Kind.FILE || !SVNUtility.isPriorToSVN17())
							&& new File(statuses[i].path).exists()) {
						textStatus = IStateFilter.ST_PREREPLACED;
					}

					if (FileUtility.isLinked(tRes)) {
						textStatus = IStateFilter.ST_LINKED;
					} else if (textStatus != IStateFilter.ST_OBSTRUCTED
							&& statuses[i].textStatus == org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind.UNVERSIONED) {
						textStatus = getDelegatedStatus(tRes, IStateFilter.ST_NEW, changeMask);
					}

					if (textStatus == IStateFilter.ST_NEW && nodeKind == SVNEntry.Kind.DIR
							&& this.canFetchStatuses(new Path(fsNodePath))) { // still, could be the case even with the SVN 1.7 working copy
						textStatus = IStateFilter.ST_OBSTRUCTED;
					}

					// fetch revision for "copied from"
					long revision = statuses[i].lastChangedRevision == SVNRevision.INVALID_REVISION_NUMBER
							&& (changeMask & ILocalResource.IS_COPIED) != 0
									? statuses[i].revision
									: statuses[i].lastChangedRevision;
					local = registerResource(tRes, revision, statuses[i].revision, textStatus, propStatus, changeMask,
							statuses[i].lastCommitAuthor, statuses[i].lastChangedDate,
							statuses[i].treeConflicts == null || statuses[i].treeConflicts.length == 0
									? null
									: statuses[i].treeConflicts[0]);
				}

				if (tRes == resource) {
					retVal = local;
				}
			}
			loadTargets[0] = lTargetsTmp.values().toArray(new SVNChangeStatus[lTargetsTmp.size()]);
		} finally {
			if (proxy != null) {
				proxy.dispose();
			}
		}

		return retVal;
	}

	/*
	 * Contain SVN meta in one of its sub directories
	 */
	protected boolean containsSVNMetaInChildren(IResource resource) {
		if (SVNUtility.isIgnored(resource)) {
			return false;
		}
		boolean hasSVNMeta = false;
		if (resource.getType() == IResource.FOLDER && resource.getLocation() != null) {
			File folder = resource.getLocation().toFile();
			do {
				/*
				 * return only the first found folder,
				 * if folder is found then don't process other resources
				 */
				final boolean[] check = { true };
				String[] children = folder.list((dir, name) -> {
					boolean accept = false;
					if (check[0] && (accept = new File(dir, name).isDirectory())) {
						check[0] = false;
					}
					return accept;
				});
				folder = children != null && children.length > 0 ? new File(folder, children[0]) : null;
			} while (folder != null && !(hasSVNMeta = this.canFetchStatuses(folder)));
		}
		return hasSVNMeta;
	}

	protected String getDelegatedStatus(IResource resource, String status, int mask) {
		// calculate applicability of delegated status
		if (IStateFilter.SF_LINKED.accept(resource, status, mask) || FileUtility.isLinked(resource)) {
			return IStateFilter.ST_LINKED;
		}
		if (IStateFilter.SF_OBSTRUCTED.accept(resource, status, mask)) {
			return IStateFilter.ST_OBSTRUCTED;
		}
		if (IStateFilter.ST_IGNORED == status) {
			return IStateFilter.ST_IGNORED;
		}
		return getTopLevelStatus(resource, status, mask);
	}

	protected String getTopLevelStatus(IResource resource, String status, int mask) {
		ILocalResource topLevel = getFirstExistingParentLocal(resource);
		if (topLevel != null) {
			String topLevelStatus = topLevel.getStatus();
			IResource topLevelResource = topLevel.getResource();
			if (IStateFilter.SF_OBSTRUCTED.accept(topLevelResource, topLevelStatus, mask)
					|| IStateFilter.SF_LINKED.accept(topLevelResource, topLevelStatus, mask)
					|| IStateFilter.SF_IGNORED.accept(topLevelResource, topLevelStatus, mask)) {
				return topLevelStatus;
			}
		}
		return status;
	}

	protected ILocalResource registerUnversionedResource(IResource resource, int changeMask) {
		return registerResource(resource, SVNRevision.INVALID_REVISION_NUMBER, SVNRevision.INVALID_REVISION_NUMBER,
				IStateFilter.ST_IGNORED, IStateFilter.ST_NORMAL, changeMask, null, 0, null);
	}

	protected ILocalResource registerResource(IResource current, long revision, long baseRevision, String textStatus,
			String propStatus, int changeMask, String author, long date, SVNConflictDescriptor treeConflictDescriptor) {
		SVNLocalResource local = null;

		if (IStateFilter.SF_OBSTRUCTED.accept(current, textStatus, changeMask)) {
			try {
				IIgnoreRecommendations[] ignores = CoreExtensionsManager.instance().getIgnoreRecommendations();
				for (IIgnoreRecommendations ignore : ignores) {
					if (ignore.isAcceptableNature(current) && ignore.isOutput(current)) {
						IPath location = current.getProject().getLocation();
						if (location != null) {
							String projectPath = location.removeLastSegments(1).toString();
							File checkedResource = new File(projectPath + current.getFullPath().toString());
							textStatus = !checkedResource.exists()
									? IStateFilter.ST_NOTEXISTS
									: IStateFilter.ST_IGNORED;
							// Bug fix: incorrect path calculation; thanks to Panagiotis Korros
							IPath invalidMetaPath = new Path(projectPath + current.getFullPath().toString())
									.append(SVNUtility.getSVNFolderName());
							FileUtility.deleteRecursive(invalidMetaPath.toFile());
						}
						break;
					}
				}
			} catch (CoreException e1) {
				// Cannot detect output folder due to some reasons. So, do not process the resource.
			}
		}

		if (IStateFilter.SF_UNVERSIONED.accept(current, textStatus, changeMask)
				&& !(IStateFilter.SF_PREREPLACEDREPLACED.accept(current, textStatus, changeMask)
						|| IStateFilter.SF_DELETED.accept(current, textStatus, changeMask))
				|| IStateFilter.SF_LINKED.accept(current, textStatus, changeMask)) {
			revision = SVNRevision.INVALID_REVISION_NUMBER;
			author = null;
			date = -1;
		}

		local = current instanceof IContainer
				? (SVNLocalResource) new SVNLocalFolder(current, revision, baseRevision, textStatus, propStatus,
						changeMask, author, date, treeConflictDescriptor)
				: new SVNLocalFile(current, revision, baseRevision, textStatus, propStatus, changeMask, author, date,
						treeConflictDescriptor);

		setCachedResource(local);

		if (current.getType() == IResource.PROJECT && !changeMonitorMap.containsKey(current)) {
			File wcDB = FileUtility.findWCDB(FileUtility.getResourcePath(current).toFile());
			if (wcDB != null) {
				changeMonitorMap.put(current, wcDB);
			}
		}

		return local;
	}

	protected ILocalResource getFirstExistingParentLocal(IResource node) {
		IResource parent = node.getParent();
		if (parent == null) {
			return null;
		}
		ILocalResource local = getCachedResource(parent);
		if (local != null) {
			return local;
		}
		return getFirstExistingParentLocal(parent);

	}

	protected String deserializeStatus(String status) {
		if ("null".equals(status)) { //$NON-NLS-1$
			return IStateFilter.ST_NOTEXISTS;
		} else if (IStateFilter.ST_IGNORED.equals(status)) {
			return IStateFilter.ST_IGNORED;
		} else if (IStateFilter.ST_NEW.equals(status)) {
			return IStateFilter.ST_NEW;
		} else if (IStateFilter.ST_ADDED.equals(status)) {
			return IStateFilter.ST_ADDED;
		} else if (IStateFilter.ST_NORMAL.equals(status)) {
			return IStateFilter.ST_NORMAL;
		} else if (IStateFilter.ST_MODIFIED.equals(status)) {
			return IStateFilter.ST_MODIFIED;
		} else if (IStateFilter.ST_CONFLICTING.equals(status)) {
			return IStateFilter.ST_CONFLICTING;
		} else if (IStateFilter.ST_DELETED.equals(status)) {
			return IStateFilter.ST_DELETED;
		} else if (IStateFilter.ST_MISSING.equals(status)) {
			return IStateFilter.ST_MISSING;
		} else if (IStateFilter.ST_OBSTRUCTED.equals(status)) {
			return IStateFilter.ST_OBSTRUCTED;
		} else if (IStateFilter.ST_PREREPLACED.equals(status)) {
			return IStateFilter.ST_PREREPLACED;
		} else if (IStateFilter.ST_REPLACED.equals(status)) {
			return IStateFilter.ST_REPLACED;
		}
		throw new RuntimeException(SVNMessages.getErrorString("Error_UnknownStatus")); //$NON-NLS-1$
	}

	public static String getCompoundStatusString(SVNEntryStatus.Kind propStatus, SVNEntryStatus.Kind textStatus,
			boolean isRemoteStatus) {
		String textStr = SVNRemoteStorage.getTextStatusString(propStatus, textStatus, isRemoteStatus);
		String propStr = SVNRemoteStorage.getPropStatusString(propStatus);
		return SVNRemoteStorage.getCompoundStatusString(textStr, propStr);
	}

	public static String getCompoundStatusString(String textStatus, String propStatus) {
		String status = textStatus;
		if (propStatus == IStateFilter.ST_CONFLICTING) {
			status = IStateFilter.ST_CONFLICTING;
		} else if (textStatus == IStateFilter.ST_NORMAL && propStatus == IStateFilter.ST_MODIFIED) {
			status = IStateFilter.ST_MODIFIED;
		}
		return status;
	}

	public static String getTextStatusString(SVNEntryStatus.Kind propKind, SVNEntryStatus.Kind textKind,
			boolean isRemoteStatus) {
		String status = IStateFilter.ST_NORMAL;
		switch (textKind) {
			case IGNORED: {
				status = IStateFilter.ST_IGNORED;
				break;
			}
			case UNVERSIONED: {
				status = isRemoteStatus ? IStateFilter.ST_NOTEXISTS : IStateFilter.ST_NEW;
				break;
			}
			case ADDED: {
				status = IStateFilter.ST_ADDED;
				break;
			}
			case DELETED: {
				status = IStateFilter.ST_DELETED;
				break;
			}
			case MISSING: {
				status = IStateFilter.ST_MISSING;
				break;
			}
			case CONFLICTED: {
				status = isRemoteStatus ? IStateFilter.ST_MODIFIED : IStateFilter.ST_CONFLICTING;
				break;
			}
			case MERGED:
			case MODIFIED: {
				status = IStateFilter.ST_MODIFIED;
				break;
			}
			case OBSTRUCTED: {
				status = IStateFilter.ST_OBSTRUCTED;
				break;
			}
			case REPLACED: {
				status = IStateFilter.ST_REPLACED;
				break;
			}
			case NONE: {
				if (!isRemoteStatus && propKind == SVNEntryStatus.Kind.NONE) {
					status = IStateFilter.ST_NOTEXISTS;
				}
				break;
			}
			default:
				break;
		}
		return status;
	}

	protected static String getPropStatusString(SVNEntryStatus.Kind propKind) {
		String status = IStateFilter.ST_NORMAL;
		if (propKind == SVNEntryStatus.Kind.CONFLICTED) {
			status = IStateFilter.ST_CONFLICTING;
		} else if (propKind == SVNEntryStatus.Kind.MODIFIED) {
			status = IStateFilter.ST_MODIFIED;
		}
		return status;
	}

	protected static int getChangeMask(SVNEntryStatus.Kind textStatus, SVNEntryStatus.Kind propKind, boolean isCopied,
			boolean isSwitched, boolean isSVNExternals) {
		int changeMask = ILocalResource.NO_MODIFICATION;
		if (isCopied) {
			changeMask |= ILocalResource.IS_COPIED;
		}
		if (isSwitched) {
			changeMask |= ILocalResource.IS_SWITCHED;
		}
		if (isSVNExternals) {
			changeMask |= ILocalResource.IS_SVN_EXTERNALS;
		}
		return changeMask;
	}

	private static class SvnChange implements IQueuedElement<SvnChange> {

		public final SVNChangeStatus[] st;

		public final IResource target;

		public SvnChange(SVNChangeStatus[] st, IResource target) {
			this.st = st;
			this.target = target;
		}

		/**
		 * Can't be skipped because we don't look into SVNChangeStatus elements for equality (it does not have a simple and fast equals())
		 */
		@Override
		public boolean canSkip() {
			return false;
		}

		/**
		 * Not re-implemented by purpose, to make every change different
		 */
		@Override
		public final boolean equals(Object obj) {
			return super.equals(obj);
		}

		/**
		 * Not re-implemented by purpose, to make every change different
		 */
		@Override
		public final int hashCode() {
			return super.hashCode();
		}

		@Override
		public boolean canMerge(SvnChange d) {
			return target.equals(d.target);
		}

		@Override
		public SvnChange merge(SvnChange d) {
			SVNChangeStatus[] result = new SVNChangeStatus[st.length + d.st.length];
			System.arraycopy(st, 0, result, 0, st.length);
			System.arraycopy(d.st, 0, result, st.length, d.st.length);
			return new SvnChange(result, target);
		}

		@Override
		public String toString() {
			StringBuilder out = new StringBuilder();
			out.append("SvnChange [");
			out.append("size: ").append(st.length);
			out.append(", target: ");
			out.append(target);
			out.append("]");
			return out.toString();
		}
	}

	private SVNRemoteStorage() {
		localResources = new HashMap(500);
		switchedToUrls = Collections.synchronizedMap(new LinkedHashMap());
		externalsLocations = new HashMap();
		resourceStateListeners = new HashMap<>();
		fetchQueue = new AsynchronousActiveQueue<>("Operation_UpdateSVNCache",
				(monitor, op, record) -> {
					SVNChangeStatus[] st = record.st;
					IResource target = record.target;
					IProject prj = target.getProject();
					IPath location = prj.getLocation();
					if (location != null) {
						int projectEnd = location.toString().length();
						for (int i = 0; i < st.length && !monitor.isCanceled(); i++) {
							ProgressMonitorUtility.progress(monitor, i, IProgressMonitor.UNKNOWN);
							if (st[i] != null && st[i].nodeKind == SVNEntry.Kind.DIR
									&& st[i].path.length() > projectEnd) {
								IResource folder = prj.getFolder(new Path(st[i].path.substring(projectEnd)));
								ProgressMonitorUtility.setTaskInfo(monitor, op, folder.getFullPath().toString());
								ILocalFolder local = (ILocalFolder) SVNRemoteStorage.this.asLocalResource(folder);
								if (!IStateFilter.SF_INTERNAL_INVALID.accept(local)) {
									local.getChildren();
								}
							}
						}
					}
				}, false);
		eventQueue = new AsynchronousActiveQueue<>("Operation_UpdateSVNCache",
				(monitor, op, event) -> {
IResourceStatesListener[] listeners;
synchronized (resourceStateListeners) {
				List<IResourceStatesListener> listenersList = resourceStateListeners.get(event.getClass());
				if (listenersList == null || listenersList.size() == 0) {
					return;
				}
				listeners = listenersList.toArray(new IResourceStatesListener[listenersList.size()]);
}
for (int i = 0; i < listeners.length && !monitor.isCanceled(); i++) {
				listeners[i].resourcesStateChanged(event);
}
}, true);
		lastMonitorTime = System.currentTimeMillis();
		changeMonitorMap = new HashMap<>();
	}

	private static final IStateFilter SF_NONSVN = new IStateFilter.AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_PREREPLACED || state == IStateFilter.ST_NEW
					|| state == IStateFilter.ST_IGNORED || state == IStateFilter.ST_NOTEXISTS
					|| state == IStateFilter.ST_LINKED || state == IStateFilter.ST_OBSTRUCTED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

}
