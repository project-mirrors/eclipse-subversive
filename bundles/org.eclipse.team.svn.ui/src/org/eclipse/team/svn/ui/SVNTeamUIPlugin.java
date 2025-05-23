/*******************************************************************************
 * Copyright (c) 2005, 2024 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *    Alessandro Nistico - [patch] Change Set's implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.console.SVNConsole;
import org.eclipse.team.svn.ui.console.SVNConsoleFactory;
import org.eclipse.team.svn.ui.decorator.SVNLightweightDecorator;
import org.eclipse.team.svn.ui.discovery.DiscoveryConnectorsHelper;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Plugin entry point. Implements "system facade" pattern
 * 
 * @author Alexander Gurov
 */
public class SVNTeamUIPlugin extends AbstractUIPlugin {
	private volatile static SVNTeamUIPlugin instance = null;

	private ProjectCloseListener pcListener;

	private URL baseUrl;

	private Timer timer;

	public SVNTeamUIPlugin() {
		SVNTeamUIPlugin.instance = this;
		pcListener = new ProjectCloseListener();
		timer = new Timer();
	}

	public static SVNTeamUIPlugin instance() {
		return SVNTeamUIPlugin.instance;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public SVNConsole getConsole() {
		return SVNConsoleFactory.getConsole();
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public IConsoleStream getConsoleStream() {
		return SVNConsoleFactory.getConsole().getConsoleStream();
	}

	public ImageDescriptor getImageDescriptor(String path) {
		try {
			return ImageDescriptor.createFromURL(new URL(baseUrl, path));
		} catch (MalformedURLException e) {
			LoggedOperation.reportError(SVNUIMessages.getErrorString("Error_GetImageDescriptor"), e); //$NON-NLS-1$
			return null;
		}
	}

	public String getVersionString() {
		return getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
	}

	public IEclipsePreferences getPreferences() {
		return InstanceScope.INSTANCE.getNode(getBundle().getSymbolicName());
	}

	public void savePreferences() {
		try {
			SVNTeamUIPlugin.instance().getPreferences().flush();
		} catch (BackingStoreException ex) {
			UILoggedOperation.reportError(SVNUIMessages.getErrorString("Error_SavePreferences"), ex); //$NON-NLS-1$
		}
	}

	/*
	 * Important: Don't call any CoreExtensionsManager's methods here,
	 * because CoreExtensionsManager instantiates some UI classes through extension
	 * points and there could be problems in following case:
	 * Subversive core calls CoreExtensionsManager, where CoreExtensionsManager calls SVNTeamUIPlugin.start
	 * but SVNTeamUIPlugin.start calls CoreExtensionsManager
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		baseUrl = context.getBundle().getEntry("/"); //$NON-NLS-1$
		ResourcesPlugin.getWorkspace()
		.addResourceChangeListener(SVNTeamUIPlugin.this.pcListener,
				IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
		checkFirstStartup();
		if (connectorsAreRequired()) {
			discoveryConnectors();
		}
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				SVNRemoteStorage.instance().checkForExternalChanges();
			}
		}, 1000, 1000);
	}

	private void checkFirstStartup() throws CoreException {
		IPreferenceStore store = getPreferenceStore();
		if (store.getBoolean(SVNTeamPreferences.FIRST_STARTUP)) {
			store.setValue(SVNTeamPreferences.FIRST_STARTUP, false);
			savePreferences();
			// If we enable the decorator in the XML, the SVN plug-in will be loaded
			// on startup even if the user never uses SVN. Therefore, we enable the
			// decorator on the first start of the SVN plug-in since this indicates that
			// the user has done something with SVN. Subsequent startups will load
			// the SVN plug-in unless the user disables the decorator. In this case,
			// we will not re-enable since we only enable automatically on the first startup.
			WorkbenchJob job = new WorkbenchJob(Display.getDefault(), "Enable SVN Decorator") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						PlatformUI.getWorkbench().getDecoratorManager().setEnabled(SVNLightweightDecorator.ID, true);
						return Status.OK_STATUS;

					} catch (CoreException e) {
						return e.getStatus();
					}
				}
			};
			job.setSystem(true);
			job.schedule();
		}
	}

	protected boolean connectorsAreRequired() {
		if (!CoreExtensionsManager.isExtensionsRegistered(CoreExtensionsManager.SVN_CONNECTOR)) {
			for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
				if (SVNUtility.hasSVNFolderInOrAbove(project) || FileUtility.isConnected(project)) {
					return true;
				}
			}
		}
		return false;
	}

	public void discoveryConnectors() {
		/*
		 * We can't run Discovery Connectors through IActionOperation, because
		 * it uses CoreExtensionsManager (for getting nationalized operation name),
		 * which isn't allowed here, see bug 300592; so instead we use Job.
		 */
		Job job = new Job(SVNUIMessages.Operation_DiscoveryConnectors) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					DiscoveryConnectorsHelper discovery = new DiscoveryConnectorsHelper();
					discovery.run(monitor);
				} catch (Throwable t) {
					//shouldn't prevent plug-in start
					LoggedOperation.reportError(SVNUIMessages.Operation_DiscoveryConnectors_Error, t);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setUser(false);
		job.schedule();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		timer.cancel();
		SVNConsoleFactory.destroyConsole();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(pcListener);
		super.stop(context);
	}

}
