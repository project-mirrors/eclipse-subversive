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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.tests.core;

import static org.junit.Assert.assertTrue;

import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation.LocationReferenceTypeEnum;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.junit.Test;

/**
 * Manage repository locations using SVNRemoteStorage test
 * 
 * @author Alexander Gurov
 */
public class RepositoryLocationsManagementTest {
	@Test
	public void testLocationsManagement() {
		SVNRemoteStorage storage = SVNRemoteStorage.instance();
		IRepositoryLocation location = storage.newRepositoryLocation();

		location.setUrl("http://testurl");
		location.setLabel("Label");
		location.setPassword("password");
		location.setPasswordSaved(true);
		location.setTagsLocation("tags");
		location.setTrunkLocation("trunk");
		location.setBranchesLocation("branches");
		location.setUsername("username");

		storage.addRepositoryLocation(location);

		try {
			storage.saveConfiguration();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		String reference = storage.repositoryLocationAsReference(location, LocationReferenceTypeEnum.ALL);

		IRepositoryLocation refTest = storage.newRepositoryLocation(reference);

		assertTrue("Location reference", location.getId().equals(refTest.getId())
				&& location.getBranchesLocation().equals(refTest.getBranchesLocation())
				&& location.getLabel().equals(refTest.getLabel()) && location.getName().equals(refTest.getName())
				&& location.getTagsLocation().equals(refTest.getTagsLocation())
				&& location.getTrunkLocation().equals(refTest.getTrunkLocation())
				&& location.getUrl().equals(refTest.getUrl()));

		storage.removeRepositoryLocation(location);

		try {
			storage.saveConfiguration();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		refTest = storage.newRepositoryLocation(reference);

		assertTrue("Location reference", location.getId().equals(refTest.getId())
				&& location.getBranchesLocation().equals(refTest.getBranchesLocation())
				&& location.getLabel().equals(refTest.getLabel()) && location.getName().equals(refTest.getName())
				&& location.getTagsLocation().equals(refTest.getTagsLocation())
				&& location.getTrunkLocation().equals(refTest.getTrunkLocation())
				&& location.getUrl().equals(refTest.getUrl()));
	}

}
