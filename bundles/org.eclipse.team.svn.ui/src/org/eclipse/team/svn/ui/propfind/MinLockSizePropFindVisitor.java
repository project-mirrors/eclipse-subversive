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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.propfind;

import org.eclipse.team.svn.core.connector.SVNProperty;

/**
 * TODO insert type description here
 * 
 * @author Alexei Goncharov
 */
public class MinLockSizePropFindVisitor implements IPropFindVisitor {
	protected int minLockSize;

	public MinLockSizePropFindVisitor() {
		this.minLockSize = 0;
	}

	public boolean visit(SVNProperty propertyParam) {
		if (propertyParam.name.equals("tsvn:lockmsgminsize")) { //$NON-NLS-1$
			try {
				int currMinSize = Integer.decode(propertyParam.value);
				if (this.minLockSize < currMinSize) {
					this.minLockSize = currMinSize;
				}
			} catch (NumberFormatException ex) {
				//we ignore the exception
			}
		}
		return true;
	}

	public int getMinLockSize() {
		return this.minLockSize;
	}

}
