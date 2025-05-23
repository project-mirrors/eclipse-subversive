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
 *    Michael (msa) - Eclipse-SourceReferences support
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Class that implements serializing and deserializing of references to the SVN based projects
 * 
 * @author Alexander Gurov
 */
public class SVNTeamProjectSetCapability extends ProjectSetCapability {

	private static final Pattern SINGLE_SCHEME_URL_PATTERN = Pattern.compile("^.*:(\\w[\\w+-_]*://.*)$"); //$NON-NLS-1$

	public SVNTeamProjectSetCapability() {
	}

	@Override
	public String[] asReference(IProject[] projects, ProjectSetSerializationContext context, IProgressMonitor monitor)
			throws TeamException {
		monitor.beginTask(SVNMessages.Operation_ExportProjectSet, projects.length);
		try {
			String[] result = new String[projects.length];
			for (int i = 0; i < projects.length; i++) {
				result[i] = SVNTeamProjectSetCapability.DEFAULT_HANDLER.asReference(projects[i]);
				monitor.worked(1);
			}
			return result;
		} finally {
			monitor.done();
		}
	}

	@Override
	public String asReference(URI uri, String projectName) {
		String resourceUrl = SVNTeamProjectSetCapability.getSingleSchemeUrl(uri);
		return SVNTeamProjectSetCapability.DEFAULT_HANDLER.asReference(resourceUrl, projectName);
	}

	/**
	 * remove everything before the final scheme part, e.g.: {@code scm:svn:http://xyz -> http://xyz}
	 */
	public static String getSingleSchemeUrl(URI uri) {
		Matcher m = SVNTeamProjectSetCapability.SINGLE_SCHEME_URL_PATTERN.matcher(uri.toString());
		return m.replaceAll("$1"); //$NON-NLS-1$
	}

	@Override
	public IProject[] addToWorkspace(String[] referenceStrings, ProjectSetSerializationContext context,
			IProgressMonitor monitor) throws TeamException {
		if (referenceStrings.length == 0) {
			return new IProject[0];
		}
		IProjectSetHandler handler = SVNTeamProjectSetCapability.getProjectSetHandler(referenceStrings[0]);
		if (handler == null) {
			return new IProject[0];
		}

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Map<IProject, String> project2reference = new HashMap<>();
		for (String referenceString : referenceStrings) {
			String name = handler.getProjectNameForReference(referenceString);
			if (name != null) {
				project2reference.put(root.getProject(name), referenceString);
			}
		}

		Set<IProject> allProjects = project2reference.keySet();
		IProject[] projects = confirmOverwrite(context, allProjects.toArray(new IProject[allProjects.size()]));

		if (projects != null && projects.length > 0) {
			final CompositeOperation op = new CompositeOperation("Operation_ImportProjectSet", SVNMessages.class); //$NON-NLS-1$

			op.add(new SaveRepositoryLocationsOperation());

			ArrayList<IProject> retVal = new ArrayList<>();
			for (IProject project2 : projects) {
				String fullReference = project2reference.get(project2);
				IProject project = handler.configureCheckoutOperation(op, project2, fullReference);
				if (project != null) {
					retVal.add(project);
				}
			}
			projects = retVal.toArray(new IProject[retVal.size()]);

			op.add(new RefreshResourcesOperation(projects));
			SVNTeamPlugin.instance().getOptionProvider().addProjectSetCapabilityProcessing(op);

			// already in WorkspaceModifyOperation context
			ProgressMonitorUtility.doTaskExternal(op, monitor);
		}

		return projects;
	}

	protected static IProjectSetHandler DEFAULT_HANDLER = new DefaultProjectSetHandler();

	protected static IProjectSetHandler SUBCLIPSE_HANDLER = new SubclipseProjectSetHandler();

	public static IProjectSetHandler getProjectSetHandler(String referenceString) {
		if (DEFAULT_HANDLER.accept(referenceString)) {
			return DEFAULT_HANDLER;
		} else if (SUBCLIPSE_HANDLER.accept(referenceString)) {
			return SUBCLIPSE_HANDLER;
		}
		return null;
	}

}
