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

package org.eclipse.team.svn.core.utility;

import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;

/**
 * Implementation of the interface should wrap any input operation with log writer operation
 * 
 * @author Alexander Gurov
 */
public interface ILoggedOperationFactory {
	ILoggedOperationFactory EMPTY = operation -> operation;

	ILoggedOperationFactory DEFAULT = operation -> new LoggedOperation(operation);

	IActionOperation getLogged(IActionOperation operation);
}
