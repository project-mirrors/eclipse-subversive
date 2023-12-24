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
 * Visitor combines another visitors implementing IPropFindVisitors
 * 
 * @author Alexei Goncharov
 */
public class CompositePropFindVisitor implements IPropFindVisitor {
	protected IPropFindVisitor[] visitorsList;

	public CompositePropFindVisitor(IPropFindVisitor[] visitorsListParam) {
		this.visitorsList = visitorsListParam;
	}

	public boolean visit(SVNProperty propertyParam) {
		boolean retVal = false;
		for (int i = 0; i < this.visitorsList.length; i++) {
			retVal |= this.visitorsList[i].visit(propertyParam);
		}
		return retVal;
	}

}
