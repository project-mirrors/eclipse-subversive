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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.common.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.common.IActionOperation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVNUtility test
 * 
 * @author Alexander Gurov
 */
public abstract class SVNUtilityTest extends AbstractOperationTestCase {

	protected IActionOperation getOperation() {
		return new AbstractActionOperation("SVN Utility") {
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				assertTrue("Valid Location", SVNUtility.isValidRepositoryLocation(SVNRemoteStorage.instance().getRepositoryLocations()[0]));
				
				assertTrue("Connected Info", SVNUtility.getConnectedToSVNInfo(SVNUtilityTest.this.getProject()) != null);
				
				assertFalse("Is Ignored", SVNUtility.isIgnored(SVNUtilityTest.this.getProject()));
			}
		};
	}
	
}
