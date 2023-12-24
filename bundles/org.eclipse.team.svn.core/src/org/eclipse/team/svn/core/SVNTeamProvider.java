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
 *    Alexei Goncharov (Polarion Software) - Closing project with file in editor and reopening project generates NPE (bug 246147)
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNEntryInfo;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNErrorCodes;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.crashrecovery.ErrorDescription;
import org.eclipse.team.svn.core.history.SVNFileHistoryProvider;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.HiddenException;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.SVNResourceRuleFactory;
import org.eclipse.team.svn.core.operation.UnreportableException;
import org.eclipse.team.svn.core.operation.local.management.DisconnectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation.LocationReferenceTypeEnum;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * IProject attachment, that allows us to associate a project with a repository
 * 
 * @author Alexander Gurov
 */
@SuppressWarnings("deprecation")
public class SVNTeamProvider extends RepositoryProvider implements IConnectedProjectInformation {
	public final static QualifiedName LOCATION_PROPERTY = new QualifiedName("org.eclipse.team.svn", "location"); //$NON-NLS-1$ //$NON-NLS-2$

	public final static QualifiedName VERIFY_TAG_ON_COMMIT_PROPERTY = new QualifiedName("org.eclipse.team.svn", //$NON-NLS-1$
			"verifyTagOnCommit"); //$NON-NLS-1$

	public final static boolean DEFAULT_VERIFY_TAG_ON_COMMIT = true;

	protected IRepositoryLocation location;

	protected IRepositoryResource resource;

	protected String relocatedTo;

	protected int errorCode;

	protected int state;

	public SVNTeamProvider() {
		state = 0;
	}

	@Override
	public synchronized IRepositoryLocation getRepositoryLocation() throws HiddenException {
		if (state != 1) {
			restoreLocation();
		}
		return location;
	}

	@Override
	public synchronized IRepositoryResource getRepositoryResource() throws HiddenException {
		if (state != 1) {
			connectToProject();
		}
		return resource;
	}

	@Override
	public synchronized void switchResource(IRepositoryResource resource) throws CoreException {
		//does not affect finite automate state
		this.resource = SVNUtility.copyOf(resource);
		location = resource.getRepositoryLocation();
		SVNTeamProvider.setRepositoryLocation(getProject(), location);
	}

	@Override
	public synchronized void relocateResource() throws CoreException {
		if (state != 1) {
			restoreLocation();
		}
		SVNTeamProvider.setRepositoryLocation(getProject(), location);
		state = 0;
	}

	public static void map(IProject project, IRepositoryResource resource) throws CoreException {
		SVNTeamProvider.setRepositoryLocation(project, resource.getRepositoryLocation());
		RepositoryProvider.map(project, SVNTeamPlugin.NATURE_ID);
	}

	@Override
	public String getID() {
		return SVNTeamPlugin.NATURE_ID;
	}

	@Override
	public void configureProject() {
		connectToProject();
		SVNRemoteStorage.instance()
				.fireResourceStatesChangedEvent(new ResourceStatesChangedEvent(new IResource[] { getProject() },
						IResource.DEPTH_ZERO, ResourceStatesChangedEvent.CHANGED_NODES));
	}

	@Override
	public void deconfigure() throws CoreException {

	}

	@Override
	public IMoveDeleteHook getMoveDeleteHook() {
		return new SVNTeamMoveDeleteHook();
	}

	@Override
	public FileModificationValidator getFileModificationValidator2() {
		return SVNTeamPlugin.instance().getOptionProvider().getFileModificationValidator();
	}

	@Override
	public boolean canHandleLinkedResources() {
		// deprecated in Eclipse 3.2
		return canHandleLinkedResourceURI();
	}

	@Override
	public boolean canHandleLinkedResourceURI() {
		// since Eclipse 3.2
		return true;
	}

	@Override
	public IResourceRuleFactory getRuleFactory() {
		return SVNResourceRuleFactory.INSTANCE;
	}

	@Override
	public IFileHistoryProvider getFileHistoryProvider() {
		return new SVNFileHistoryProvider();
	}

	@Override
	protected void deconfigured() {
		IProject project = getProject();
		if (project != null) {
			try {
				project.setPersistentProperty(SVNTeamProvider.LOCATION_PROPERTY, null);
			} catch (Exception ex) {
			}
		}
		SVNRemoteStorage.instance()
				.fireResourceStatesChangedEvent(new ResourceStatesChangedEvent(new IResource[] { getProject() },
						IResource.DEPTH_ZERO, ResourceStatesChangedEvent.CHANGED_NODES));
		super.deconfigured();
	}

	protected static void setRepositoryLocation(IProject project, IRepositoryLocation location) throws CoreException {
		//as property length is limited by size, we save only required data
		project.setPersistentProperty(SVNTeamProvider.LOCATION_PROPERTY, SVNRemoteStorage.instance()
				.repositoryLocationAsReference(location, LocationReferenceTypeEnum.ONLY_REQUIRED_DATA));
	}

	protected void restoreLocation() throws HiddenException {
		if (state == 0) {
			location = null;
			if ((errorCode = uploadRepositoryLocation()) == ErrorDescription.SUCCESS
					|| (errorCode = acquireResolution(false)) == ErrorDescription.SUCCESS) {
				return;
			}

			performDisconnect(new Exception());
		}
		breakThreadExecution();
	}

	protected synchronized void connectToProject() throws HiddenException {
		if (state == 0) {
			location = null;
			resource = null;
			relocatedTo = null;
			if ((errorCode = uploadRepositoryResource()) == ErrorDescription.SUCCESS
					|| (errorCode = acquireResolution(true)) == ErrorDescription.SUCCESS) {
				state = 1;
				return;
			}
			performDisconnect(new Exception());
		}
		breakThreadExecution();
	}

	protected int acquireResolution(boolean full) {
		while (true) {
			Object context = null;
			if (errorCode == ErrorDescription.REPOSITORY_LOCATION_IS_DISCARDED) {
				context = new Object[] { getProject(), location };
			} else if (errorCode == ErrorDescription.PROJECT_IS_RELOCATED_OUTSIDE_PLUGIN) {
				context = new Object[] { getProject(), relocatedTo, location };
			} else if (errorCode == ErrorDescription.CANNOT_READ_LOCATION_DATA) {
				context = new Object[] { getProject(), relocatedTo };
			} else {
				context = getProject();
			}

			if (SVNTeamPlugin.instance()
					.getErrorHandlingFacility()
					.acquireResolution(new ErrorDescription(errorCode, context))) {
				int newError = full ? uploadRepositoryResource() : uploadRepositoryLocation();
				if (newError != ErrorDescription.SUCCESS) {
					if (newError != errorCode) {
						errorCode = newError;
						continue;
					}
					return newError;
				}
				return ErrorDescription.SUCCESS;
			}
			return errorCode;
		}
	}

	protected void performDisconnect(final Throwable source) {
		state = -1;
		CompositeOperation op = new CompositeOperation("Operation_OpenProject", SVNMessages.class); //$NON-NLS-1$
		op.add(new DisconnectOperation(new IProject[] { getProject() }, false));
		// notify user about the problem is happened
		op.add(new AbstractActionOperation(op.getId(), op.getMessagesClass()) {
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				throw new UnreportableException(SVNTeamProvider.this.getAutoDisconnectMessage(), source);
			}
		});
		ProgressMonitorUtility.doTaskScheduled(op);
	}

	protected void breakThreadExecution() {
		throw new HiddenException(getAutoDisconnectMessage());
	}

	protected String getAutoDisconnectMessage() {
		return SVNMessages.formatErrorString("Error_AutoDisconnect", new String[] { getProject().getName() }); //$NON-NLS-1$
	}

	public static boolean requiresUpgrade(IProject project) {
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider == null || !SVNTeamPlugin.NATURE_ID.equals(provider.getID())) {
			return false;
		}
		IPath location = FileUtility.getResourcePath(project);
		location = location.append(SVNUtility.getSVNFolderName());
		if (SVNUtility.isPriorToSVN17()) {
			return false;
		}
		if (location.toFile().exists() && !location.append("pristine").toFile().exists()) {
			return true;
		}
		IRepositoryLocation rLocation = SVNRemoteStorage.instance().getRepositoryLocation(project);
		ISVNConnector proxy = rLocation.acquireSVNProxy();
		try {
			SVNUtility.properties(proxy,
					new SVNEntryRevisionReference(FileUtility.getWorkingCopyPath(project), null, SVNRevision.WORKING),
					ISVNConnector.Options.NONE, new SVNNullProgressMonitor());
		} catch (SVNConnectorException e) {
			return e.getErrorId() == SVNErrorCodes.wcOldFormat;
		} finally {
			rLocation.releaseSVNProxy(proxy);
		}
		return false;
	}

	protected int uploadRepositoryResource() {
		int errorCode = uploadRepositoryLocation();

		IProject project = getProject();
		IPath location = FileUtility.getResourcePath(project);
		if (SVNUtility.isPriorToSVN17() && !location.append(SVNUtility.getSVNFolderName()).toFile().exists()) {
			return ErrorDescription.CANNOT_READ_PROJECT_METAINFORMATION;
		}
		ISVNConnector proxy = CoreExtensionsManager.instance().getSVNConnectorFactory().createConnector();
		try {
			SVNChangeStatus[] sts = SVNUtility.status(proxy, location.toString(), SVNDepth.IMMEDIATES,
					ISVNConnector.Options.INCLUDE_UNCHANGED, new SVNNullProgressMonitor());
			if (sts != null && sts.length > 0) {
				relocatedTo = getProjectURL(location.toString(), sts);
				if (relocatedTo == null) {
					// the last try to avoid problem with the client library not returning URL's sometimes, since it could cause a request to the server, as far as I remember...
					SVNEntryInfo[] info = SVNUtility.info(proxy, new SVNEntryRevisionReference(location.toString()),
							SVNDepth.EMPTY, new SVNNullProgressMonitor());
					if (info == null || info.length == 0 || info[0].url == null) {
						return ErrorDescription.CANNOT_READ_PROJECT_METAINFORMATION;
					}
					relocatedTo = SVNUtility.decodeURL(info[0].url);
				}
				if (this.location != null) {
					resource = this.location.asRepositoryContainer(relocatedTo, true);
					if (resource == null) {
						return ErrorDescription.PROJECT_IS_RELOCATED_OUTSIDE_PLUGIN;
					}
				}
			}
		} catch (SVNConnectorException ex) {
			if (ex.getErrorId() == SVNErrorCodes.wcCleanupRequired) {
				// no way to read statuses, return some fake for now...
				return ErrorDescription.WORKING_COPY_REQUIRES_CLEANUP;
			}
			if (ex.getErrorId() == SVNErrorCodes.wcOldFormat) {
				return ErrorDescription.WORKING_COPY_REQUIRES_UPGRADE;
			}
			return ErrorDescription.CANNOT_READ_PROJECT_METAINFORMATION;
		} finally {
			proxy.dispose();
		}
		return errorCode;
	}

	protected String getProjectURL(String projectPath, SVNChangeStatus[] sts) {
		SVNUtility.reorder(sts, true);
		for (SVNChangeStatus st : sts) {
			if (st.url != null && !st.isFileExternal && !st.isSwitched && !st.isCopied) { // sometime SVN client library fails to return URL for an unknown reason, so, we'll just take it from one of the children resources
				String url = SVNUtility.decodeURL(st.url);
				url = url.substring(0, url.length() - (st.path.length() - projectPath.length()));
				return url;
			}
		}
		return null;
	}

	public IRepositoryLocation peekAtLocation() {
		try {
			IProject project = getProject();
			String data = project.getPersistentProperty(SVNTeamProvider.LOCATION_PROPERTY);
			if (data != null) {
				return SVNRemoteStorage.instance().newRepositoryLocation(data);
			}
		} catch (CoreException ex) {
			// do nothing
		}
		return null;
	}

	protected int uploadRepositoryLocation() {
		location = peekAtLocation();
		if (location == null) {
			return ErrorDescription.CANNOT_READ_LOCATION_DATA;
		}
		if (SVNRemoteStorage.instance().getRepositoryLocation(location.getId()) == null) {
			return ErrorDescription.REPOSITORY_LOCATION_IS_DISCARDED;
		}
		return ErrorDescription.SUCCESS;
	}

	public boolean isVerifyTagOnCommit() {
		try {
			String strProp = getProject().getPersistentProperty(SVNTeamProvider.VERIFY_TAG_ON_COMMIT_PROPERTY);
			if (strProp == null) {
				return SVNTeamProvider.DEFAULT_VERIFY_TAG_ON_COMMIT;
			}
			return Boolean.parseBoolean(strProp);
		} catch (CoreException e) {
			//ignore and return default value
			return SVNTeamProvider.DEFAULT_VERIFY_TAG_ON_COMMIT;
		}
	}

	public void setVerifyTagOnCommit(boolean isVerifyTagOnCommit) throws CoreException {
		getProject().setPersistentProperty(SVNTeamProvider.VERIFY_TAG_ON_COMMIT_PROPERTY,
				String.valueOf(isVerifyTagOnCommit));
	}

}
