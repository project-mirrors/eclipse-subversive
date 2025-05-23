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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.utility;

import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.utility.ILoggedOperationFactory;
import org.eclipse.team.svn.ui.console.SVNConsoleFactory;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;

/**
 * UI logged operation factory
 * 
 * @author Alexander Gurov
 */
public class UILoggedOperationFactory implements ILoggedOperationFactory {
	@Override
	public IActionOperation getLogged(IActionOperation operation) {
		IActionOperation retVal = wrappedOperation(operation);

		retVal.setConsoleStream(SVNConsoleFactory.getConsole().getConsoleStream());

		return retVal;
	}

	protected IActionOperation wrappedOperation(IActionOperation operation) {
		return new UILoggedOperation(operation);
	}
}
