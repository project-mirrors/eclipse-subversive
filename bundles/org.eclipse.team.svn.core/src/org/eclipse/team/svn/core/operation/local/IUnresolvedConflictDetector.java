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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;

/**
 * Interface that enable us to detect unresolved conflict situation
 * 
 * @author Alexander Gurov
 */
public interface IUnresolvedConflictDetector {
	public void setUnresolvedConflict(boolean hasUnresolvedConflict);

	public boolean hasUnresolvedConflicts();

	public void addUnprocessed(IResource unprocessed);

	public IResource[] getUnprocessed();

	public void removeProcessed(IResource resource);

	public IResource[] getProcessed();

	public void setConflictMessage(String message);

	public String getMessage();
}
