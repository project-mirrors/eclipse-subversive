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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.common.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.common.IActionOperation;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * FileUtility test
 * 
 * @author Alexander Gurov
 */
public abstract class FileUtilityTest extends AbstractOperationTestCase {

	protected IActionOperation getOperation() {
		return new AbstractActionOperation("File Utility") {
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				String name = FileUtility.formatResourceName("http://testurl\\data");
				assertFalse("Format Name", name.indexOf('\\') == -1 && name.indexOf('.') == -1 && name.indexOf('/') == -1);
				
				assertTrue("Already Connected", FileUtility.alreadyConnectedToSVN(FileUtilityTest.this.getProject()));
				
				assertFalse("Team Private", FileUtility.isTeamPrivateMember(FileUtilityTest.this.getProject()));
				
				IResource []normal = FileUtility.getResourcesRecursive(new IResource[] {FileUtilityTest.this.getProject()}, IStateFilter.SF_NOTMODIFIED);
				IResource []versioned = FileUtility.getResourcesRecursive(new IResource[] {FileUtilityTest.this.getProject()}, IStateFilter.SF_VERSIONED);
				assertTrue("Versioned Resources", normal.length == versioned.length);
				
				final int []cntr = new int[1];
				cntr[0] = 0;
				FileUtility.visitNodes(FileUtilityTest.this.getProject(), new IResourceVisitor() {
					public boolean visit(IResource resource)
							throws CoreException {
						cntr[0]++;
						return true;
					}
				}, IResource.DEPTH_INFINITE);
				assertTrue("Visit Nodes", cntr[0] == normal.length);
			}
		};
	}
	
}
