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

package org.eclipse.team.svn.ui.utility;

import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.utility.ILoggedOperationFactory;

/**
 * Executable operation wrapper factory provide ability to change default IActionOperation behaviour corresponding to some rules
 * 
 * @author Alexander Gurov
 */
public interface IOperationWrapperFactory extends ILoggedOperationFactory {
	public ICancellableOperationWrapper getCancellable(IActionOperation operation);
}
