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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.mylyn;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.team.ui.AbstractTaskReference;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.ui.history.SVNHistoryPage;
import org.eclipse.team.svn.ui.panel.local.CommitPanel;
import org.eclipse.team.svn.ui.properties.bugtraq.BugtraqModel;
import org.eclipse.team.svn.ui.properties.bugtraq.IssueList;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;

/**
 * LinkedTaskInfo adapter factory
 * 
 * @author Alexander Gurov
 */
public class SVNLinkedTaskInfoAdapterFactory implements IAdapterFactory {
	private static final Class[] ADAPTED_TYPES = { AbstractTaskReference.class };

	@Override
	public Class[] getAdapterList() {
		return SVNLinkedTaskInfoAdapterFactory.ADAPTED_TYPES;
	}

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (!AbstractTaskReference.class.equals(adapterType)) {
			return null;
		}

		if (adaptableObject instanceof ChangeSet) {
			return createFromChangeSet((ChangeSet) adaptableObject);
		}

		Object adapted = Platform.getAdapterManager().getAdapter(adaptableObject, SVNLogEntry.class);
		if (adapted != null) {
			SVNLogEntry historyEntry = (SVNLogEntry) adapted;
			String comment = historyEntry.message == null ? "" : historyEntry.message; //$NON-NLS-1$

			IWorkbenchPage page = UIMonitorUtility.getActivePage();
			if (page != null) {
				IViewPart view = page.findView(IHistoryView.VIEW_ID);
				if (view instanceof IHistoryView) {
					IHistoryPage historyPage = ((IHistoryView) view).getHistoryPage();
					if (historyPage instanceof SVNHistoryPage) {
						IResource resource = ((SVNHistoryPage) historyPage).getResource();
						return new SVNLinkedTaskInfo(getTaskRepositoryUrl(resource), null,
								getTaskFullUrl(resource, comment), comment);
					}
				}
			}
			return new SVNLinkedTaskInfo(null, null, null, comment);
		}

		return null;
	}

	protected AbstractTaskReference createFromChangeSet(ChangeSet set) {
		IResource[] resources = set.getResources();
		if (resources != null && resources.length > 0) {
			return new SVNLinkedTaskInfo(getTaskRepositoryUrl(resources[0]), null,
					getTaskFullUrl(resources[0], set.getComment()), set.getComment());
		}

		return new SVNLinkedTaskInfo(null, null, null, set.getComment());
	}

	protected String getTaskRepositoryUrl(IResource resource) {
		if (resource != null) {
			TaskRepository repository = TasksUiPlugin.getDefault().getRepositoryForResource(resource);
			if (repository != null) {
				return repository.getRepositoryUrl();
			}
		}
		return null;
	}

	protected String getTaskFullUrl(IResource resource, String comment) {
		BugtraqModel model = SVNLinkedTaskInfoAdapterFactory.getBugtraqModel(resource);
		IssueList linkList = new IssueList();
		linkList.parseMessage(comment, model);
		List issues = linkList.getLinks();
		if (issues.size() > 0) {
			return model.getResultingURL((IssueList.LinkPlacement) issues.get(0));
		}
		return null;
	}

	public static BugtraqModel getBugtraqModel(IResource resource) {
		CommitPanel.CollectPropertiesOperation bugtraqOp = new CommitPanel.CollectPropertiesOperation(
				new IResource[] { resource });
		bugtraqOp.run(new NullProgressMonitor());
		return bugtraqOp.getBugtraqModel();
	}

}
