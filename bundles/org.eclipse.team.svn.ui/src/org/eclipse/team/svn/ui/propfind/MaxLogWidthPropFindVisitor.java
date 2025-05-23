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
 * Selects a maximum width of log message form a logWidthMarker properties.
 * 
 * @author Alexei Goncharov
 */
public class MaxLogWidthPropFindVisitor implements IPropFindVisitor {
	protected int width;

	public MaxLogWidthPropFindVisitor() {
		width = 0;
	}

	@Override
	public boolean visit(SVNProperty propertyParam) {
		if (propertyParam.name.equals("tsvn:logwidthmarker")) { //$NON-NLS-1$
			try {
				int currWidth = Integer.decode(propertyParam.value);
				if (width > currWidth || width == 0) {
					width = currWidth;
				}
			} catch (NumberFormatException ex) {
				//we ignore the exception
			}
		}
		return true;
	}

	public int getMaxLogWidth() {
		return width;
	}

}
