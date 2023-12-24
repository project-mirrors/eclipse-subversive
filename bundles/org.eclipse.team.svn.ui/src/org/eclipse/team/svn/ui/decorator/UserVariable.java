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
 *    Alexander Gurov - Initial implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.decorator;

/**
 * User defined decoration variable 
 * 
 * @author Alexander Gurov
 */
public class UserVariable extends PredefinedVariable {
	public UserVariable(String domain, String data) {
		super(domain, data);
	}
	
	public String getDescription() {
		return this.getDescription("_user_defined_data"); //$NON-NLS-1$
	}
	
}
