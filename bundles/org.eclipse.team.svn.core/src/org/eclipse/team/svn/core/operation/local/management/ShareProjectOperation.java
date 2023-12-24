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

package org.eclipse.team.svn.core.operation.local.management;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.SVNTeamProjectMapper;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConnectorCancelException;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Share project operation implementation
 * 
 * @author Alexander Gurov
 */
public class ShareProjectOperation extends AbstractWorkingCopyOperation {
	public static final int LAYOUT_DEFAULT = 0;

	public static final int LAYOUT_SINGLE = 1;

	public static final int LAYOUT_MULTIPLE = 2;

	public interface IFolderNameMapper {
		String getRepositoryFolderName(IProject project);
	}

	protected IRepositoryLocation location;

	protected IFolderNameMapper mapper;

	protected int shareLayout;

	protected String rootName;

	protected boolean managementFoldersEnabled;

	protected String commitComment;

	protected IShareProjectPrompt shareProjectPrompt;

	protected boolean ignoreExternals;

	public ShareProjectOperation(IProject[] projects, IRepositoryLocation location, IFolderNameMapper mapper) {
		this(projects, location, mapper, null);
	}

	public ShareProjectOperation(IProject[] projects, IRepositoryLocation location, IFolderNameMapper mapper,
			String commitComment) {
		this(projects, location, mapper, null, ShareProjectOperation.LAYOUT_DEFAULT, true, commitComment);
	}

	public ShareProjectOperation(IProject[] projects, IRepositoryLocation location, IFolderNameMapper mapper,
			String rootName, int shareLayout, boolean managementFoldersEnabled) {
		this(projects, location, mapper, rootName, shareLayout, managementFoldersEnabled, null);
	}

	public ShareProjectOperation(IProject[] projects, IRepositoryLocation location, IFolderNameMapper mapper,
			String rootName, int shareLayout, boolean managementFoldersEnabled, String commitComment) {
		super("Operation_ShareProject", SVNMessages.class, projects); //$NON-NLS-1$
		this.mapper = mapper;
		this.location = location;
		this.shareLayout = shareLayout;
		this.rootName = rootName;
		this.managementFoldersEnabled = managementFoldersEnabled;
		this.commitComment = commitComment;
	}

	public void setIngoreExternals(boolean ignoreExternals) {
		this.ignoreExternals = ignoreExternals;
	}

	public void setSharePrompt(IShareProjectPrompt prompt) {
		shareProjectPrompt = prompt;
	}

	public static String getTargetUrl(IRepositoryLocation location, int shareLayout, String projectName,
			String rootName, boolean managementFoldersEnabled) {
		return SVNUtility.normalizeURL(ShareProjectOperation.getTargetUrlImpl(location, shareLayout, projectName,
				rootName, managementFoldersEnabled));
	}

	public static String getDefaultComment(IProject project, IRepositoryResource remote) {
		return BaseMessages.format(SVNMessages.Operation_ShareProject_DefaultComment,
				new String[] { project.getName(), SVNUtility.encodeURL(remote.getUrl()) });
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource[] resources = operableData();

		HashMap<IResource, IRepositoryContainer> localResourceToRemoteRepoMap = new HashMap<>();
		for (IResource element : resources) {
			String url = ShareProjectOperation.getTargetUrl(location, shareLayout,
					mapper == null ? element.getName() : mapper.getRepositoryFolderName((IProject) element), rootName,
					managementFoldersEnabled);
			IRepositoryContainer remote = location.asRepositoryContainer(url, false);
			localResourceToRemoteRepoMap.put(element, remote);
		}

		if (commitComment == null) {
			commitComment = ""; //$NON-NLS-1$
			for (Iterator it = localResourceToRemoteRepoMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String commentPart = ShareProjectOperation.getDefaultComment((IProject) entry.getKey(),
						(IRepositoryContainer) entry.getValue());
				commitComment += commitComment.length() == 0 ? commentPart : "\n" + commentPart; //$NON-NLS-1$
			}
		}

		Set newRepositoryResources = null;
		switch (shareLayout) {
			case ShareProjectOperation.LAYOUT_DEFAULT: {
				newRepositoryResources = createDefaultRepositoryResourceSet(localResourceToRemoteRepoMap);
				break;
			}
			case ShareProjectOperation.LAYOUT_SINGLE: {
				newRepositoryResources = doSingleLayout(localResourceToRemoteRepoMap);
				break;
			}
			case ShareProjectOperation.LAYOUT_MULTIPLE: {
				newRepositoryResources = doMultipleLayout(localResourceToRemoteRepoMap);
				break;
			}
			default: {
				String message = getNationalizedString("Error_UnknownProjectLayoutType"); //$NON-NLS-1$
				throw new Exception(BaseMessages.format(message, new Object[] { String.valueOf(shareLayout) }));
			}
		}

		final HashSet existingRemoteProjects = new HashSet();
		for (Iterator it = localResourceToRemoteRepoMap.entrySet().iterator(); it.hasNext() && !monitor.isCanceled();) {
			final Map.Entry entry = (Map.Entry) it.next();
			this.protectStep(monitor1 -> {
				try {
					IRepositoryContainer remote = (IRepositoryContainer) entry.getValue();
					if (remote.getChildren().length > 0) {
						existingRemoteProjects.add(entry.getKey());
					}
				} catch (SVNConnectorException ex) {
					// do nothing
				}
			}, monitor, localResourceToRemoteRepoMap.size() * 2);
		}

		if (existingRemoteProjects.size() > 0 && shareProjectPrompt != null && !shareProjectPrompt
				.prompt((IProject[]) existingRemoteProjects.toArray(new IProject[existingRemoteProjects.size()]))) {
			throw new SVNConnectorCancelException(getNationalizedString("Error_ShareCanceled")); //$NON-NLS-1$
		}

		final ISVNConnector proxy = location.acquireSVNProxy();
		try {
			IRepositoryResource[] newOrderedRepositoryResources = getOrderedSet(newRepositoryResources);
			mkdir(proxy, newOrderedRepositoryResources, monitor);

			for (Iterator it = localResourceToRemoteRepoMap.entrySet().iterator(); it.hasNext()
					&& !monitor.isCanceled();) {
				final Map.Entry entry = (Map.Entry) it.next();
				this.protectStep(monitor1 -> {
					IRepositoryContainer remote = (IRepositoryContainer) entry.getValue();
					IProject project = (IProject) entry.getKey();

					File tempDir = existingRemoteProjects.contains(project)
							? ShareProjectOperation.this.createTempDirectory(project)
							: null;
					String checkoutTo = tempDir != null
							? tempDir.toString()
							: FileUtility.getWorkingCopyPath(project);
					long options = ignoreExternals
							? ISVNConnector.Options.IGNORE_EXTERNALS
							: ISVNConnector.Options.NONE;
					proxy.checkout(SVNUtility.getEntryRevisionReference(remote), checkoutTo, SVNDepth.INFINITY,
							options, new SVNProgressMonitor(ShareProjectOperation.this, monitor1, null));

					if (tempDir != null) {
						ShareProjectOperation.this.copySVNMeta(tempDir,
								FileUtility.getResourcePath(project).toFile());
						FileUtility.deleteRecursive(tempDir, monitor1);
					}

					SVNTeamProjectMapper.map(project, remote);
				}, monitor, localResourceToRemoteRepoMap.size() * 2);
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	protected Set<IRepositoryResource> createDefaultRepositoryResourceSet(Map local2remote) throws Exception {
		HashSet<IRepositoryResource> fullSet = new HashSet<>();
		for (Iterator it = local2remote.values().iterator(); it.hasNext();) {
			IRepositoryContainer remote = (IRepositoryContainer) it.next();
			IRepositoryResource[] resources = SVNUtility.makeResourceSet(remote.getRepositoryLocation().getRoot(),
					remote);
			fullSet.addAll(Arrays.asList(resources));
		}
		return fullSet;
	}

	protected Set doSingleLayout(Map local2remote) throws Exception {
		if (managementFoldersEnabled) {
			HashSet fullSet = new HashSet();
			for (Iterator it = local2remote.values().iterator(); it.hasNext();) {
				IRepositoryContainer remote = (IRepositoryContainer) it.next();
				IRepositoryResource[] resources = SVNUtility.makeResourceSet(remote.getRepositoryLocation().getRoot(),
						remote);
				fullSet.addAll(Arrays.asList(resources));
				IRepositoryContainer parent = (IRepositoryContainer) remote.getParent();
				fullSet.add(makeChild(parent, ShareProjectOperation.getTagsName(location)));
				fullSet.add(makeChild(parent, ShareProjectOperation.getBranchesName(location)));
			}
			return fullSet;
		}
		return createDefaultRepositoryResourceSet(local2remote);
	}

	protected Set doMultipleLayout(Map local2remote) throws Exception {
		if (managementFoldersEnabled) {
			HashSet fullSet = new HashSet();
			for (Iterator it = local2remote.values().iterator(); it.hasNext();) {
				IRepositoryContainer remote = (IRepositoryContainer) it.next();
				IRepositoryResource[] resources = SVNUtility.makeResourceSet(remote.getRepositoryLocation().getRoot(),
						remote);
				fullSet.addAll(Arrays.asList(resources));

				String targetUrl = ShareProjectOperation.getTargetUrl(location, shareLayout, "", //$NON-NLS-1$
						rootName, false);
				IRepositoryContainer parent = location.asRepositoryContainer(targetUrl, false);
				fullSet.add(makeChild(parent, ShareProjectOperation.getTagsName(location)));
				fullSet.add(makeChild(parent, ShareProjectOperation.getBranchesName(location)));
			}
			return fullSet;
		}
		return createDefaultRepositoryResourceSet(local2remote);
	}

	protected IRepositoryResource[] getOrderedSet(Set fullSet) {
		IRepositoryResource[] resources = (IRepositoryResource[]) fullSet
				.toArray(new IRepositoryResource[fullSet.size()]);
		Arrays.sort(resources, (arg0, arg1) -> {
			IRepositoryResource first = (IRepositoryResource) arg0;
			IRepositoryResource second = (IRepositoryResource) arg1;
			return first.getUrl().compareTo(second.getUrl());
		});
		return resources;
	}

	protected void mkdir(ISVNConnector proxy, IRepositoryResource[] resourceSet, IProgressMonitor monitor)
			throws Exception {
		ArrayList urlsList = new ArrayList();
		for (int i = 0; i < resourceSet.length && !monitor.isCanceled(); i++) {
			ProgressMonitorUtility.setTaskInfo(monitor, this, resourceSet[i].getUrl());
			if (!resourceSet[i].exists()) {
				urlsList.add(SVNUtility.encodeURL(resourceSet[i].getUrl()));
			}
			ProgressMonitorUtility.progress(monitor, IProgressMonitor.UNKNOWN, resourceSet.length);
		}
		String[] urls = (String[]) urlsList.toArray(new String[urlsList.size()]);
		proxy.mkdir(urls, commitComment, ISVNConnector.Options.INCLUDE_PARENTS, null,
				new SVNProgressMonitor(this, monitor, null));
	}

	protected IRepositoryContainer makeChild(IRepositoryContainer parent, String name) {
		return location.asRepositoryContainer(parent.getUrl() + "/" + name, false); //$NON-NLS-1$
	}

	protected File createTempDirectory(IProject project) {
		try {
			File tempDirectory = File.createTempFile("save_" + project.getName(), ".tmp", //$NON-NLS-1$ //$NON-NLS-2$
					FileUtility.getResourcePath(project).toFile().getParentFile());
			tempDirectory.deleteOnExit();
			tempDirectory.delete();
			return tempDirectory;
		} catch (IOException ex) {
			String message = SVNMessages.formatErrorString("Error_CannotCheckOutMeta", //$NON-NLS-1$
					new String[] { String.valueOf(project.getName()) });
			throw new UnreportableException(message, ex);
		}
	}

	protected void copySVNMeta(File fromFolder, File toFolder) {
		if (!toFolder.isDirectory()) {
			return;
		}
		File[] tempFiles = fromFolder.listFiles();
		if (tempFiles != null) {
			for (File tempFile : tempFiles) {
				File renameToFile = new File(toFolder + "/" + tempFile.getName()); //$NON-NLS-1$
				if (!renameToFile.exists()) {
					tempFile.renameTo(renameToFile);
				} else if (tempFile.isDirectory()) {
					copySVNMeta(tempFile, renameToFile);
				}
			}
		}
	}

	protected static String getTargetUrlImpl(IRepositoryLocation location, int shareLayout, String projectName,
			String rootName, boolean managementFoldersEnabled) {
		String trunkName = managementFoldersEnabled ? "/" + ShareProjectOperation.getTrunkName(location) : ""; //$NON-NLS-1$ //$NON-NLS-2$
		switch (shareLayout) {
			case ShareProjectOperation.LAYOUT_DEFAULT: {
				return location.getUrl() + trunkName + "/" + projectName; //$NON-NLS-1$
			}
			case ShareProjectOperation.LAYOUT_SINGLE: {
				return location.getUrl() + "/" + projectName + trunkName; //$NON-NLS-1$
			}
			case ShareProjectOperation.LAYOUT_MULTIPLE: {
				return location.getUrl() + "/" + rootName + trunkName + "/" + projectName; //$NON-NLS-1$ //$NON-NLS-2$
			}
			default: {
				String message = SVNMessages.formatErrorString("Error_UnknownProjectLayoutType", //$NON-NLS-1$
						new String[] { String.valueOf(shareLayout) });
				throw new RuntimeException(message);
			}
		}
	}

	public static String getTrunkName(IRepositoryLocation location) {
		if (location.isStructureEnabled()) {
			return location.getTrunkLocation();
		}
		return SVNTeamPlugin.instance().getOptionProvider().getString(IOptionProvider.DEFAULT_TRUNK_NAME);
	}

	public static String getBranchesName(IRepositoryLocation location) {
		if (location.isStructureEnabled()) {
			return location.getBranchesLocation();
		}
		return SVNTeamPlugin.instance().getOptionProvider().getString(IOptionProvider.DEFAULT_BRANCHES_NAME);
	}

	public static String getTagsName(IRepositoryLocation location) {
		if (location.isStructureEnabled()) {
			return location.getTagsLocation();
		}
		return SVNTeamPlugin.instance().getOptionProvider().getString(IOptionProvider.DEFAULT_TAGS_NAME);
	}

}
