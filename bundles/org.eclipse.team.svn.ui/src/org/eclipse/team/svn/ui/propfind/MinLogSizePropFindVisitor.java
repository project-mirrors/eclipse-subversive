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
 * Selects a minimum size of log message form a minLogSize properties.
 * 
 * @author Alexei Goncharov
 */
public class MinLogSizePropFindVisitor implements IPropFindVisitor {
	protected int minLogSize;

	public MinLogSizePropFindVisitor() {
		minLogSize = 0;
	}

	@Override
	public boolean visit(SVNProperty propertyParam) {
		if (propertyParam.name.equals("tsvn:logminsize")) { //$NON-NLS-1$
			try {
				int currMinSize = Integer.decode(propertyParam.value);
				if (minLogSize < currMinSize) {
					minLogSize = currMinSize;
				}
			} catch (NumberFormatException ex) {
				//we ignore the exception
			}
		}
		return true;
	}

	public int getMinLogSize() {
		return minLogSize;
	}

}
