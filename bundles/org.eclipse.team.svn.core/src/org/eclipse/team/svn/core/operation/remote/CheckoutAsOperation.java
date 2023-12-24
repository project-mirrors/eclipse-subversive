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

package org.eclipse.team.svn.core.operation.remote;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamProjectMapper;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.SVNResourceRuleFactory;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Checkout content into the project which has the specified name
 * 
 * @author Alexander Gurov
 */
public class CheckoutAsOperation extends AbstractActionOperation {
	protected IProject project;

	protected IRepositoryResource resource;

	protected String projectLocation;

	protected List<IProject> overlappingProjects;

	protected SVNDepth depth;

	protected long options;

	protected RestoreProjectMetaOperation restoreOp;

	public CheckoutAsOperation(String projectName, IRepositoryResource resource, SVNDepth depth,
			boolean ignoreExternals) {
		this(projectName, resource, Platform.getLocation().toString(), depth, ignoreExternals);
	}

	public CheckoutAsOperation(String projectName, IRepositoryResource resource, boolean respectHierarchy,
			String location, SVNDepth depth, boolean ignoreExternals) {
		this(projectName, resource,
				location == null
						? Platform.getLocation().toString()
						: location + (respectHierarchy ? SVNUtility.getResourceParent(resource) : ""), //$NON-NLS-1$
				depth, ignoreExternals);
	}

	@Override
	public int getOperationWeight() {
		return 19;
	}

	public CheckoutAsOperation(String projectName, IRepositoryResource resource, String projectLocation, SVNDepth depth,
			boolean ignoreExternals) {
		this(projectName, resource, projectLocation, depth,
				ignoreExternals ? ISVNConnector.Options.IGNORE_EXTERNALS : ISVNConnector.Options.NONE);
	}

	public CheckoutAsOperation(String projectName, IRepositoryResource resource, String projectLocation, SVNDepth depth,
			long options) {
		super("Operation_CheckOutAs", SVNMessages.class); //$NON-NLS-1$
		projectName = FileUtility.formatResourceName(projectName);
		if (FileUtility.isCaseInsensitiveOS()) {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			IProject projectToCheckOut = null;
			for (IProject project2 : projects) {
				if (project2.getName().equalsIgnoreCase(projectName)) {
					projectToCheckOut = project2;
				}
			}
			project = projectToCheckOut != null
					? projectToCheckOut
					: ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		} else {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		}
		this.resource = resource;
		this.projectLocation = projectLocation;
		this.depth = depth;
		this.options = options & ISVNConnector.CommandMasks.CHECKOUT;
		overlappingProjects = new ArrayList<>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project2 : projects) {
			if (!FileUtility.isRemoteProject(project2)
					&& new Path(this.projectLocation).append(project.getName()).isPrefixOf(project2.getLocation())) {
				overlappingProjects.add(project2);
			}
		}
		overlappingProjects.add(project);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		IProject[] projects = overlappingProjects.toArray(new IProject[overlappingProjects.size()]);
		ISchedulingRule[] rules = new ISchedulingRule[overlappingProjects.size()];
		for (int i = 0; i < projects.length; i++) {
			rules[i] = SVNResourceRuleFactory.INSTANCE.modifyRule(projects[i]);
		}
		return new MultiRule(rules);
	}

	public IProject getProject() {
		return project;
	}

	public void setRestoreOperation(RestoreProjectMetaOperation restoreOp) {
		this.restoreOp = restoreOp;
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		String projectName = project.isAccessible() && !FileUtility.isRemoteProject(project)
				? project.getLocation().toString().substring(project.getLocation().toString().lastIndexOf("/") + 1) //$NON-NLS-1$
				: project.getName();
		final IPath destination = new Path(projectLocation).append(projectName);

		ProgressMonitorUtility.doSubTask(this, monitor1 -> CheckoutAsOperation.this.doCheckout(monitor1, destination), monitor, 20, 19);

		ProgressMonitorUtility.doSubTask(this, monitor1 -> CheckoutAsOperation.this.doOpen(monitor1, destination), monitor, 20, 1);
	}

	protected void doOpen(IProgressMonitor monitor, IPath destination) throws Exception {
		if (restoreOp != null) {
			this.reportStatus(restoreOp.run(monitor).getStatus());
		}

		IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
		if (!Platform.getLocation().equals(new Path(projectLocation))) {
			description.setLocation(destination);
		}
		project.create(description, monitor);
		project.open(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
		SVNTeamProjectMapper.map(project, resource);
	}

	protected void doCheckout(IProgressMonitor monitor, IPath destination) throws Exception {
		// prepare workspace...
		ProgressMonitorUtility.setTaskInfo(monitor, this, getOperationResource("PrepareFS")); //$NON-NLS-1$

		for (Iterator<IProject> it = overlappingProjects.iterator(); it.hasNext() && !monitor.isCanceled();) {
			IProject overlappingProject = it.next();
			deleteProject(overlappingProject, monitor);
		}
		deleteFolderContent(destination.toString(), monitor);

		// check out files from the repository
		IRepositoryLocation location = resource.getRepositoryLocation();
		// using parent because deleted project does not have any location (null value
		// returned)
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
			String path = destination.toString();
			writeToConsole(IConsoleStream.LEVEL_CMD,
					"svn checkout \"" + resource.getUrl() + "@" + resource.getPegRevision() + "\" -r " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							+ resource.getSelectedRevision() + ISVNConnector.Options.asCommandLine(options)
							+ SVNUtility.getDepthArg(depth, ISVNConnector.Options.NONE) + " \"" //$NON-NLS-1$
							+ FileUtility.normalizePath(path) + "\"" //$NON-NLS-1$
							+ FileUtility.getUsernameParam(location.getUsername()) + "\n"); //$NON-NLS-1$
			proxy.checkout(SVNUtility.getEntryRevisionReference(resource), path, depth, options,
					new SVNProgressMonitor(this, monitor, project.getFullPath()));
		} finally {
			location.releaseSVNProxy(proxy);
		}
	}

	protected void deleteProject(IProject project, IProgressMonitor monitor) throws Exception {
		IPath location = project.getLocation();
		project.delete(false, true, null);
		if (location != null) {
			deleteFolderContent(location.toString(), monitor);
		}
	}

	protected void deleteFolderContent(String targetPath, IProgressMonitor monitor) throws Exception {
		File target = new File(targetPath);
		File[] children = target.listFiles();
		if (children != null && children.length > 0) {
			for (File child : children) {
				FileUtility.deleteRecursive(child, monitor);
			}
			children = target.listFiles();
			if (children != null && children.length > 0) {
				String message = getNationalizedString("Error_LockedExternally"); //$NON-NLS-1$
				throw new UnreportableException(
						BaseMessages.format(message, new Object[] { children[0].getAbsolutePath() }));
			}
		}
	}

	@Override
	protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] { resource.getUrl() });
	}

}
