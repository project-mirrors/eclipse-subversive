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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.file;

import java.io.File;

/**
 * Interface that enables us to detect unresolved conflict situation
 * 
 * @author Alexander Gurov
 */
public interface IUnresolvedConflictDetector {
	public boolean hasUnresolvedConflicts();

	public File[] getUnprocessed();

	public File[] getProcessed();

	public String getMessage();
}
