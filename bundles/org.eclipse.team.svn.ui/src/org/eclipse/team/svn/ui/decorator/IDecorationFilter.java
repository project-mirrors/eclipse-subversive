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

package org.eclipse.team.svn.ui.decorator;

import org.eclipse.core.resources.IResource;

/**
 * Check if resource decoration is allowed
 * 
 * @author Alexander Gurov
 */
public interface IDecorationFilter {
	/**
	 * Returns <code>false</code> if resource should not be decorated
	 * 
	 * @param resource
	 *            resource which is about to be decorated
	 * @return <code>false</code> if resource should not be decorated, <code>true</code> otherwise
	 */
	public boolean isAcceptable(IResource resource);
}
