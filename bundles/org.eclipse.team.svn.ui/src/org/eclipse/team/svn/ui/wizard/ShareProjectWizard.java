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
 *    Alessandro Nistico - [patch] Change Set's implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.NotifyProjectStatesChangedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.management.ReconnectProjectOperation;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.resource.events.ProjectStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.action.IResourceSelector;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.extension.factory.ICommitDialog;
import org.eclipse.team.svn.ui.extension.factory.IShareProjectFactory;
import org.eclipse.team.svn.ui.panel.local.CommitPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.synchronize.SVNChangeSetCapability;
import org.eclipse.team.svn.ui.utility.CommitActionUtility;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.wizard.shareproject.AddRepositoryLocationPage;
import org.eclipse.team.svn.ui.wizard.shareproject.AlreadyConnectedPage;
import org.eclipse.team.svn.ui.wizard.shareproject.EditCommentPage;
import org.eclipse.team.svn.ui.wizard.shareproject.IShareProjectWrapper;
import org.eclipse.team.svn.ui.wizard.shareproject.SelectProjectNamePage;
import org.eclipse.team.svn.ui.wizard.shareproject.SelectProjectsGroupPage;
import org.eclipse.team.svn.ui.wizard.shareproject.SelectRepositoryLocationPage;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Share project wizard main class
 * 
 * @author Alexander Gurov
 */
public class ShareProjectWizard extends AbstractSVNWizard implements IConfigurationWizard {
	protected IProject[] allProjects;

	protected IResourceProvider projects;

	protected SelectRepositoryLocationPage selectLocation;

	protected SelectProjectNamePage selectName;

	protected AddRepositoryLocationPage addLocation;

	protected AlreadyConnectedPage connectedPage;

	protected EditCommentPage commentPage;

	public ShareProjectWizard() {
		setWindowTitle(SVNUIMessages.ShareProjectWizard_Title_Single);
	}

	@Override
	public void addPages() {
		projects = new SelectProjectsGroupPage(allProjects);

		addPage((SelectProjectsGroupPage) projects);

		addPage(connectedPage = new AlreadyConnectedPage());

		IRepositoryLocation[] locations = SVNRemoteStorage.instance().getRepositoryLocations();
		if (locations.length > 0) {
			addPage(selectLocation = new SelectRepositoryLocationPage(locations));
		}
		addPage(addLocation = new AddRepositoryLocationPage());
		addPage(selectName = ExtensionsManager.getInstance().getCurrentShareProjectFactory().getProjectLayoutPage());
		addPage(commentPage = new EditCommentPage(projects));
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
	}

	@Override
	public IWizardPage getStartingPage() {
		if (!isGroupSelectionRequired()) {
			return getNextPage(super.getStartingPage());
		}
		return super.getStartingPage();
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage retVal = null;
		addLocation.setInitialUrl(null);
		if (page == projects && !alreadyConnected()) {
			retVal = super.getNextPage(super.getNextPage(page));
		} else if (page instanceof SelectRepositoryLocationPage && selectLocation.useExistingLocation()) {
			retVal = super.getNextPage(super.getNextPage(page));
		} else if (page instanceof AlreadyConnectedPage) {
			if (connectedPage.createUsingProjectSettings()) {
				addLocation.setInitialUrl(connectedPage.getResourceUrl());
				addLocation.setForceDisableRoots(true);
				retVal = selectLocation == null ? super.getNextPage(page) : super.getNextPage(super.getNextPage(page));
			} else {
				addLocation.setInitialUrl(""); //$NON-NLS-1$
				addLocation.setForceDisableRoots(false);
//				this.addLocation.setMessage(AddRepositoryLocationPage.DEFAULT_MESSAGE);
				retVal = super.getNextPage(page);
			}
		} else {
			retVal = super.getNextPage(page);
		}

		if (retVal == selectName) {
			selectName.setProjectsAndLocation(getProjects(),
					selectLocation != null && selectLocation.useExistingLocation()
							? selectLocation.getRepositoryLocation()
							: addLocation.getRepositoryLocation());
		} else if (retVal == commentPage) {
			commentPage.setSelectedRepositoryLocation(
					selectLocation != null && selectLocation.useExistingLocation()
							? selectLocation.getRepositoryLocation()
							: addLocation.getRepositoryLocation());
			commentPage.setDefaultCommitMessage();
		} else if (retVal == connectedPage && alreadyConnected()) {
			connectedPage.setProjects(getProjects());
		}

		return retVal;
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		IWizardPage retVal = null;
		if (super.getPreviousPage(page) == connectedPage && !alreadyConnected()) {
			retVal = super.getPreviousPage(super.getPreviousPage(page));
		} else if (page instanceof SelectProjectNamePage && selectLocation != null
				&& selectLocation.useExistingLocation()) {
			retVal = super.getPreviousPage(super.getPreviousPage(page));
		} else if (page instanceof AddRepositoryLocationPage && connectedPage != null
				&& connectedPage.createUsingProjectSettings()) {
			retVal = super.getPreviousPage(super.getPreviousPage(page));
		} else {
			retVal = super.getPreviousPage(page);
		}
		if (retVal == projects && !isGroupSelectionRequired()) {
			return null;
		}
		return retVal;
	}

	@Override
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		IShareProjectFactory factory = ExtensionsManager.getInstance().getCurrentShareProjectFactory();
		if (currentPage instanceof SelectProjectsGroupPage) {
			return false;
		}
		if (currentPage instanceof AlreadyConnectedPage) {
			return ((AlreadyConnectedPage) currentPage).useProjectSettings() && currentPage.isPageComplete()
					&& !factory.disallowFinishOnAlreadyConnected(getProjects());
		}
		if (currentPage instanceof AddRepositoryLocationPage && connectedPage != null
				&& connectedPage.createUsingProjectSettings()) {
			return currentPage.isPageComplete() && !factory.disallowFinishOnAddRepositoryLocation(getProjects());
		}
		if (currentPage instanceof SelectRepositoryLocationPage) {
			return selectLocation.useExistingLocation()
					&& !factory.disallowFinishOnSelectRepositoryLocation(getProjects());
		}
		return super.canFinish();
	}

	@Override
	public boolean performFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage instanceof AddRepositoryLocationPage && !addLocation.performFinish()) {
			return false;
		}

		boolean reconnect = connectedPage == null
				|| !connectedPage.useProjectSettings() && !connectedPage.createUsingProjectSettings();
		if (selectName.getRootProjectName() == null && currentPage instanceof AddRepositoryLocationPage) {
			selectName.setProjectsAndLocation(getProjects(), addLocation.getRepositoryLocation());
		}

		final IShareProjectWrapper mainOp = reconnect
				? getFreshConnectOperation()
				: getAlreadyConnectedOperation(connectedPage.createUsingProjectSettings());

		final CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());

		op.add(new NotifyProjectStatesChangedOperation(mainOp.getProjects(), ProjectStatesChangedEvent.ST_PRE_SHARED));

		if (addLocation.getOperationToPeform() != null) {
			op.add(addLocation.getOperationToPeform());
			op.add(mainOp, new IActionOperation[] { addLocation.getOperationToPeform() });
		} else {
			op.add(mainOp);
		}
		op.add(new RefreshResourcesOperation(mainOp, IResource.DEPTH_INFINITE, RefreshResourcesOperation.REFRESH_ALL));

		op.add(new NotifyProjectStatesChangedOperation(mainOp.getProjects(), ProjectStatesChangedEvent.ST_POST_SHARED));

		if ((!alreadyConnected() || reconnect) && (commentPage == null || commentPage.isShowCommitDialog())) {
			op.add(new PostShareCommitOperation(mainOp));
		}

		UIMonitorUtility.doTaskScheduledActive(op);

		return true;
	}

	protected boolean isGroupSelectionRequired() {
		return ((SelectProjectsGroupPage) projects).isGroupSelectionRequired();
	}

	protected IShareProjectWrapper getAlreadyConnectedOperation(boolean create) {
		return create
				? new ReconnectProjectOperationImpl(getProjects(), addLocation.getRepositoryLocation())
				: new ReconnectProjectOperationImpl(getProjects(),
						connectedPage.getSelectedRoot().getRepositoryLocation());
	}

	protected IShareProjectWrapper getFreshConnectOperation() {
		IActionOperation addLocationOp = addLocation.getOperationToPeform();
		IRepositoryLocation location = null;
		if (addLocationOp != null) {
			location = addLocation.getRepositoryLocation();
		} else {
			location = selectLocation.getRepositoryLocation();
		}

		final ShareProjectOperation mainOp = ExtensionsManager.getInstance()
				.getCurrentShareProjectFactory()
				.getShareProjectOperation(getProjects(), location, selectName, commentPage.getCommitComment());
		boolean ignoreExternals = SVNTeamPreferences.getBehaviourBoolean(
				SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.BEHAVIOUR_IGNORE_EXTERNALS_NAME);
		mainOp.setIngoreExternals(ignoreExternals);
		mainOp.setSharePrompt(projects -> {
			final int[] result = new int[1];
			UIMonitorUtility.getDisplay().syncExec(() -> {
				String projectNames = FileUtility.getNamesListAsString(projects);
				String message = BaseMessages.format(
						projects.length == 1
								? SVNUIMessages.ShareProject_Confirmation_Description_Single
								: SVNUIMessages.ShareProject_Confirmation_Description_Multiple,
						new String[] { projectNames });
				MessageDialog dialog = new MessageDialog(
						UIMonitorUtility.getShell(),
						projects.length == 1
								? SVNUIMessages.ShareProject_Confirmation_Title_Single
								: SVNUIMessages.ShareProject_Confirmation_Title_Multiple,
						null, message, MessageDialog.WARNING,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
				result[0] = dialog.open();
			});
			return result[0] == 0;
		});
		CompositeOperationImpl op = new CompositeOperationImpl(mainOp.getId(), mainOp.getMessagesClass());

		//drop .svn folders if we want to do a fresh share
		IActionOperation predecessor = null;
		if (alreadyConnected()) {
			op.add(predecessor = new AbstractActionOperation("Operation_DropSVNMeta", SVNUIMessages.class) { //$NON-NLS-1$
				@Override
				public ISchedulingRule getSchedulingRule() {
					return MultiRule.combine(ShareProjectWizard.this.getProjects());
				}

				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					IProject[] projects = ShareProjectWizard.this.getProjects();
					for (int i = 0; i < projects.length && !monitor.isCanceled(); i++) {
						final IProject project = projects[i];
						this.protectStep(monitor1 -> FileUtility.removeSVNMetaInformation(project, monitor1), monitor, projects.length);
					}
				}
			});
		}
		if (predecessor != null) {
			op.add(mainOp, new IActionOperation[] { predecessor });
		} else {
			op.add(mainOp);
		}

		return op;
	}

	@Override
	public void init(IWorkbench workbench, IProject project) {
		this.init(new IProject[] { project });
	}

	public void init(IProject[] projects) {
		allProjects = projects;
		if (projects.length > 1) {
			setWindowTitle(SVNUIMessages.ShareProjectWizard_Title_Multiple);
		} else {
			setWindowTitle(SVNUIMessages.ShareProjectWizard_Title_Single);
		}
	}

	protected boolean alreadyConnected() {
		return FileUtility.alreadyOnSVN(getProjects()[0]);
	}

	protected IProject[] getProjects() {
		return (IProject[]) projects.getResources();
	}

	protected class ReconnectProjectOperationImpl extends ReconnectProjectOperation implements IShareProjectWrapper {
		public ReconnectProjectOperationImpl(IProject[] projects, IRepositoryLocation location) {
			super(projects, location);
		}

		@Override
		public IResource[] getResources() {
			return getProjects();
		}

		@Override
		public IProject[] getProjects() {
			return ShareProjectWizard.this.getProjects();
		}

	}

	protected class CompositeOperationImpl extends CompositeOperation implements IShareProjectWrapper {
		public CompositeOperationImpl(String name, Class<? extends NLS> messagesClass) {
			super(name, messagesClass);
		}

		@Override
		public IResource[] getResources() {
			return getProjects();
		}

		@Override
		public IProject[] getProjects() {
			return ShareProjectWizard.this.getProjects();
		}

	}

	protected class PostShareCommitOperation extends AbstractActionOperation {
		protected IActionOperation mainOp;

		public PostShareCommitOperation(IActionOperation mainOp) {
			super("Operation_PrepareCommit", SVNUIMessages.class); //$NON-NLS-1$
			this.mainOp = mainOp;
		}

		@Override
		protected void runImpl(final IProgressMonitor monitor) throws Exception {
			if (mainOp.getExecutionState() != IStatus.OK) {
				return;
			}
			IResourceSelector selector = new IResourceSelector() {
				@Override
				public IResource[] getSelectedResources() {
					return getProjects();
				}

				@Override
				public IResource[] getSelectedResources(IStateFilter filter) {
					return FileUtility.getResourcesRecursive(this.getSelectedResources(), filter, IResource.DEPTH_ZERO);
				}

				@Override
				public IResource[] getSelectedResourcesRecursive(IStateFilter filter) {
					return this.getSelectedResourcesRecursive(filter, IResource.DEPTH_INFINITE);
				}

				@Override
				public IResource[] getSelectedResourcesRecursive(IStateFilter filter, int depth) {
					return FileUtility.getResourcesRecursive(this.getSelectedResources(), filter, depth,
							PostShareCommitOperation.this, monitor);
				}
			};
			final CommitActionUtility commitUtility = new CommitActionUtility(selector);
			if (commitUtility.getAllResources().length == 0) {
				return;
			}
			String proposedComment = SVNChangeSetCapability.getProposedComment(commitUtility.getAllResources());
			IResource[] allResources = commitUtility.getAllResources();
			final CommitPanel commitPanel = new CommitPanel(allResources, allResources, CommitPanel.MSG_COMMIT,
					proposedComment);
			final CompositeOperation[] commitOp = new CompositeOperation[1];
			UIMonitorUtility.getDisplay().syncExec(() -> {
				ICommitDialog commitDialog = ExtensionsManager.getInstance()
						.getCurrentCommitFactory()
						.getCommitDialog(getShell(), commitUtility.getAllResourcesSet(), commitPanel);
				if (commitDialog.open() == 0) {
					commitOp[0] = commitUtility.getCompositeCommitOperation(commitPanel.getSelectedResources(),
							commitPanel.getNotSelectedResources(), commitPanel.getTreatAsEdits(),
							commitDialog.getMessage(), commitPanel.getKeepLocks(), getShell(),
							UIMonitorUtility.getActivePart(), true);
				}
			});
			if (commitOp[0] != null) {
				UIMonitorUtility.doTaskScheduledActive(commitOp[0]);
			}
		}
	}

}
