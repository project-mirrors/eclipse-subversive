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
 *    Zend Technologies - [patch] Workaround for 'thread "Worker-3" timed out waiting (5000ms) for thread...' problem
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.crashrecovery.DefaultErrorHandlingFacility;
import org.eclipse.team.svn.core.extension.crashrecovery.IErrorHandlingFacility;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.mapping.SVNActiveChangeSetCollector;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.resource.ISVNStorage;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Team provider plug-in implementation
 * 
 * @author Alexander Gurov
 */
public class SVNTeamPlugin extends Plugin {
	// all projects shared with subversion will have this nature
	public static final String NATURE_ID = "org.eclipse.team.svn.core.svnnature"; //$NON-NLS-1$

	public static final String CORE_SVNCLIENT_NAME = "svnconnector"; //$NON-NLS-1$

	private volatile static SVNTeamPlugin instance = null;

	private ProjectCloseListener pcListener;

	private ResourceChangeListener rcListener;

	private SVNFolderListener svnListener;

	private ServiceTracker tracker;

	private FileReplaceListener fileReplaceListener; //FIXME see bug #276018

	private IErrorHandlingFacility errorHandlingFacility;

	/**
	 * Allows to save changes in repository root URL and UUID
	 */
	private boolean isLocationsDirty;

	private ActiveChangeSetManager activeChangeSetManager;

	public SVNTeamPlugin() {
		SVNTeamPlugin.instance = this;

		pcListener = new ProjectCloseListener();
		rcListener = new ResourceChangeListener();
		svnListener = new SVNFolderListener();
		fileReplaceListener = new FileReplaceListener();

		errorHandlingFacility = new DefaultErrorHandlingFacility();
	}

	public File getTemporaryFile(File parent, String fileName) {
		File retVal = parent == null
				? getStateLocation().append(".tmp" + System.currentTimeMillis()).append(fileName).toFile() //$NON-NLS-1$
				: new File(parent, fileName);
		retVal.deleteOnExit();
		parent = retVal.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
			parent.deleteOnExit();
		}
		return retVal;
	}

	public void setLocationsDirty(boolean isLocationsDirty) {
		this.isLocationsDirty = isLocationsDirty;
	}

	public boolean isLocationsDirty() {
		return isLocationsDirty;
	}

	public static SVNTeamPlugin instance() {
		return SVNTeamPlugin.instance;
	}

	public IOptionProvider getOptionProvider() {
		return CoreExtensionsManager.instance().getOptionProvider();
	}

	public IErrorHandlingFacility getErrorHandlingFacility() {
		return errorHandlingFacility;
	}

	// FIXME remove later after Integration API is changed
	public void setOptionProvider(IOptionProvider optionProvider) {
		CoreExtensionsManager.instance()
				.setOptionProvider(optionProvider == null ? IOptionProvider.DEFAULT : optionProvider);

		SVNRemoteStorage.instance().reconfigureLocations();

		// remove temporary files if IDE is crached time ago...
		ProgressMonitorUtility
				.doTaskScheduledDefault(new AbstractActionOperation("Remove Temporary Files", SVNMessages.class) {
					@Override
					protected void runImpl(IProgressMonitor monitor) throws Exception {
						SVNTeamPlugin.instance().getStateLocation().toFile().listFiles((FileFilter) pathname -> {
							String name = pathname.getName();
							if (!name.equals(SVNRemoteStorage.STATE_INFO_FILE_NAME)
									&& !name.equals(SVNFileStorage.STATE_INFO_FILE_NAME)) {
								FileUtility.deleteRecursive(pathname);
							}
							return false;
						});
					}
				});
	}

	public IEclipsePreferences getPreferences() {
		return new InstanceScope().getNode(getBundle().getSymbolicName());
	}

	public void savePreferences() {
		try {
			SVNTeamPlugin.instance().getPreferences().flush();
		} catch (BackingStoreException ex) {
			LoggedOperation.reportError(SVNMessages.getErrorString("Error_SavePreferences"), ex); //$NON-NLS-1$
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		tracker = new ServiceTracker(context, IProxyService.class.getName(), null);
		tracker.open();

		HashMap preferences = new HashMap();
		preferences.put(ISVNStorage.PREF_STATE_INFO_LOCATION, getStateLocation());
		SVNFileStorage.instance().initialize(preferences);
		SVNRemoteStorage.instance().initialize(preferences);

		WorkspaceJob job = new WorkspaceJob("") { //$NON-NLS-1$
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();

				workspace.addResourceChangeListener(rcListener, IResourceChangeEvent.POST_CHANGE);
				workspace.addResourceChangeListener(svnListener, IResourceChangeEvent.PRE_BUILD);
				workspace.addResourceChangeListener(pcListener,
						IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
				workspace.addResourceChangeListener(fileReplaceListener, IResourceChangeEvent.PRE_BUILD);

				// shouldn't prevent plugin start
				try {
					rcListener.handleInitialWorkspaceDelta();
				} catch (Throwable ex) {
					LoggedOperation.reportError("Handle Initial Workspace Delta", ex);
				}

				// if some team provider is missing the code below "enables" team menu "Share Project" action...
				IProject[] projects = workspace.getRoot().getProjects();
				for (IProject project : projects) {
					RepositoryProvider.getProvider(project);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		job.setUser(false);
		job.schedule();
	}

	public IProxyService getProxyService() {
		return (IProxyService) tracker.getService();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		workspace.removeResourceChangeListener(svnListener);
		workspace.removeResourceChangeListener(rcListener);
		workspace.removeResourceChangeListener(pcListener);
		workspace.removeResourceChangeListener(fileReplaceListener);

		if (isLocationsDirty) {
			SVNRemoteStorage.instance().saveConfiguration();
			SVNFileStorage.instance().saveConfiguration();
		}
		SVNRemoteStorage.instance().dispose();
		SVNFileStorage.instance().dispose();

		tracker.close();

		// cleanup temporary files if any
		File temporaryFilesStorage = SVNTeamPlugin.instance().getStateLocation().toFile();
		File[] files = temporaryFilesStorage.listFiles((FileFilter) pathname -> pathname.getName().indexOf(".tmp") != -1);
		if (files != null) {
			for (File file : files) {
				FileUtility.deleteRecursive(file);
			}
		}

		if (activeChangeSetManager != null) {
			activeChangeSetManager.dispose();
		}

		super.stop(context);
	}

	public synchronized ActiveChangeSetManager getModelChangeSetManager() {
		if (activeChangeSetManager == null) {
			activeChangeSetManager = new SVNActiveChangeSetCollector(UpdateSubscriber.instance());
		}
		return activeChangeSetManager;
	}

}
