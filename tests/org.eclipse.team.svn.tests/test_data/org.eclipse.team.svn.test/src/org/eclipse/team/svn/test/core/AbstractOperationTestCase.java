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

package org.eclipse.team.svn.test.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.operation.common.IActionOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;

import junit.framework.TestCase;

/**
 * Abstract operation test
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractOperationTestCase extends TestCase {

	public void testOperation() {
		this.assertOperation(this.getOperation());
	}
	
	protected abstract IActionOperation getOperation();
	
	protected IProject getProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects()[0];
	}
	
	protected IRepositoryLocation getLocation() {
		return SVNRemoteStorage.instance().getRepositoryLocations()[0];
	}
	
	protected void assertOperation(IActionOperation op) {
		assertTrue(op.run(new NullProgressMonitor()).isSuccessfull());
	}
	
}
