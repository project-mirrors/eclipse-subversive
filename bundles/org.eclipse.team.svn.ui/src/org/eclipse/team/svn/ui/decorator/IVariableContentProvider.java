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
 * Implementros of this interface will provide the content of the decoration variable
 * 
 * @author Alexander Gurov
 */
public interface IVariableContentProvider {
	String getValue(IVariable var);
}
