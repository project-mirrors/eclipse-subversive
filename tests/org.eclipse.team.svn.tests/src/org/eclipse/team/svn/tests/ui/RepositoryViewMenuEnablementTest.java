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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.tests.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.local.AddToSVNOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.tests.TestPlugin;
import org.eclipse.team.svn.tests.workflow.ActionOperationWorkflowBuilder;
import org.eclipse.team.svn.ui.RemoteResourceTransferrable;
import org.eclipse.team.svn.ui.action.local.management.CleanupAction;
import org.eclipse.team.svn.ui.action.remote.BranchAction;
import org.eclipse.team.svn.ui.action.remote.CompareAction;
import org.eclipse.team.svn.ui.action.remote.CopyAction;
import org.eclipse.team.svn.ui.action.remote.CreateFolderAction;
import org.eclipse.team.svn.ui.action.remote.CutAction;
import org.eclipse.team.svn.ui.action.remote.DeleteAction;
import org.eclipse.team.svn.ui.action.remote.PasteAction;
import org.eclipse.team.svn.ui.action.remote.RefreshAction;
import org.eclipse.team.svn.ui.action.remote.RenameAction;
import org.eclipse.team.svn.ui.action.remote.ShowAnnotationAction;
import org.eclipse.team.svn.ui.action.remote.ShowHistoryAction;
import org.eclipse.team.svn.ui.action.remote.TagAction;
import org.eclipse.team.svn.ui.action.remote.management.CreateProjectStructureAction;
import org.eclipse.team.svn.ui.action.remote.management.EditRepositoryLocationPropertiesAction;
import org.eclipse.team.svn.ui.operation.PrepareRemoteResourcesTransferrableOperation;
import org.eclipse.team.svn.ui.repository.model.RepositoryFile;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.repository.model.RepositoryLocation;
import org.eclipse.team.svn.ui.repository.model.RepositoryResource;
import org.eclipse.ui.IActionDelegate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Menu enablement test for the Subversive menus in Repository View
 *
 * @author Sergiy Logvin
 */
public class RepositoryViewMenuEnablementTest {

	@BeforeClass
	public static void beforeAll() {
		ResourceBundle bundle = TestPlugin.instance().getResourceBundle();
		boolean workbenchEnabled = "true".equals(bundle.getString("UI.WorkbenchEnabled"));
		assumeTrue(workbenchEnabled);
	}

	@Before
	public void setUp() throws Exception {
		ActionOperationWorkflowBuilder workflowBuilder = new ActionOperationWorkflowBuilder();
		workflowBuilder.buildShareAddCommitWorkflow().execute();
		File newFolder = new File(getFirstProject().getLocation().toString() + "/testFolder");
		newFolder.mkdir();
		newFolder = new File(getSecondProject().getLocation().toString() + "/testFolder");
		newFolder.mkdir();
		IResource[] projects = { getFirstProject(), getSecondProject() };
		new RefreshResourcesOperation(projects).run(new NullProgressMonitor());
		new AddToSVNOperation(new IResource[] { getSecondProject().getFolder("testFolder") })
				.run(new NullProgressMonitor());
	}

	@Test
	public void testPasteRemoteResourceAction() {
		RepositoryResource[] resources = getTwoRepositoryFiles();
		new PrepareRemoteResourcesTransferrableOperation(
				new IRepositoryResource[] { resources[0].getRepositoryResource(),
						resources[1].getRepositoryResource() },
				RemoteResourceTransferrable.OP_COPY, TestPlugin.instance().getWorkbench().getDisplay())
						.run(new NullProgressMonitor());
		IActionDelegate action = new PasteAction();
		this.assertEnablement(action, getAllRepositoryResources(), false);
		this.assertEnablement(action, getOneRepositoryContainer(), true);
		this.assertEnablement(action, getNotHeadRevisionFiles(), false);
		this.assertEnablement(action, new RepositoryResource[] { getNotHeadRevisionFiles()[0] }, false);
	}

	@Test
	public void testBranchRemoteAction() {
		IActionDelegate action = new BranchAction();
		this.assertEnablement(action, getAllRepositoryResources(), true);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, true);
	}

	@Test
	public void testTagRemoteAction() {
		IActionDelegate action = new TagAction();
		this.assertEnablement(action, getAllRepositoryResources(), true);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, true);
	}

	@Test
	public void testCleanupAction() {
		IActionDelegate action = new CleanupAction();
		this.assertEnablement(action, getSelectedProjects(), true);
		this.assertEnablement(action, new IResource[] { getSelectedProjects()[0] }, true);
	}

	@Test
	public void testCompareTwoRepositoryResourcesAction() {
		IActionDelegate action = new CompareAction();
		this.assertEnablement(action, new IResource[] { getSelectedProjects()[0] }, false);
		this.assertEnablement(action, getOneRepositoryContainer(), true);
		this.assertEnablement(action, getOneRepositoryFile(), true);
		this.assertEnablement(action, getAllRepositoryResources(), false);
	}

	@Test
	public void testCopyRemoteResourceAction() {
		IActionDelegate action = new CopyAction();
		this.assertEnablement(action, getTwoRepositoryContainers(), true);
		this.assertEnablement(action, getAllRepositoryResources(), true);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, true);
	}

	@Test
	public void testCreateProjectStructureAction() {
		IActionDelegate action = new CreateProjectStructureAction();
		this.assertEnablement(action, getOneRepositoryContainer(), true);
		this.assertEnablement(action, getRepositoryLocation(), true);
	}

	@Test
	public void testCreateRemoteFolderAction() {
		IActionDelegate action = new CreateFolderAction();
		this.assertEnablement(action, getTwoRepositoryContainers(), false);
		this.assertEnablement(action, new RepositoryResource[] { getTwoRepositoryContainers()[0] }, true);
		this.assertEnablement(action, new RepositoryResource[] { getNotHeadRevisionFiles()[0] }, false);
	}

	@Test
	public void testCutRemoteResourceAction() {
		IActionDelegate action = new CutAction();
		this.assertEnablement(action, getTwoRepositoryContainers(), true);
		this.assertEnablement(action, getAllRepositoryResources(), true);
		this.assertEnablement(action, getNotHeadRevisionFiles(), false);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, true);
		this.assertEnablement(action, getRepositoryLocation(), false);
		this.assertEnablement(action, getRepositoryRoots(), false);
	}

	@Test
	public void testDeleteRemoteResourceAction() {
		IActionDelegate action = new DeleteAction();
		this.assertEnablement(action, getTwoRepositoryContainers(), true);
		this.assertEnablement(action, getAllRepositoryResources(), true);
		this.assertEnablement(action, new RepositoryResource[] { getNotHeadRevisionFiles()[0] }, false);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, true);
		this.assertEnablement(action, getRepositoryLocation(), false);
		this.assertEnablement(action, getRepositoryRoots(), false);
	}

	@Test
	public void testEditRepositoryLocationPropertiesAction() {
		IActionDelegate action = new EditRepositoryLocationPropertiesAction();
		this.assertEnablement(action, getTwoRepositoryContainers(), false);
		this.assertEnablement(action, getRepositoryLocation(), true);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, false);
	}

	@Test
	public void testRefreshRemoteAction() {
		IActionDelegate action = new RefreshAction();
		this.assertEnablement(action, getTwoRepositoryContainers(), true);
		this.assertEnablement(action, getNotHeadRevisionFiles(), true);
		this.assertEnablement(action, getRepositoryLocation(), true);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, true);
	}

	@Test
	public void testRenameRemoteResourceAction() {
		IActionDelegate action = new RenameAction();
		this.assertEnablement(action, getTwoRepositoryContainers(), false);
		this.assertEnablement(action, getAllRepositoryResources(), false);
		this.assertEnablement(action, new RepositoryResource[] { getNotHeadRevisionFiles()[0] }, false);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, true);
		this.assertEnablement(action, getRepositoryLocation(), false);
		this.assertEnablement(action, getRepositoryRoots(), false);
	}

	@Test
	public void testShowRemoteAnnotationAction() {
		IActionDelegate action = new ShowAnnotationAction();
		this.assertEnablement(action, getAllRepositoryResources(), false);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, true);
	}

	@Test
	public void testShowRemoteResourceHistoryAction() {
		IActionDelegate action = new ShowHistoryAction();
		this.assertEnablement(action, getAllRepositoryResources(), false);
		this.assertEnablement(action, new RepositoryResource[] { getAllRepositoryResources()[0] }, true);
	}

	protected void assertEnablement(IActionDelegate actionDelegate, RepositoryResource[] resources,
			boolean expectedEnablement) {
		IAction action = new Action() {
		};
		ISelection selection = asSelection(resources);
		actionDelegate.selectionChanged(action, selection);
		assertEquals(getName(actionDelegate) + " enablement wrong!", expectedEnablement, action.isEnabled());
	}

	protected void assertEnablement(IActionDelegate actionDelegate, IResource[] resources, boolean expectedEnablement) {
		IAction action = new Action() {
		};
		ISelection selection = asSelection(resources);
		actionDelegate.selectionChanged(action, selection);
		assertEquals(getName(actionDelegate) + " enablement wrong!", expectedEnablement, action.isEnabled());
	}

	protected void assertEnablement(IActionDelegate actionDelegate, RepositoryLocation[] locations,
			boolean expectedEnablement) {
		IAction action = new Action() {
		};
		ISelection selection = asSelection(locations);
		actionDelegate.selectionChanged(action, selection);
		assertEquals(getName(actionDelegate) + " enablement wrong!", expectedEnablement, action.isEnabled());
	}

	protected ISelection asSelection(Object[] resources) {
		return new StructuredSelection(resources);
	}

	protected String getName(IActionDelegate actionDelegate) {
		return actionDelegate.getClass().getName();
	}

	protected RepositoryLocation[] getRepositoryLocation() {
		return new RepositoryLocation[] { new RepositoryLocation(
				getAllRepositoryResources()[0].getRepositoryResource().getRepositoryLocation()) };
	}

	protected RepositoryResource[] getAllRepositoryResources() {
		SVNRemoteStorage storage = SVNRemoteStorage.instance();
		List<RepositoryResource> remoteResources = new ArrayList<>();
		IResource[] resources = FileUtility.getResourcesRecursive(
				new IResource[] { getFirstProject(), getSecondProject() }, IStateFilter.SF_ONREPOSITORY);
		for (IResource element : resources) {
			remoteResources.add(RepositoryFolder.wrapChild(null, storage.asRepositoryResource(element), null));
		}
		return remoteResources.toArray(new RepositoryResource[remoteResources.size()]);
	}

	protected RepositoryResource[] getOneRepositoryFile() {
		return new RepositoryResource[] { getTwoRepositoryFiles()[0] };
	}

	protected RepositoryResource[] getTwoRepositoryFiles() {
		List<RepositoryResource> twoRemoteFiles = new ArrayList<>();
		RepositoryResource[] resources = getAllRepositoryResources();
		for (RepositoryResource element : resources) {
			if (element instanceof RepositoryFile) {
				twoRemoteFiles.add(element);
				if (twoRemoteFiles.size() == 2) {
					return twoRemoteFiles.toArray(new RepositoryResource[twoRemoteFiles.size()]);
				}
			}
		}
		return twoRemoteFiles.toArray(new RepositoryResource[twoRemoteFiles.size()]);
	}

	protected RepositoryResource[] getNotHeadRevisionFiles() {
		List<RepositoryResource> twoRemoteFiles = new ArrayList<>();
		RepositoryResource[] resources = getAllRepositoryResources();
		for (RepositoryResource element : resources) {
			if (element instanceof RepositoryFile) {
				element.getRepositoryResource().setSelectedRevision(SVNRevision.fromNumber(123));
				twoRemoteFiles.add(element);
				if (twoRemoteFiles.size() == 2) {
					return twoRemoteFiles.toArray(new RepositoryResource[twoRemoteFiles.size()]);
				}
			}
		}
		return twoRemoteFiles.toArray(new RepositoryResource[twoRemoteFiles.size()]);
	}

	protected RepositoryResource[] getOneRepositoryContainer() {
		return new RepositoryResource[] { getTwoRepositoryContainers()[0] };
	}

	protected RepositoryResource[] getTwoRepositoryContainers() {
		List<RepositoryResource> twoRemoteFolders = new ArrayList<>();
		RepositoryResource[] resources = getAllRepositoryResources();
		for (RepositoryResource element : resources) {
			if (element instanceof RepositoryFolder) {
				twoRemoteFolders.add(element);
				if (twoRemoteFolders.size() == 2) {
					return twoRemoteFolders.toArray(new RepositoryResource[twoRemoteFolders.size()]);
				}
			}
		}
		return twoRemoteFolders.toArray(new RepositoryResource[twoRemoteFolders.size()]);
	}

	protected IResource[] getSelectedProjects() {
		IResource[] selectedResources = FileUtility.getResourcesRecursive(
				new IResource[] { getFirstProject(), getSecondProject() }, IStateFilter.SF_ONREPOSITORY);

		ArrayList<IResource> projects = new ArrayList<>();
		for (IResource resource : selectedResources) {
			if (resource.getType() == IResource.PROJECT) {
				projects.add(resource);
			}
		}
		return projects.toArray(new IResource[projects.size()]);
	}

	protected RepositoryResource[] getRepositoryRoots() {
		List<RepositoryResource> roots = new ArrayList<>();
		RepositoryResource[] resources = getAllRepositoryResources();
		for (RepositoryResource element : resources) {
			if (resources[0].getRepositoryResource() instanceof IRepositoryRoot) {
				roots.add(element);
			}
		}
		return roots.toArray(new RepositoryResource[roots.size()]);
	}

	protected IProject getFirstProject() {
		ResourceBundle bundle = TestPlugin.instance().getResourceBundle();
		return ResourcesPlugin.getWorkspace().getRoot().getProject(bundle.getString("Project1.Name"));
	}

	protected IProject getSecondProject() {
		ResourceBundle bundle = TestPlugin.instance().getResourceBundle();
		return ResourcesPlugin.getWorkspace().getRoot().getProject(bundle.getString("Project2.Name"));
	}

}
