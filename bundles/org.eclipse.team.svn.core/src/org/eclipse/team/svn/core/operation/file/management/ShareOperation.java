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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.file.management;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.file.AbstractFileOperation;
import org.eclipse.team.svn.core.operation.file.IFileProvider;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Share selected folders with the specified repository
 * 
 * @author Alexander Gurov
 */
public class ShareOperation extends AbstractFileOperation {
	public static final int LAYOUT_DEFAULT = 0;

	public static final int LAYOUT_SINGLE = 1;

	public static final int LAYOUT_MULTIPLE = 2;

	public interface IFolderNameMapper {
		String getRepositoryFolderName(File folder);
	}

	protected IRepositoryLocation location;

	protected IFolderNameMapper mapper;

	protected String rootName;

	protected int shareLayout;

	protected boolean managementFoldersEnabled;

	protected String commitComment;

	protected boolean ignoreExternals;

	public ShareOperation(File[] files, IRepositoryLocation location, IFolderNameMapper mapper, String rootName,
			int shareLayout, boolean managementFoldersEnabled, String commitComment, boolean ignoreExternals) {
		super("Operation_ShareFile", SVNMessages.class, files); //$NON-NLS-1$
		this.location = location;
		this.mapper = mapper;
		this.rootName = rootName;
		this.shareLayout = shareLayout;
		this.managementFoldersEnabled = managementFoldersEnabled;
		this.commitComment = commitComment;
		this.ignoreExternals = ignoreExternals;
	}

	public ShareOperation(IFileProvider provider, IRepositoryLocation location, IFolderNameMapper mapper,
			String rootName, int shareLayout, boolean managementFoldersEnabled, String commitComment,
			boolean ignoreExternals) {
		super("Operation_ShareFile", SVNMessages.class, provider); //$NON-NLS-1$
		this.location = location;
		this.mapper = mapper;
		this.rootName = rootName;
		this.shareLayout = shareLayout;
		this.managementFoldersEnabled = managementFoldersEnabled;
		this.commitComment = commitComment;
		this.ignoreExternals = ignoreExternals;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		File[] folders = FileUtility.shrinkChildNodes(operableData(), true);
		HashMap<File, IRepositoryContainer> local2remote = new HashMap<>();
		for (File folder : folders) {
			String url = getTargetUrl(
					mapper == null ? folder.getName() : mapper.getRepositoryFolderName(folder),
					managementFoldersEnabled);
			IRepositoryContainer remote = location.asRepositoryContainer(url, false);
			local2remote.put(folder, remote);
		}
		if (commitComment == null) {
			commitComment = ""; //$NON-NLS-1$
			for (Iterator<?> it = local2remote.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String commentPart = ShareOperation.getDefaultComment((File) entry.getKey(),
						(IRepositoryContainer) entry.getValue());
				commitComment += commitComment.length() == 0 ? commentPart : "\n" + commentPart; //$NON-NLS-1$
			}
		}

		final ISVNConnector proxy = location.acquireSVNProxy();
		try {
			IRepositoryResource[] resourceSet = null;
			switch (shareLayout) {
				case ShareProjectOperation.LAYOUT_DEFAULT: {
					resourceSet = doDefaultLayout(local2remote);
					break;
				}
				case ShareProjectOperation.LAYOUT_SINGLE: {
					resourceSet = doSingleLayout(local2remote);
					break;
				}
				case ShareProjectOperation.LAYOUT_MULTIPLE: {
					resourceSet = doMultipleLayout(local2remote);
					break;
				}
				default: {
					String message = getNationalizedString("Error_UnknownProjectLayoutType"); //$NON-NLS-1$
					throw new Exception(
							BaseMessages.format(message, new Object[] { String.valueOf(shareLayout) }));
				}
			}

			mkdir(proxy, resourceSet, monitor);

			for (Iterator<?> it = local2remote.entrySet().iterator(); it.hasNext() && !monitor.isCanceled();) {
				final Map.Entry entry = (Map.Entry) it.next();
				this.protectStep(monitor1 -> {
					IRepositoryContainer remote = (IRepositoryContainer) entry.getValue();
					File local = (File) entry.getKey();
					long options = ignoreExternals
							? ISVNConnector.Options.IGNORE_EXTERNALS
							: ISVNConnector.Options.NONE;
					proxy.checkout(SVNUtility.getEntryRevisionReference(remote), local.getAbsolutePath(),
							SVNDepth.EMPTY, options, new SVNProgressMonitor(ShareOperation.this, monitor1, null));
				}, monitor, local2remote.size());
			}
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	public static String getDefaultComment(File folder, IRepositoryResource remote) {
		return BaseMessages.format(SVNMessages.Operation_ShareFile_DefaultComment,
				new String[] { folder.getName(), remote.getUrl() });
	}

	public String getTargetUrl(String projectName, boolean managementFoldersEnabled) {
		return SVNUtility.normalizeURL(getTargetUrlImpl(projectName, managementFoldersEnabled));
	}

	protected String getTargetUrlImpl(String projectName, boolean managementFoldersEnabled) {
		String trunkName = managementFoldersEnabled ? "/" + getTrunkName() : ""; //$NON-NLS-1$ //$NON-NLS-2$
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
				String message = getNationalizedString("Error_UnknownProjectLayoutType"); //$NON-NLS-1$
				throw new RuntimeException(
						BaseMessages.format(message, new Object[] { String.valueOf(shareLayout) }));
			}
		}
	}

	protected void mkdir(ISVNConnector proxy, IRepositoryResource[] resourceSet, IProgressMonitor monitor)
			throws Exception {
		ArrayList<String> urlsList = new ArrayList<>();
		for (int i = 0; i < resourceSet.length && !monitor.isCanceled(); i++) {
			ProgressMonitorUtility.setTaskInfo(monitor, this, resourceSet[i].getUrl());
			if (!resourceSet[i].exists()) {
				urlsList.add(SVNUtility.encodeURL(resourceSet[i].getUrl()));
			}
			ProgressMonitorUtility.progress(monitor, IProgressMonitor.UNKNOWN, resourceSet.length);
		}
		String[] urls = urlsList.toArray(new String[urlsList.size()]);
		proxy.mkdir(urls, commitComment, ISVNConnector.Options.INCLUDE_PARENTS, null,
				new SVNProgressMonitor(this, monitor, null));
	}

	protected IRepositoryResource[] doDefaultLayout(Map<File, IRepositoryContainer> local2remote) {
		HashSet<IRepositoryResource> fullSet = new HashSet<>();
		for (IRepositoryContainer remote : local2remote.values()) {
			IRepositoryResource[] resources = SVNUtility.makeResourceSet(remote.getRepositoryLocation().getRoot(),
					remote);
			fullSet.addAll(Arrays.asList(resources));
		}
		return getOrderedSet(fullSet);
	}

	protected IRepositoryResource[] doSingleLayout(Map<File, IRepositoryContainer> local2remote) {
		if (managementFoldersEnabled) {
			HashSet<IRepositoryResource> fullSet = new HashSet<>();
			for (IRepositoryContainer remote : local2remote.values()) {
				IRepositoryResource[] resources = SVNUtility.makeResourceSet(remote.getRepositoryLocation().getRoot(),
						remote);
				fullSet.addAll(Arrays.asList(resources));
				IRepositoryContainer parent = (IRepositoryContainer) remote.getParent();
				fullSet.add(makeChild(parent, getTagsName()));
				fullSet.add(makeChild(parent, getBranchesName()));
			}
			return getOrderedSet(fullSet);
		}
		return doDefaultLayout(local2remote);
	}

	protected IRepositoryResource[] doMultipleLayout(Map<File, IRepositoryContainer> local2remote) {
		if (managementFoldersEnabled) {
			HashSet<IRepositoryResource> fullSet = new HashSet<>();
			for (IRepositoryContainer remote : local2remote.values()) {
				IRepositoryResource[] resources = SVNUtility.makeResourceSet(remote.getRepositoryLocation().getRoot(),
						remote);
				fullSet.addAll(Arrays.asList(resources));

				String targetUrl = getTargetUrl("", false); //$NON-NLS-1$
				IRepositoryContainer parent = location.asRepositoryContainer(targetUrl, false);
				fullSet.add(makeChild(parent, getTagsName()));
				fullSet.add(makeChild(parent, getBranchesName()));
			}
			return getOrderedSet(fullSet);
		}
		return doDefaultLayout(local2remote);
	}

	protected IRepositoryContainer makeChild(IRepositoryContainer parent, String name) {
		return location.asRepositoryContainer(parent.getUrl() + "/" + name, false); //$NON-NLS-1$
	}

	protected IRepositoryResource[] getOrderedSet(Set<IRepositoryResource> fullSet) {
		IRepositoryResource[] resources = fullSet.toArray(new IRepositoryResource[fullSet.size()]);
		Arrays.sort(resources, (arg0, arg1) -> {
			IRepositoryResource first = (IRepositoryResource) arg0;
			IRepositoryResource second = (IRepositoryResource) arg1;
			return first.getUrl().compareTo(second.getUrl());
		});
		return resources;
	}

	protected String getTrunkName() {
		if (location.isStructureEnabled()) {
			return location.getTrunkLocation();
		}
		return SVNTeamPlugin.instance().getOptionProvider().getString(IOptionProvider.DEFAULT_TRUNK_NAME);
	}

	protected String getBranchesName() {
		if (location.isStructureEnabled()) {
			return location.getBranchesLocation();
		}
		return SVNTeamPlugin.instance().getOptionProvider().getString(IOptionProvider.DEFAULT_BRANCHES_NAME);
	}

	protected String getTagsName() {
		if (location.isStructureEnabled()) {
			return location.getTagsLocation();
		}
		return SVNTeamPlugin.instance().getOptionProvider().getString(IOptionProvider.DEFAULT_TAGS_NAME);
	}

}
