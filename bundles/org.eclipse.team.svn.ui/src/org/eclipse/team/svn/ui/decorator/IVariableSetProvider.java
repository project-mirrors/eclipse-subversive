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
 *    Alexey Mikoyan - Initial implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.decorator;

/**
 * Implementors of this interface will provide variables that can be used for various decoration purposes
 *
 * @author Alexey Mikoyan
 *
 */
public interface IVariableSetProvider {
	public IVariable getVariable(String name);

	public IVariable getCenterVariable();

	public String getDomainName();
}
