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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.SVNTeamMoveDeleteHook;
import org.eclipse.team.svn.core.operation.local.AddToSVNOperation;
import org.eclipse.team.svn.core.operation.local.CommitOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.tests.TestPlugin;
import org.eclipse.team.svn.tests.workflow.ActionOperationWorkflowBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for SVNTeamMoveDeleteHook operations for the different cases
 *
 * @author Sergiy Logvin
 */
public class SVNTeamMoveDeleteHookTest {
	@Before
	public void setUp() throws Exception {
		ActionOperationWorkflowBuilder workflowBuilder = new ActionOperationWorkflowBuilder();
		workflowBuilder.buildShareAddCommitWorkflow().execute();
	}

	@Test
	public void testDeleteFile() throws Exception {
		// Deleting commited file
		SVNTeamMoveDeleteHook hook = new SVNTeamMoveDeleteHook();
		IFile forDeleteCommited = getFirstProject().getFile("maven.xml");
		assertTrue(forDeleteCommited.exists());
		hook.deleteFile(null, forDeleteCommited, IResource.FORCE, new NullProgressMonitor());
		assertFalse(forDeleteCommited.exists());

		// Deleting unversioned file
		try (FileOutputStream fos = new FileOutputStream(getFirstProject().getLocation().toString() + "/testFile")) {
			fos.write("contents".getBytes());
		}
		IFile forDeleteUnversioned = getFirstProject().getFile("testFile");
		assertFalse(hook.deleteFile(null, forDeleteUnversioned, 0, new NullProgressMonitor()));
	}

	@Test
	public void testDeleteFolder() throws Exception {
		// Deleting commited folder
		IFolder forDeleteCommited = getSecondProject().getFolder("web");
		assertTrue(forDeleteCommited.exists());
		SVNTeamMoveDeleteHook hook = new SVNTeamMoveDeleteHook();
		new RefreshResourcesOperation(new IResource[] { getSecondProject().getFolder("web"),
				getSecondProject().getFile("web/site.css"), getSecondProject().getFile("web/site.xsl") })
						.run(new NullProgressMonitor());
		hook.deleteFolder(null, forDeleteCommited, IResource.FORCE, new NullProgressMonitor());
		assertFalse(getSecondProject().getFile("web/site.css").exists());

		// Deleting unversioned folder
		File newFolder = new File(getFirstProject().getLocation().toString() + "/testFolder");
		newFolder.mkdir();
		IFolder forDeleteUnversioned = getFirstProject().getFolder("testFolder");
		assertFalse(hook.deleteFolder(null, forDeleteUnversioned, 0, new NullProgressMonitor()));
	}

	@Test
	public void testMoveFile() throws Exception {
		// Moving commited file to the commited destination
		IFile source = getFirstProject().getFile("maven.xml");
		IFile destination = getFirstProject().getFile("src/maven.xml");
		assertTrue(source.exists());
		SVNTeamMoveDeleteHook hook = new SVNTeamMoveDeleteHook();
		hook.moveFile(null, source, destination, IResource.FORCE, new NullProgressMonitor());
		assertFalse(source.exists());
		assertTrue(destination.exists());

		// Moving commited file to the unversioned destination
		IFile source2 = getSecondProject().getFile("site.xml");
		File newFolder = new File(getFirstProject().getLocation().toString() + "/testFolder");
		newFolder.mkdirs();
		IFile destination2 = getFirstProject().getFile("testFolder/site.xml");
		hook.moveFile(null, source2, destination2, IResource.FORCE, new NullProgressMonitor());
		assertFalse(source2.exists());
		assertTrue(destination2.exists());

		// Moving unversioned file to the unversioned destination
		try (FileOutputStream fos = new FileOutputStream(getSecondProject().getLocation().toString() + "/testFile.txt")) {
			fos.write("contents".getBytes());
		}
		IFile source3 = getSecondProject().getFile("testFile.txt");
		File newFolder2 = new File(getSecondProject().getLocation().toString() + "/testFolder2");
		newFolder2.mkdir();
		IFile destination3 = getSecondProject().getFile("testFolder2/testFile.txt");
		assertFalse(hook.moveFile(null, source3, destination3, IResource.FORCE, new NullProgressMonitor()));
		assertFalse(destination3.exists());

		// Moving unversioned file to the commited destination
		assertFalse(hook.moveFile(null, source3, getSecondProject().getFile("web/testFile.txt"), IResource.FORCE,
				new NullProgressMonitor()));
	}

	@Test
	public void testMoveFolder() throws Exception {
		// Moving commited folder to the commited destination
		File sourceFolder = new File(getFirstProject().getLocation().toString() + "/commitedFolder");
		sourceFolder.mkdir();
		IFolder[] commitedFolder = { getFirstProject().getFolder("commitedFolder") };
		new AddToSVNOperation(commitedFolder).run(new NullProgressMonitor());
		new CommitOperation(commitedFolder, "", false, false).run(new NullProgressMonitor());
		SVNRemoteStorage.instance().refreshLocalResources(commitedFolder, IResource.DEPTH_INFINITE);
		commitedFolder[0].refreshLocal(IResource.DEPTH_INFINITE, null);
		IFolder destination = getFirstProject().getFolder("src/testFolder");
		SVNTeamMoveDeleteHook hook = new SVNTeamMoveDeleteHook();
		IFolder source = commitedFolder[0];
		assertTrue(hook.moveFolder(null, source, destination, IResource.FORCE, new NullProgressMonitor()));
		assertTrue(destination.exists());

		// Moving commited folder to the unversioned destination
		sourceFolder = new File(getFirstProject().getLocation().toString() + "/commitedFolder2");
		sourceFolder.mkdir();
		commitedFolder = new IFolder[] { getFirstProject().getFolder("commitedFolder2") };
		new AddToSVNOperation(commitedFolder).run(new NullProgressMonitor());
		new CommitOperation(commitedFolder, "", false, false).run(new NullProgressMonitor());
		SVNRemoteStorage.instance().refreshLocalResources(commitedFolder, IResource.DEPTH_INFINITE);
		commitedFolder[0].refreshLocal(IResource.DEPTH_INFINITE, null);
		File unversionedFolder = new File(getFirstProject().getLocation().toString() + "/destinationFolder");
		unversionedFolder.mkdir();
		destination = getSecondProject().getFolder("destinationFolder");
		source = commitedFolder[0];
		assertTrue(hook.moveFolder(null, source, destination, IResource.FORCE, new NullProgressMonitor()));
		assertTrue(destination.exists());

		// Moving unversioned folder to the unversioned destination
		sourceFolder = new File(getFirstProject().getLocation().toString() + "/unversionedSourceFolder");
		sourceFolder.mkdir();
		source = getFirstProject().getFolder("unversionedSourceFolder");
		unversionedFolder = new File(getFirstProject().getLocation().toString() + "/destinationFolder2");
		unversionedFolder.mkdir();
		destination = getSecondProject().getFolder("destinationFolder2");
		assertFalse(hook.moveFolder(null, source, destination, IResource.FORCE, new NullProgressMonitor()));

		// Moving unversioned folder to the commited destination
		assertFalse(hook.moveFolder(null, source, commitedFolder[0], IResource.FORCE, new NullProgressMonitor()));
	}

	protected IProject getFirstProject() {
		ResourceBundle bundle = TestPlugin.instance().getResourceBundle();
		return ResourcesPlugin.getWorkspace().getRoot().getProject(bundle.getString("Project1.Name"));
	}

	protected IProject getSecondProject() {
		ResourceBundle bundle = TestPlugin.instance().getResourceBundle();
		return ResourcesPlugin.getWorkspace().getRoot().getProject(bundle.getString("Project2.Name"));
	}

	protected IRepositoryLocation getLocation() {
		return SVNRemoteStorage.instance().getRepositoryLocations()[0];
	}

}
