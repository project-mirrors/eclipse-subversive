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
import org.eclipse.team.svn.core.operation.ClearLocalStatusesOperation;
import org.eclipse.team.svn.core.operation.common.IActionOperation;

/**
 * ClearLocalStatusesOperation test
 * 
 * @author Alexander Gurov
 */
public abstract class ClearLocalStatusesOperationTest extends AbstractOperationTestCase {

	protected IActionOperation getOperation() {
		return new ClearLocalStatusesOperation(new IResource[] {this.getProject()});
	}

}
