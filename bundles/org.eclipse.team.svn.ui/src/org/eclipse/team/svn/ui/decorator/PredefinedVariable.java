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

import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Predefined decoration variable
 * 
 * @author Alexander Gurov
 */
public class PredefinedVariable implements IVariable {
	protected String domain;

	protected String name;

	public PredefinedVariable(String domain, String name) {
		this.domain = domain;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return this.getDescription(name);
	}

	protected String getDescription(String name) {
		return SVNUIMessages.getString(domain + "_" + name); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof IVariable) {
			return name.equals(((IVariable) obj).getName());
		}
		return super.equals(obj);
	}
}
