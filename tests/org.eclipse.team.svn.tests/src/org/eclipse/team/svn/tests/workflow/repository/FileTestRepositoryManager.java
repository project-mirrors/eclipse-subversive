/*******************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *   See git history
 *******************************************************************************/

package org.eclipse.team.svn.tests.workflow.repository;

import java.util.HashMap;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.operation.remote.CreateFolderOperation;
import org.eclipse.team.svn.core.operation.remote.DeleteResourcesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.ISVNStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.tests.TestPlugin;

public class FileTestRepositoryManager implements TestRepositoryManager {
	protected IRepositoryLocation repositoryLocation;

	protected ISVNStorage svnStorage;

	protected ResourceBundle bundle = TestPlugin.instance().getResourceBundle();

	public FileTestRepositoryManager() {
		svnStorage = SVNFileStorage.instance();
	}

	@Override
	public synchronized void createRepository() throws Exception {
		// NIC compare with SVNTeamPlugin.start(..) and TestPlugin.start(..) and
		// TestUtil.setUpRemoteRepository()
		// this extended version was taken from TestWorkflow

		HashMap<String, Object> preferences = new HashMap<>();
		preferences.put(ISVNStorage.PREF_STATE_INFO_LOCATION, TestPlugin.instance().getStateLocation());
		svnStorage.initialize(preferences);

		repositoryLocation = svnStorage.newRepositoryLocation();
		repositoryLocation.setUrl(bundle.getString("Repository.URL"));
		repositoryLocation.setTrunkLocation(bundle.getString("Repository.Trunk"));
		repositoryLocation.setBranchesLocation(bundle.getString("Repository.Branches"));
		repositoryLocation.setTagsLocation(bundle.getString("Repository.Tags"));
		repositoryLocation.setStructureEnabled(true);
		repositoryLocation.setLabel(bundle.getString("Repository.Label"));
		repositoryLocation.setUsername(bundle.getString("Repository.Username"));
		repositoryLocation.setPassword(bundle.getString("Repository.Password"));
		repositoryLocation.setPasswordSaved("true".equals(bundle.getString("Repository.SavePassword")));

		svnStorage.addRepositoryLocation(repositoryLocation);
		repositoryLocation = svnStorage.getRepositoryLocation(repositoryLocation.getId());

		deleteRepositoryNode(SVNUtility.getProposedTrunk(repositoryLocation));
		deleteRepositoryNode(SVNUtility.getProposedBranches(repositoryLocation));
		deleteRepositoryNode(SVNUtility.getProposedTags(repositoryLocation));

		CreateFolderOperation op = new CreateFolderOperation(repositoryLocation.getRoot(),
				repositoryLocation.getTrunkLocation(), "create trunk");
		op.run(new NullProgressMonitor());
		op = new CreateFolderOperation(repositoryLocation.getRoot(), repositoryLocation.getBranchesLocation(),
				"create branches");
		op.run(new NullProgressMonitor());
		op = new CreateFolderOperation(repositoryLocation.getRoot(), repositoryLocation.getTagsLocation(),
				"createTags");
		op.run(new NullProgressMonitor());

	}

	@Override
	public synchronized void removeRepository() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		try {
			root.delete(true, true, null);
		} catch (Exception ex) {
		}

		try {
			cleanRepositoryNode(SVNUtility.getProposedTags(repositoryLocation));
		} catch (Exception ex) {
		}
		try {
			cleanRepositoryNode(SVNUtility.getProposedBranches(repositoryLocation));
		} catch (Exception ex) {
		}
		try {
			cleanRepositoryNode(SVNUtility.getProposedTrunk(repositoryLocation));
		} catch (Exception ex) {
		}

		ISVNStorage storage = svnStorage;
		IRepositoryLocation[] locations = storage.getRepositoryLocations();

		for (IRepositoryLocation location : locations) {
			location.dispose();
			try {
				storage.removeRepositoryLocation(location);
			} catch (Exception ex) {
			}
		}
	}

	@Override
	public IRepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	private void deleteRepositoryNode(IRepositoryContainer node) throws Exception {
		if (node.exists()) {
			DeleteResourcesOperation op = new DeleteResourcesOperation(new IRepositoryResource[] { node },
					"test delete");
			op.run(new NullProgressMonitor());
		}
	}

	private void cleanRepositoryNode(IRepositoryContainer node) throws Exception {
		if (node.exists()) {
			IRepositoryResource[] children = node.getChildren();
			if (children != null && children.length > 0) {
				String[] toDelete = new String[children.length];
				for (int i = 0; i < children.length; i++) {
					toDelete[i] = SVNUtility.encodeURL(children[i].getUrl());
				}
				ISVNConnector proxy = repositoryLocation.acquireSVNProxy();
				try {
					proxy.removeRemote(toDelete, "Test Done", ISVNConnector.Options.FORCE, null,
							new SVNNullProgressMonitor());
				} finally {
					repositoryLocation.releaseSVNProxy(proxy);
				}
			}
		}
	}

}
