/*******************************************************************************
 * Copyright (c) 2008, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.mylyn;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylyn.internal.resources.ui.ResourcesUiBridgePlugin;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.team.ui.FocusedTeamUiPlugin;
import org.eclipse.mylyn.internal.team.ui.LinkedTaskInfo;
import org.eclipse.mylyn.internal.team.ui.properties.TeamPropertiesLinkProvider;
import org.eclipse.mylyn.monitor.core.InteractionEvent;
import org.eclipse.mylyn.resources.ui.ResourcesUi;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.team.ui.AbstractTaskReference;
import org.eclipse.mylyn.team.ui.IContextChangeSet;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.provider.ThreeWayDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.svn.core.mapping.SVNActiveChangeSet;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.Preferences;

public class SVNContextChangeSet extends SVNActiveChangeSet implements IAdaptable, IContextChangeSet {

	// HACK: copied from super
	protected static final String CTX_TITLE = "title"; //$NON-NLS-1$

	protected boolean suppressInterestContribution = false;

	protected final ITask task;

	public SVNContextChangeSet(ITask task, ActiveChangeSetManager manager) {
		super(manager, task.getSummary());
		this.task = task;
		updateLabel();
	}

	@Override
	public boolean isManagedExternally() {
		return true;
	}

	@Override
	public boolean isUserCreated() {
		return true;
	}

	@Override
	public void updateLabel() {
		super.setName(task.getSummary());
		super.setTitle(task.getSummary());
	}

	/**
	 * Encodes the handle in the title, since init won't get called on this class.
	 */
	@Override
	public void save(Preferences prefs) {
		super.save(prefs);
		prefs.put(CTX_TITLE, getTitleForPersistance());
	}

	protected String getTitleForPersistance() {
		return getTitle() + " (" + task.getHandleIdentifier() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String getHandleFromPersistedTitle(String title) {
		int delimStart = title.lastIndexOf('(');
		int delimEnd = title.lastIndexOf(')');
		if (delimStart != -1 && delimEnd != -1) {
			return title.substring(delimStart + 1, delimEnd);
		} else {
			return null;
		}
	}

	@Override
	public String getComment() {
		return getComment(true);
	}

	@Override
	public String getComment(boolean checkTaskRepository) {
		String template = null;
		Set<IProject> projects = new HashSet<>();
		IResource[] resources = getChangedResources();
		for (IResource resource : resources) {
			IProject project = resource.getProject();
			if (project != null && project.isAccessible() && !projects.contains(project)) {
				TeamPropertiesLinkProvider provider = new TeamPropertiesLinkProvider();
				template = provider.getCommitCommentTemplate(project);
				if (template != null) {
					break;
				}
				projects.add(project);
			}
		}

		boolean proceed = true;

		if (checkTaskRepository) {
			boolean unmatchedRepositoryFound = false;
			for (IProject project : projects) {
				TaskRepository repository = TasksUiPlugin.getDefault().getRepositoryForResource(project);
				if (repository != null) {
					if (!repository.getRepositoryUrl().equals(task.getRepositoryUrl())) {
						unmatchedRepositoryFound = true;
					}
				}
			}

			if (unmatchedRepositoryFound) {
				proceed = MessageDialog.openQuestion(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Mylyn Change Set Management", //$NON-NLS-1$
						"You are attempting to commit a resource which is not associated with the selected task repository.  Proceed with creating the commit message?"); //$NON-NLS-1$
			}
		}

		if (proceed) {
			if (template == null) {
				template = FocusedTeamUiPlugin.getDefault()
						.getPreferenceStore()
						.getString(
								FocusedTeamUiPlugin.COMMIT_TEMPLATE);
			}
			return FocusedTeamUiPlugin.getDefault().getCommitTemplateManager().generateComment(task, template);
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	@Override
	public void add(IDiff diff) {
		super.add(diff);
		IResource resource = getResourceFromDiff(diff);
		if (!suppressInterestContribution && resource != null) {
			Set<IResource> resources = new HashSet<>();
			resources.add(resource);
			if (ResourcesUiBridgePlugin.getDefault() != null) {
				ResourcesUi.addResourceToContext(resources, InteractionEvent.Kind.SELECTION);
			}
		}
	}

	protected IResource getResourceFromDiff(IDiff diff) {
		if (diff instanceof ResourceDiff) {
			return ((ResourceDiff) diff).getResource();
		} else if (diff instanceof ThreeWayDiff) {
			ThreeWayDiff threeWayDiff = (ThreeWayDiff) diff;
			return ResourcesPlugin.getWorkspace().getRoot().findMember(threeWayDiff.getPath());
		} else {
			return null;
		}
	}

	@Override
	public void restoreResources(IResource[] newResources) throws CoreException {
		suppressInterestContribution = true;
		try {
			super.add(newResources);
			setComment(getComment(false));
		} catch (TeamException e) {
			throw e;
		} finally {
			suppressInterestContribution = false;
		}
	}

	public IResource[] getChangedResources() {
		return super.getResources();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof SVNContextChangeSet && task != null) {
			SVNContextChangeSet changeSet = (SVNContextChangeSet) object;
			return task.equals(changeSet.getTask());
		} else {
			return super.equals(object);
		}
	}

	@Override
	public int hashCode() {
		if (task != null) {
			return task.hashCode();
		} else {
			return super.hashCode();
		}
	}

	@Override
	public ITask getTask() {
		return task;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (adapter == AbstractTask.class) {
			return task;
		} else if (adapter == AbstractTaskReference.class) {
			return new LinkedTaskInfo(getTask(), this);
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
}
