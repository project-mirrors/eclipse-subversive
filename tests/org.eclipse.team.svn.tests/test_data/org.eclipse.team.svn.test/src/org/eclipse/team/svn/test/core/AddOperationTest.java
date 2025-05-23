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

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.svn.core.operation.AddToSVNOperation;
import org.eclipse.team.svn.core.operation.common.IActionOperation;
import org.eclipse.team.svn.test.TestPlugin;

/**
 * Add operation test
 * 
 * @author Alexander Gurov
 */
public abstract class AddOperationTest extends AbstractOperationTestCase {

	protected IActionOperation getOperation() {
		IProject prj = this.getProject();
		
		IFile file = prj.getFile(TestPlugin.instance().getResourceBundle().getString("File.AdditionTest"));
		
		try {
			file.create(new ByteArrayInputStream("data".getBytes()), true, null);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		
		return new AddToSVNOperation(new IResource[] {file});
	}

}
